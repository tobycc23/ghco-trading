package com.tobycc.ghcoTrading.service;

import com.tobycc.ghcoTrading.model.PnLAggregationRequest;
import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.model.TradeFilter;
import com.tobycc.ghcoTrading.model.enums.Currency;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.tobycc.ghcoTrading.model.enums.AggregateField.*;
import static com.tobycc.ghcoTrading.model.enums.AggregateField.USER;

@Service
public class StartupService {

    @Autowired
    private Map<String, Trade> trades;

    @Autowired
    private TradeAggregationService tradeAggregationService;

    /**
     * To complete our task we can action the initial aggregation of pnl positions “per BBGCode, per portfolio,
     * per strategy, per user” on startup. Commenting out for now and doing manually for demo purposes
     */
    @PostConstruct
    public void aggregateInitialTrades() {
        //For our example we will convert our aggregations into one currency via FX conversion
        tradeAggregationService.aggregateTrades(trades, new PnLAggregationRequest(
                Optional.of(new TreeSet<>(Arrays.asList(BBG_CODE, PORTFOLIO, STRATEGY, USER))),
                Optional.of(Currency.USD),
                Optional.of(new HashSet<>(Arrays.asList(
                        new TradeFilter(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("Account1"), Optional.of("Strategy5"), Optional.empty()),
                        new TradeFilter(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("Strategy6"), Optional.empty())
                )))
        ));

        //Could do following too/instead if we wanted it split by currency instead of converted into USD
        //This would lead to 11,661 different aggregations which would not be that useful
//        tradeAggregationService.aggregateTrades(trades, new PnLAggregationRequest(
//                Optional.of(new TreeSet<>(Arrays.asList(BBG_CODE, PORTFOLIO, STRATEGY, USER))),
//                Optional.empty(),
//                Optional.empty()
//        ));
    }

}
