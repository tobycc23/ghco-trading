package com.tobycc.ghcoTrading.controller;

import com.tobycc.ghcoTrading.file.CSVParser;
import com.tobycc.ghcoTrading.model.PnLAggregationRequest;
import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.service.TradeAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Tag(name = "Trade Controller")
@Validated
@Controller
@RequestMapping(path = {"/api/v1/trade"})
public class TradeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeController.class);

    private final CSVParser csvParser;
    private final Map<String, Trade> existingTrades;
    private Map<String, List<PnLPosition>> aggregatedTrades;
    private final TradeAggregationService tradeAggregationService;

    public TradeController(CSVParser csvParser, Map<String, Trade> existingTrades, Map<String, List<PnLPosition>> aggregatedTrades, TradeAggregationService tradeAggregationService) {
        this.csvParser = csvParser;
        this.existingTrades = existingTrades;
        this.aggregatedTrades = aggregatedTrades;
        this.tradeAggregationService = tradeAggregationService;
    }

    @PostMapping
    @Operation(summary = "Persist a number of new trades")
    public List<String> postNewTrades(@RequestBody List<@Valid Trade> newTrades) {
        if(newTrades.isEmpty()) return Collections.emptyList();

        LOGGER.info("Trades (" + newTrades.size() + ") inputted from REST call. Persisting to input csv file, which will" +
                " then be picked up from file watcher and loaded");
        return csvParser.writeTradesIntoCsv(newTrades);
    }

    @PostMapping(value = "aggregate")
    @Operation(summary = "Get a PnL aggregation result based on input parameters")
    public Map<String, List<PnLPosition>> pnlAggregation(@RequestBody PnLAggregationRequest request) {
        return tradeAggregationService.aggregateTrades(existingTrades, request);
    }

    @PostMapping(value = "aggregateAndVisualise")
    @Operation(summary = "Get a PnL aggregation result based on input parameters and show graph")
    public String pnlAggregationAndVisualisation(@RequestBody PnLAggregationRequest request) {
        tradeAggregationService.aggregateTrades(existingTrades, request);
        String title = request.convertForTitle();
        String redirect =  "redirect:/api/v1/visualiser?title=" + UriUtils.encode(title, "UTF-8");
        return redirect;
    }
}
