package com.tobycc.ghcoTrading.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.service.TradeAggregationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(path = {"/api/v1/visualiser"})
public class TradeVisualiserController {

    private final TradeAggregationService tradeAggregationService;
    private final ObjectMapper objectMapper;

    public TradeVisualiserController(TradeAggregationService tradeAggregationService, ObjectMapper objectMapper) {
        this.tradeAggregationService = tradeAggregationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String index(Model model) throws JsonProcessingException {
        Map<String, List<PnLPosition>> aggregatedTrades = tradeAggregationService.getAggregatedTrades();
        model.addAttribute("chartData", objectMapper.writeValueAsString(aggregatedTrades));
        return "visualiser";
    }
}