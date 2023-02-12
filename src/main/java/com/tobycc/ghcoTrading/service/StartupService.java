package com.tobycc.ghcoTrading.service;

import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.model.enums.Currency;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

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
        //tradeAggregationService.aggregateTrades(trades, new TreeSet<>(Arrays.asList(BBG_CODE, PORTFOLIO, STRATEGY, USER)), Optional.of(Currency.USD));

        //Could do following too/instead if we wanted it split by currency instead of converted into USD
        //This would lead to 11,661 different aggregations which would not be that useful
//        tradeAggregationService.aggregateTrades(trades, new TreeSet<>(Arrays.asList(BBG_CODE, PORTFOLIO, STRATEGY, USER)));
    }

}
