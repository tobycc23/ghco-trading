package com.tobycc.ghcoTrading.service;

import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.model.enums.Action;
import com.tobycc.ghcoTrading.model.enums.AggregateField;
import com.tobycc.ghcoTrading.model.enums.Currency;
import com.tobycc.ghcoTrading.model.enums.Side;
import com.tobycc.ghcoTrading.parser.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.tobycc.ghcoTrading.model.enums.AggregateField.CURRENCY;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
public class TradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeService.class);

    @Autowired
    private CSVParser csvParser;

    @Bean
    public Map<String, Trade> loadInitialTrades() throws IOException {
        LOGGER.info("Beginning load of initial sample of trades");
        List<Trade> rawTrades = csvParser.readInitialTrades();
        return cleanTrades(rawTrades);
    }

    /**
     * Filtering the list of trades so that only the highest priority of each trade, in the following order, is present:
     * NEW -> AMEND (latest amended) -> CANCEL
     *
     * Note: We assume here that an AMEND cannot modify the TradeTimeInUTC information, as we use this to determine which is
     * the latest AMEND.
     * TODO: It would be good to have an extra column for "DateTimeCreated" to show when the record itself was created
     *  so that the TradeTimeInUTC could potentially be modified and we can still tell when the most recent AMEND arrives,
     *  irrespective of this.
     * No current logic to "uncancel" a trade by doing a consequent AMEND - this could be implemented if that logic is desired.
     *
     * @return filtered Map of trades
     */
    public Map<String,Trade> cleanTrades(List<Trade> rawTrades) {
        Map<String,Trade> cleanedTrades = new HashMap<>();
        LOGGER.info("Cleaning trades to remove redundant action variants that have been replaced");
        rawTrades.forEach(t ->
                //We filter the trades into a map
                cleanedTrades.merge(
                    t.getTradeId(),
                    t,
                    //The following remapping could be replaced by calling `robustFiltering(currTrade, newTrade)`
                    //for thorough robustness checks on the data.
                    (currTrade, newTrade) -> {
                        //Current trade is NEW, lowest priority, must be overwritten. Or if new trade arriving is CANCEL,
                        //highest priority, it must overwrite.
                        if(currTrade.getAction().equals(Action.NEW) || newTrade.getAction().equals(Action.CANCEL)) {
                            return newTrade;
                        }

                        //Current trade is CANCEL, highest priority, cannot be overwritten
                        if(currTrade.getAction().equals(Action.CANCEL)) {
                            return currTrade;
                        }

                        //Current trade is otherwise AMEND. New trade to process could be NEW or AMEND with older time,
                        // in which case cannot overwrite. Otherwise, it is an AMEND with a newer time, so must overwrite.
                        return newTrade.getAction().equals(Action.NEW) || newTrade.getDateTime().isBefore(currTrade.getDateTime())
                                ? currTrade : newTrade;
                    }
            )
        );

        return cleanedTrades;
    }

    /**
     * Entry point to where the cleaned trades can be grouped and then aggregated depending on input criteria
     * @param trades
     * @param fieldsToGroup
     * @param convertIntoCurrency
     */
    public void aggregateTrades(Map<String,Trade> trades, Set<AggregateField> fieldsToGroup, Optional<Currency> convertIntoCurrency) {
        Map<String, List<Trade>> groupedTrades = groupTrades(trades, fieldsToGroup, convertIntoCurrency);
        processPnlAggregation(groupedTrades, convertIntoCurrency);
    }

    public void aggregateTrades(Map<String,Trade> trades, Set<AggregateField> fieldsToGroup) {
        aggregateTrades(trades, fieldsToGroup, Optional.empty());
    }

    /**
     * First we have to group trades by the given fields we wish to group them by
     * @param trades
     * @param fieldsToGroup
     * @param convertIntoCurrency
     */
    public Map<String, List<Trade>> groupTrades(Map<String,Trade> trades, Set<AggregateField> fieldsToGroup, Optional<Currency> convertIntoCurrency) {
        //If we do not specify a currency to convert the trades into, we have to include CURRENCY as a grouping field
        //(if it is not already) so that the positions are split by currency to make the numbers make sense
        if(convertIntoCurrency.isEmpty()) {
            fieldsToGroup.add(CURRENCY);
        }

        //Split trades into aggregated levels based on the fields provided
        Map<String, List<Trade>> groupedTrades = trades.values().stream().collect(
                groupingBy(t -> AggregateField.getAggregateCompositeKey(t, fieldsToGroup)));

        //Sort the dates within each grouping
        groupedTrades.values().forEach(t -> t.sort(Comparator.comparing(Trade::getDateTime)));

        return groupedTrades;
    }

    public void processPnlAggregation(Map<String, List<Trade>> groupedTrades, Optional<Currency> convertIntoCurrency) {
        Map<String, List<PnLPosition>> pnlAggregated = groupedTrades.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> pnlAggregator(e.getValue(), convertIntoCurrency)
                ));

        pnlAggregationPrinter(pnlAggregated, convertIntoCurrency);
        csvParser.writeAggregationPositionsIntoCsv(pnlAggregated, convertIntoCurrency);
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
     * @param convertIntoCurrency
     */
    public void pnlAggregationPrinter(Map<String, List<PnLPosition>> pnlAggregated, Optional<Currency> convertIntoCurrency) {
        //Sort the map keys so output printed is more ordered
        List<String> keys = pnlAggregated.keySet().stream().sorted().toList();

        keys.forEach(k -> {
            LOGGER.info("");
            LOGGER.info("----- " + k + " intraday cash positions" + convertIntoCurrency
                    .map(c -> " converted to " + c + " -----")
                    .orElse(" -----")
            );
            pnlAggregated.get(k).stream().skip(1).forEach(pos ->
                    LOGGER.info(pos.dateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ": " + pos.position().toBigInteger()));
        });
    }



    /**
     * This is some robust filtering, which may be overkill, so I haven't used it in the main code but made a point to keep
     * it in for completeness.
     *
     * Potential outliers could exist if trades are not clocked in their correct action priority order, as described above.
     * There is a strong argument for any trade with an outlier described below to be removed until brought to the attention of a
     * business team and rectified. Examples:
     *
     * - A NEW trade, of same TradeID, clocked after an AMEND or CANCEL
     * - An AMEND trade clocked after a CANCEL
     * For now, this will be considered anomalous data and will be flagged via logging but not processed
     * (i.e. will not overwrite the existing trade).
     *
     * - Multiple NEWs or CANCELs of same TradeID
     * For now, the latest to arrive will be used and will be flagged via logging. For CANCELs this has no consequence to
     * overall aggregation calcs.
     *
     * - Multiple AMENDs (or even NEWs) of same TradeID have the exact same time but different data
     * This is a bigger issue that must be resolved with a business team. The aggregate calculations will be redundant
     * until the matter is resolved, especially if aspects like the price vary dramatically.
     * For now, the most recent to be processed will be used. Two CANCELs with the same time will be flagged, but of no consequence.
     *
     * - Only an AMEND or CANCEL exist without the presence of a NEW, of same TradeID
     * These will still be included as is. Harder to spot as we wouldn't know by looking at the map, as the higher priority
     * action trades replace the old, and trades can be processed in any order. Could sort first and check NEW is always
     * present first, but this wastes compute time.
     *
     * @return
     */
    public Trade robustFiltering(Trade currTrade, Trade newTrade) {
        if(currTrade.getAction().equals(Action.NEW)) {
            if(newTrade.getAction().equals(Action.NEW)) {
                LOGGER.warn("Trade " + newTrade.getTradeId() + " has multiple NEW records related to it.");
                return getTradeOfSameAction(currTrade, newTrade);
            }

            //Otherwise newTrade is AMEND or CANCEL so all good
            return newTrade;
        }
        else if(currTrade.getAction().equals(Action.AMEND)) {
            if(newTrade.getAction().equals(Action.NEW)) {
                if(!newTrade.getDateTime().isBefore(currTrade.getDateTime())) {
                    LOGGER.warn("Trade " + newTrade.getTradeId() + " has a NEW record clocked after or at same time as an AMEND. Ignoring for now.");
                }
                return currTrade;
            }
            if(newTrade.getAction().equals(Action.CANCEL)) {
                return newTrade;
            }
        }
        //Else the currTrade is CANCEL
        else {
            if(newTrade.getAction().equals(Action.NEW) || newTrade.getAction().equals(Action.AMEND)) {
                if(!newTrade.getDateTime().isBefore(currTrade.getDateTime())) {
                    LOGGER.warn("Trade " + newTrade.getTradeId() + " has a " + newTrade.getAction() +
                            " record clocked after or at same time as a CANCEL. Ignoring for now.");
                }
                return currTrade;
            }
        }

        //Otherwise newTrade has same action as currTrade (inconsequential which we return in case of CANCEL)
        return getTradeOfSameAction(currTrade, newTrade);
    }

    public Trade getTradeOfSameAction(Trade currTrade, Trade newTrade) {
        if (newTrade.getDateTime().isEqual(currTrade.getDateTime())) {
            LOGGER.warn("The trade's records are indistinguishable by time, using most recently processed");
            return newTrade;
        }

        return newTrade.getDateTime().isBefore(currTrade.getDateTime()) ? currTrade : newTrade;
    }
}
