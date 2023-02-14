package com.tobycc.ghcoTrading.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tobycc.ghcoTrading.file.CSVParser;
import com.tobycc.ghcoTrading.model.PnLAggregationRequest;
import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.model.enums.Currency;
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
import java.time.LocalDateTime;
import java.util.*;

import static com.tobycc.ghcoTrading.model.enums.AggregateField.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { TradeVisualiserController.class })
public class TradeVisualiserControllerTest {

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
    public void pnlAggregationAndVisualisation_RedirectSuccess() throws Exception {
        PnLAggregationRequest request = new PnLAggregationRequest(
                Optional.of(new TreeSet<>(Arrays.asList(BBG_CODE, PORTFOLIO, STRATEGY, USER))),
                Optional.of(Currency.USD),
                Optional.empty()
        );

        doReturn(Map.of("Test",List.of(new PnLPosition(LocalDateTime.MIN, BigDecimal.ONE))))
                .when(tradeAggregationService).aggregateTrades(any(), eq(request));

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/trade/aggregateAndVisualise").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/api/v1/trade/visualise?title=%20Currency%20converted%20to%20USD%20-%20No%20TradeFilters"));

        verify(tradeAggregationService).aggregateTrades(any(), eq(request));
    }

}