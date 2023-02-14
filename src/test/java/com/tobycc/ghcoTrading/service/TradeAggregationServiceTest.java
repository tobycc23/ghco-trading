package com.tobycc.ghcoTrading.service;

import com.tobycc.ghcoTrading.model.PnLAggregationRequest;
import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.model.TradeFilter;
import com.tobycc.ghcoTrading.model.enums.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.tobycc.ghcoTrading.model.enums.AggregateField.BBG_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Within this test, pre-aggregated trades will have already been loaded in from "/input/sample_1.csv"
 */
@SpringBootTest
@ActiveProfiles("test")
class TradeAggregationServiceTest {

    @Autowired
    private TradeAggregationService tradeAggregationService;

    @Autowired
    private TradeLoadingService tradeLoadingService;

    @Test
    public void groupTrades_noAggregationLevelsSet() {
        //No aggregation levels set for grouping, so use all that we can i.e. "BBG_CODE, ACCOUNT, PORTFOLIO, STRATEGY, USER, CURRENCY"
        Map<String, List<Trade>> result = tradeAggregationService.groupTrades(tradeLoadingService.getLoadedTrades(),
                new PnLAggregationRequest(Optional.empty(), Optional.empty(), Optional.empty())
        );

        //First check that the CANCEL trade is definitely not present
        assertEquals(0,
                result.values().stream().filter(trades ->
                        trades.stream().map(Trade::getTradeId).toList().contains("7002f15e8f234a5ca278b21157d00001")
                ).count());

        //Assert trades aggregated as expected
        assertEquals(5, result.keySet().size());
        assertTrue(result.containsKey("BRK.A US Equity,Account1,portfolio5,Strategy6,User5,JPY"));
        assertEquals(1, result.get("BRK.A US Equity,Account1,portfolio5,Strategy6,User5,JPY").size());
        assertEquals("f033fcaf0f164f99886a6bd624900000", result.get("BRK.A US Equity,Account1,portfolio5,Strategy6,User5,JPY").get(0).getTradeId());
    }

    @Test
    public void groupTrades_aggregationSet() {
        //Aggregation set for "BBG_CODE" and conversion currency set to USD
        Map<String, List<Trade>> result = tradeAggregationService.groupTrades(tradeLoadingService.getLoadedTrades(),
                new PnLAggregationRequest(Optional.of(new TreeSet<>(List.of(BBG_CODE))), Optional.of(Currency.USD), Optional.empty())
        );

        assertEquals(4, result.keySet().size());
        assertTrue(result.containsKey("AAPL US Equity"));
        assertEquals(2, result.get("AAPL US Equity").size());
        assertEquals(List.of("94de9256c1444388a569e9a8f8c00002", "6dd4d792b00244b1b81c4a0537d00003"),
                result.get("AAPL US Equity").stream().map(Trade::getTradeId).toList());

        //Aggregation set for "BBG_CODE" but no conversion currency set, so also grouped by "CURRENCY"
        Map<String, List<Trade>> result2 = tradeAggregationService.groupTrades(tradeLoadingService.getLoadedTrades(),
                new PnLAggregationRequest(Optional.of(new TreeSet<>(List.of(BBG_CODE))), Optional.empty(), Optional.empty())
        );

        assertEquals(5, result2.keySet().size());
        assertTrue(result2.containsKey("AAPL US Equity,USD"));
        assertEquals(1, result2.get("AAPL US Equity,USD").size());
    }

    @Test
    public void groupTrades_aggregationAndFilterSet() {
        //Aggregation set for "BBG_CODE" and conversion currency set to USD. Added a filter just for BBGCode="AAPL US Equity"
        Map<String, List<Trade>> result = tradeAggregationService.groupTrades(tradeLoadingService.getLoadedTrades(),
                new PnLAggregationRequest(Optional.of(new TreeSet<>(List.of(BBG_CODE))), Optional.of(Currency.USD),
                        Optional.of(new HashSet<>(List.of(new TradeFilter(
                                Optional.of("AAPL US Equity"), Optional.empty(), Optional.empty(),
                                Optional.empty(), Optional.empty(), Optional.empty())
                        )))
                )
        );

        //Map now only contains one result
        assertEquals(1, result.keySet().size());
        assertTrue(result.containsKey("AAPL US Equity"));
        assertEquals(2, result.get("AAPL US Equity").size());
        assertEquals(List.of("94de9256c1444388a569e9a8f8c00002", "6dd4d792b00244b1b81c4a0537d00003"),
                result.get("AAPL US Equity").stream().map(Trade::getTradeId).toList());
    }

    @Test
    public void processPnlAggregation_Success() {
        PnLAggregationRequest request = new PnLAggregationRequest(Optional.of(new TreeSet<>(List.of(BBG_CODE))), Optional.of(Currency.USD),
                Optional.of(new HashSet<>(List.of(new TradeFilter(
                        Optional.of("AAPL US Equity"), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty())
                )))
        );

        //Aggregation set for "BBG_CODE" and conversion currency set to USD. Added a filter just for BBGCode="AAPL US Equity"
        Map<String, List<Trade>> result = tradeAggregationService.groupTrades(
                tradeLoadingService.getLoadedTrades(),
                request
        );

        Map<String, List<PnLPosition>> aggregation = tradeAggregationService.processPnlAggregation(result, request);

        //Map now only contains one result
        assertEquals(1, aggregation.keySet().size());
        assertTrue(aggregation.containsKey("AAPL US Equity"));
        assertEquals(2, aggregation.get("AAPL US Equity").size());
        assertEquals(List.of(LocalDateTime.parse("2010-01-01T06:14:09.259619"), LocalDateTime.parse("2010-01-01T08:31:42.741820")),
                aggregation.get("AAPL US Equity").stream().map(PnLPosition::dateTime).toList());

        //First value is a Sell in USD, so we would be expected to make Profit on this, and no conversion needed
        assertEquals(BigDecimal.valueOf(2000*3), aggregation.get("AAPL US Equity").get(0).position());

        //Second value is a BUY in USD, so we would be expected to make a Loss on this (added onto the previous value),
        //and conversion needed from EUR->USD
        assertEquals(BigDecimal.valueOf(2000*3).add(
                BigDecimal.valueOf(1500*4).multiply(BigDecimal.valueOf(1.076172)).multiply(BigDecimal.valueOf(-1))
        ), aggregation.get("AAPL US Equity").get(1).position());
    }
}