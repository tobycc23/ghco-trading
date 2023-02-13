package com.tobycc.ghcoTrading.service;

import com.tobycc.ghcoTrading.model.PnLAggregationRequest;
import com.tobycc.ghcoTrading.model.enums.Currency;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.TreeSet;

import static com.tobycc.ghcoTrading.model.enums.AggregateField.*;

@Service
public class StartupService {

    private final TradeAggregationService tradeAggregationService;
    private final TradeLoadingService tradeLoadingService;

    public StartupService(TradeAggregationService tradeAggregationService, TradeLoadingService tradeLoadingService) {
        this.tradeAggregationService = tradeAggregationService;
        this.tradeLoadingService = tradeLoadingService;
    }

    /**
     * To complete our task we can action the initial aggregation of pnl positions “per BBGCode, per portfolio,
     * per strategy, per user” on startup. Commenting out for now and doing manually for demo purposes
     */
    @PostConstruct
    public void aggregateInitialTrades() {

        //For our example we will convert our aggregations into one currency via FX conversion
        tradeAggregationService.aggregateTrades(tradeLoadingService.getLoadedTrades(), new PnLAggregationRequest(
                Optional.of(new TreeSet<>(Arrays.asList(BBG_CODE, PORTFOLIO, STRATEGY, USER))),
                Optional.of(Currency.USD),
                Optional.empty()
        ));

        /*

        //Could do following too/instead if we wanted it split by currency instead of converted into USD
        //This would lead to 11,661 different aggregations which would not be that useful
        tradeAggregationService.aggregateTrades(tradeLoadingService.getLoadedTrades(), new PnLAggregationRequest(
                Optional.of(new TreeSet<>(Arrays.asList(BBG_CODE, PORTFOLIO, STRATEGY, USER))),
                Optional.empty(),
                Optional.empty()
        ));

         */
    }

}
