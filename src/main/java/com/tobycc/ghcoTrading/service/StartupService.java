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
    private TradeService tradeService;

    /**
     * To complete our task we complete the initial aggregation of pnl positions “per BBGCode, per portfolio,
     * per strategy, per user” on startup
     */
    @PostConstruct
    public void aggregateInitialTrades() {
        //For our example we will convert our aggregations into one currency via FX conversion
        tradeService.aggregateTrades(trades, new TreeSet<>(Arrays.asList(BBG_CODE, PORTFOLIO, STRATEGY, USER)), Optional.of(Currency.USD));

        //Could do following too/instead if we wanted it split by currency instead of converted into USD
        //This would lead to 11,661 different aggregations which would not be that useful
//        tradeService.aggregateTrades(trades, new TreeSet<>(Arrays.asList(BBG_CODE, PORTFOLIO, STRATEGY, USER)));
    }

}
