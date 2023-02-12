package com.tobycc.ghcoTrading.controller;

import com.tobycc.ghcoTrading.file.CSVParser;
import com.tobycc.ghcoTrading.model.Trade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Trade Controller")
@Validated
@RestController
@RequestMapping(path = {"/api/v1/trade"}, produces = APPLICATION_JSON_VALUE)
public class TradeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeController.class);

    @Autowired
    private CSVParser csvParser;

    @PostMapping
    @Operation(summary = "Persist a number of new trades")
    public List<String> postNewTrades(@RequestBody List<@Valid Trade> newTrades) {
        if(newTrades.isEmpty()) return Collections.emptyList();

        LOGGER.info("Trades (" + newTrades.size() + ") inputted from REST call. Persisting to input csv file, which will" +
                " then be picked up from file watcher and loaded");
        return csvParser.writeTradesIntoCsv(newTrades);
    }
}
