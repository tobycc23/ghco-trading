package com.tobycc.ghcoTrading.service;

import com.tobycc.ghcoTrading.config.DateUtils;
import com.tobycc.ghcoTrading.file.CSVParser;
import com.tobycc.ghcoTrading.model.PnLAggregationRequest;
import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.model.enums.Action;
import com.tobycc.ghcoTrading.model.enums.AggregateField;
import com.tobycc.ghcoTrading.model.enums.Currency;
import com.tobycc.ghcoTrading.model.enums.Side;
import com.tobycc.ghcoTrading.props.AggregationProps;
import com.tobycc.ghcoTrading.props.FileProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static com.tobycc.ghcoTrading.model.enums.AggregateField.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
public class TradeAggregationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeAggregationService.class);

    //Aggregated trades
    private Map<String, List<PnLPosition>> aggregatedTrades;

    private final CSVParser csvParser;
    private final FileProps fileProps;
    private final AggregationProps aggregationProps;

    public TradeAggregationService(CSVParser csvParser, FileProps fileProps, AggregationProps aggregationProps) {
        this.csvParser = csvParser;
        this.fileProps = fileProps;
        this.aggregationProps = aggregationProps;
    }

    /**
     * Entry point to where the cleaned trades can be grouped and then aggregated depending on input criteria
     * @param trades
     * @param request
     */
    public Map<String, List<PnLPosition>> aggregateTrades(Map<String, Trade> trades, PnLAggregationRequest request) {
        Map<String, List<Trade>> groupedTrades = groupTrades(trades, request);
        return processPnlAggregation(groupedTrades, request);
    }

    /**
     * First we have to group trades by the given fields we wish to group them by
     * @param trades
     * @param request
     */
    public Map<String, List<Trade>> groupTrades(Map<String,Trade> trades, PnLAggregationRequest request) {
        Set<AggregateField> aggregationFields = request.aggregationFields().isEmpty() || request.aggregationFields().get().isEmpty()
                ? new TreeSet<>(Arrays.asList(BBG_CODE, ACCOUNT, PORTFOLIO, STRATEGY, USER, CURRENCY))
                : request.aggregationFields().get();

        //If we do not specify a currency to convert the trades into, we have to include CURRENCY as a grouping field
        //(ignored if already included) so that the positions are split by currency to make the numbers make sense
        if(request.convertIntoCurrency().isEmpty()) {
            aggregationFields.add(CURRENCY);
        }

        //Split trades into aggregated levels based on the fields provided and filter on the trades we wish to see, else all
        Stream<Trade> groupedTradesStream = trades.values().stream().filter(trade -> !trade.getAction().equals(Action.CANCEL));
        //Note: Currently filtering is just ORing on different trade filters, could do NOTs/ANDs/more complex conditions in future
        //e.g. TradeFilters = [{account="Account1", strategy="Strategy5"}, {strategy="Strategy6"} will return all trades that
        //are either "Account1" and "Strategy5" or just "Strategy6"
        if(request.filters().isPresent() && !request.filters().get().isEmpty()) {
            groupedTradesStream = groupedTradesStream.filter(t -> request.filters().get().stream().anyMatch(tf -> tf.filter(t)));
        }

        Map<String, List<Trade>> groupedTrades = groupedTradesStream.collect(
                groupingBy(t -> AggregateField.getAggregateCompositeKey(t, aggregationFields)));

        //Sort the dates within each grouping
        groupedTrades.values().forEach(t -> t.sort(Comparator.comparing(Trade::getDateTime)));
        return groupedTrades;
    }

    /**
     * Taking in the grouped trades, we then carry out the PnL aggregation and return the results in various ways
     * @param groupedTrades
     * @param request
     * @return
     */
    public Map<String, List<PnLPosition>> processPnlAggregation(Map<String, List<Trade>> groupedTrades, PnLAggregationRequest request) {
        Map<String, List<PnLPosition>> pnlAggregated = groupedTrades.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> pnlAggregator(e.getValue(), request.convertIntoCurrency())
                ));

        //TODO following two steps of outputting aggregated data is better done via async job (via ActiveMQ etc) so REST call returns quickly
        if(aggregationProps.isOutputToConsole()) {
            pnlAggregationPrinter(pnlAggregated, request);
        }

        //We don't want to write this information to files if it goes over a predefined threshold
        if(aggregationProps.isOutputToCsv() && pnlAggregated.size() <= fileProps.getMaxFilesToOutput()) {
            csvParser.writeAggregationPositionsIntoCsv(pnlAggregated, request);
        }

        setAggregatedTrades(pnlAggregated);
        return pnlAggregated;
    }

    /**
     * Method to calculate PnL cash positions based on value of each trade Bought(Loss) or Sold(Profit)
     * Here, we define a currency for the PnL stream to be converted into.
     *
     * Note: this is pnl cash positions only, does not track how much of a stock we hold
     * @param trades
     * @param convertIntoCurrency
     * @return List of date / cumulative pnl pairs
     */
    private List<PnLPosition> pnlAggregator(List<Trade> trades, Optional<Currency> convertIntoCurrency) {
        List<PnLPosition> timeAggregate = new ArrayList<>(List.of(new PnLPosition(LocalDateTime.MIN, BigDecimal.ZERO)));

        //For each trade we work out its profit or loss, then sum this with the previous to get cumulative pnl aggregation at a give time
        for(int index=0; index < trades.size(); index++) {
            Trade t = trades.get(index);
            BigDecimal val = t.getPrice().multiply(new BigDecimal(t.getVolume()));
            if(t.getSide().equals(Side.B)) {
                val = val.multiply(new BigDecimal(-1));
            }

            //If we are converting into a specific currency, we may need to do FX conversion
            if(convertIntoCurrency.isPresent() && !convertIntoCurrency.get().equals(t.getCcy())) {
                val = val.multiply(FxService.FX_MAP.get(new FxService.FxPair(t.getCcy(), convertIntoCurrency.get())));
            }

            //We sum with previous pnl cumulative to get new pnl cumulative val for this datetime
            timeAggregate.add(new PnLPosition(t.getDateTime(), timeAggregate.get(index).position().add(val)));
        }

        timeAggregate.remove(0);
        return timeAggregate;
    }

    /**
     * Prints the output of the aggregations
     * @param pnlAggregated
     * @param request
     */
    public void pnlAggregationPrinter(Map<String, List<PnLPosition>> pnlAggregated, PnLAggregationRequest request) {
        //Sort the map keys so output printed is more ordered
        List<String> keys = pnlAggregated.keySet().stream().sorted().toList();

        keys.forEach(k -> {
            LOGGER.info("");
            LOGGER.info("----- " + k + " intraday cash positions" + request.convertForTitle() + " -----");
            pnlAggregated.get(k).forEach(pos ->
                    LOGGER.info(pos.dateTime().format(DateTimeFormatter.ofPattern(DateUtils.DATETIME_FORMAT)) + ": " + pos.position().toBigInteger()));
        });
    }

    public void setAggregatedTrades(Map<String, List<PnLPosition>> aggregatedTrades) {
        this.aggregatedTrades = aggregatedTrades;
    }

    public Map<String, List<PnLPosition>> getAggregatedTrades() {
        return aggregatedTrades;
    }
}
