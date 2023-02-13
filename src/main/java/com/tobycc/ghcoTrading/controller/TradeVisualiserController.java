package com.tobycc.ghcoTrading.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tobycc.ghcoTrading.model.PnLAggregationRequest;
import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.service.TradeAggregationService;
import com.tobycc.ghcoTrading.service.TradeLoadingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping(path = {"/api/v1/trade"})
public class TradeVisualiserController {

    private final TradeAggregationService tradeAggregationService;
    private final TradeLoadingService tradeLoadingService;
    private final ObjectMapper objectMapper;

    public TradeVisualiserController(TradeAggregationService tradeAggregationService, ObjectMapper objectMapper, TradeLoadingService tradeLoadingService) {
        this.tradeAggregationService = tradeAggregationService;
        this.objectMapper = objectMapper;
        this.tradeLoadingService = tradeLoadingService;
    }

    @PostMapping(value = "aggregateAndVisualise")
    public String pnlAggregationAndVisualisation(@RequestBody PnLAggregationRequest request) {
        tradeAggregationService.aggregateTrades(tradeLoadingService.getLoadedTrades(), request);
        return "redirect:/api/v1/trade/visualise?title=" + UriUtils.encode(request.convertForTitle(), "UTF-8");
    }

    @GetMapping(value = "visualise")
    public String index(Model model, @RequestParam String title) throws JsonProcessingException {
        Map<String, List<PnLPosition>> aggregatedTrades = tradeAggregationService.getAggregatedTrades();
        model.addAttribute("chartData", objectMapper.writeValueAsString(aggregatedTrades));
        model.addAttribute("title", Objects.requireNonNullElse(title,""));
        return "visualiser";
    }
}