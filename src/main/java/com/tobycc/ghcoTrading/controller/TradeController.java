package com.tobycc.ghcoTrading.controller;

import com.tobycc.ghcoTrading.file.CSVParser;
import com.tobycc.ghcoTrading.model.PnLAggregationRequest;
import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.service.TradeAggregationService;
import com.tobycc.ghcoTrading.service.TradeLoadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Trade Controller")
@Validated
@RestController
@RequestMapping(path = {"/api/v1/trade"})
public class TradeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeController.class);

    private final CSVParser csvParser;
    private final TradeAggregationService tradeAggregationService;
    private final TradeLoadingService tradeLoadingService;

    public TradeController(CSVParser csvParser, TradeLoadingService tradeLoadingService, TradeAggregationService tradeAggregationService) {
        this.csvParser = csvParser;
        this.tradeLoadingService = tradeLoadingService;
        this.tradeAggregationService = tradeAggregationService;
    }

    @PostMapping(produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Persist a number of new trades")
    public List<String> postNewTrades(@RequestBody List<@Valid Trade> newTrades) {
        if(newTrades.isEmpty()) return Collections.emptyList();

        LOGGER.info("Trades (" + newTrades.size() + ") inputted from REST call. Persisting to input csv file, which will" +
                " then be picked up from file watcher and loaded");
        return csvParser.writeTradesIntoCsv(newTrades);
    }

    @PostMapping(value = "aggregate", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a PnL aggregation result based on input parameters")
    public Map<String, List<PnLPosition>> pnlAggregation(@RequestBody PnLAggregationRequest request) {
        return tradeAggregationService.aggregateTrades(tradeLoadingService.getLoadedTrades(), request);
    }
}
