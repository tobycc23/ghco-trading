package com.tobycc.ghcoTrading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tobycc.ghcoTrading.file.CSVParser;
import com.tobycc.ghcoTrading.model.PnLAggregationRequest;
import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.model.enums.Action;
import com.tobycc.ghcoTrading.model.enums.Currency;
import com.tobycc.ghcoTrading.model.enums.Side;
import com.tobycc.ghcoTrading.service.TradeAggregationService;
import com.tobycc.ghcoTrading.service.TradeLoadingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.tobycc.ghcoTrading.model.enums.AggregateField.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { TradeController.class })
public class TradeControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CSVParser csvParser;

    @MockBean
    private TradeAggregationService tradeAggregationService;

    @MockBean
    private TradeLoadingService tradeLoadingService;

    @Test
    public void postNewTrades_emptySuccess() throws Exception {
        List<Trade> newTrades = Collections.emptyList();

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/trade").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTrades)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(csvParser, times(0)).writeTradesIntoCsv(any());
    }

    @Test
    public void postNewTrades_Success() throws Exception {
        List<Trade> newTrades = List.of(
                new Trade("Test", "TestBBGCode", Currency.USD, Side.B, BigDecimal.valueOf(1000), 1000,
                "portfolio1", Action.NEW, "Account1", "Strategy1", "User1", LocalDateTime.MIN, LocalDate.MIN),
                new Trade("Test2", "TestBBGCode", Currency.USD, Side.B, BigDecimal.valueOf(2000), 2000,
                        "portfolio1", Action.NEW, "Account1", "Strategy1", "User2", LocalDateTime.MIN, LocalDate.MIN)
        );

        doReturn(List.of("Test","Test2"))
                .when(csvParser).writeTradesIntoCsv(any());

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/trade").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTrades)))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"Test\",\"Test2\"]"));

        verify(csvParser).writeTradesIntoCsv(any());
    }

    @Test
    public void postNewTrades_FailOnNull() throws Exception {
        List<Trade> newTrades = List.of(
                new Trade("Test", null, Currency.USD, Side.B, BigDecimal.valueOf(1000), 1000,
                        "portfolio1", Action.NEW, "Account1", "Strategy1", "User1", LocalDateTime.MIN, LocalDate.MIN),
                new Trade("Test2", "TestBBGCode", Currency.USD, Side.B, BigDecimal.valueOf(2000), 2000,
                        "portfolio1", Action.NEW, "Account1", "Strategy1", "User2", LocalDateTime.MIN, LocalDate.MIN)
        );

        doReturn(List.of("Test","Test2"))
                .when(csvParser).writeTradesIntoCsv(any());

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/trade").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTrades)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"bbgCode\":\"must not be empty\"}"));

        verify(csvParser, times(0)).writeTradesIntoCsv(any());
    }

    @Test
    public void pnlAggregation_Success() throws Exception {
        PnLAggregationRequest request = new PnLAggregationRequest(
                Optional.of(new TreeSet<>(Arrays.asList(BBG_CODE, PORTFOLIO, STRATEGY, USER))),
                Optional.of(Currency.USD),
                Optional.empty()
        );

        doReturn(Map.of("Test",List.of(new PnLPosition(LocalDateTime.MIN, BigDecimal.ONE))))
                .when(tradeAggregationService).aggregateTrades(any(), eq(request));

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/trade/aggregate").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"Test\":[{\"dateTime\":\"-999999999-01-01T00:00:00\",\"position\":1}]}"));

        verify(tradeAggregationService).aggregateTrades(any(), eq(request));
    }
}
