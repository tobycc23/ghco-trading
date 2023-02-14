package com.tobycc.ghcoTrading.service;

import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.model.enums.Action;
import com.tobycc.ghcoTrading.model.enums.Currency;
import com.tobycc.ghcoTrading.model.enums.Side;
import com.tobycc.ghcoTrading.props.FileProps;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@EnableConfigurationProperties
@ActiveProfiles("test")
class TradeLoadingServiceTest {

    @SpyBean
    private TradeLoadingService tradeLoadingService;

    @Autowired
    private FileProps fileProps;

    @Test
    public void loadInitialTrades_Success() {
        assertEquals(6, tradeLoadingService.getLoadedTrades().keySet().size());
    }

    @Test
    public void loadNewTradesFromFile_Success() {
        assertEquals(Action.NEW, tradeLoadingService.getLoadedTrades().get("94de9256c1444388a569e9a8f8c00002").getAction());

        //Add new file
        tradeLoadingService.loadNewTradesFromFile(fileProps.getBaseDirectory() + "/testFiles/sample_2.csv");
        assertEquals(8, tradeLoadingService.getLoadedTrades().keySet().size());
        assertEquals(Action.NEW, tradeLoadingService.getLoadedTrades().get("269ec017c505493390f02fa6e5700006").getAction());
        assertEquals(Action.AMEND, tradeLoadingService.getLoadedTrades().get("94de9256c1444388a569e9a8f8c00002").getAction());

        //Add another new file
        tradeLoadingService.loadNewTradesFromFile(fileProps.getBaseDirectory() + "/testFiles/sample_3.csv");
        assertEquals(9, tradeLoadingService.getLoadedTrades().keySet().size());
        assertEquals(BigDecimal.valueOf(1800), tradeLoadingService.getLoadedTrades().get("269ec017c505493390f02fa6e5700008").getPrice());
        assertEquals(Action.CANCEL, tradeLoadingService.getLoadedTrades().get("269ec017c505493390f02fa6e5700006").getAction());
    }

    @Test
    public void cleanTrades_Success() {
        List<Trade> rawTrades = List.of(
                new Trade("Test", "TestBBGCode", Currency.USD, Side.B, BigDecimal.valueOf(1000), 1000,
                        "portfolio1", Action.NEW, "Account1", "Strategy1", "User1", LocalDateTime.now(), LocalDate.MIN),
                new Trade("Test2", "TestBBGCode", Currency.USD, Side.B, BigDecimal.valueOf(2000), 2000,
                        "portfolio1", Action.NEW, "Account1", "Strategy1", "User2", LocalDateTime.now(), LocalDate.MIN)
        );
        Map<String, Trade> cleaned = new HashMap<>();
        cleaned = tradeLoadingService.cleanTrades(rawTrades, cleaned);
        assertEquals(2, cleaned.keySet().size());

        //Amend a trade
        List<Trade> rawTrades2 = List.of(
                new Trade("Test", "TestBBGCode", Currency.GBP, Side.B, BigDecimal.valueOf(1000), 1000,
                        "portfolio1", Action.AMEND, "Account1", "Strategy1", "User2", LocalDateTime.now(), LocalDate.MIN)
        );
        cleaned = tradeLoadingService.cleanTrades(rawTrades2, cleaned);
        assertEquals(2, cleaned.keySet().size());
        assertEquals(Action.AMEND, cleaned.get("Test").getAction());
        assertEquals("User2", cleaned.get("Test").getUser());
        assertEquals(Currency.GBP, cleaned.get("Test").getCcy());

        //Amend a trade with older time (nothing should change)
        List<Trade> rawTrades3 = List.of(
                new Trade("Test", "TestBBGCode", Currency.GBP, Side.B, BigDecimal.valueOf(1000), 1000,
                        "portfolio1", Action.AMEND, "Account1", "Strategy1", "User3", LocalDateTime.now().minusDays(1), LocalDate.MIN)
        );
        cleaned = tradeLoadingService.cleanTrades(rawTrades3, cleaned);
        assertEquals(2, cleaned.keySet().size());
        assertEquals(Action.AMEND, cleaned.get("Test").getAction());
        assertEquals("User2", cleaned.get("Test").getUser());

        //Amend a trade with newer time, result should change
        List<Trade> rawTrades4 = List.of(
                new Trade("Test", "TestBBGCode", Currency.GBP, Side.B, BigDecimal.valueOf(1000), 1000,
                        "portfolio1", Action.AMEND, "Account1", "Strategy1", "User4", LocalDateTime.now().plusDays(1), LocalDate.MIN)
        );
        cleaned = tradeLoadingService.cleanTrades(rawTrades4, cleaned);
        assertEquals(2, cleaned.keySet().size());
        assertEquals(Action.AMEND, cleaned.get("Test").getAction());
        assertEquals("User4", cleaned.get("Test").getUser());

        //Cancel a trade (the trade should still be present but as CANCEL)
        List<Trade> rawTrades5 = List.of(
                new Trade("Test", "TestBBGCode", Currency.GBP, Side.B, BigDecimal.valueOf(1000), 1000,
                        "portfolio1", Action.CANCEL, "Account1", "Strategy1", "User2", LocalDateTime.MIN, LocalDate.MIN)
        );
        cleaned = tradeLoadingService.cleanTrades(rawTrades5, cleaned);
        assertEquals(2, cleaned.keySet().size());
        assertEquals(Action.CANCEL, cleaned.get("Test").getAction());

        //Amend the cancelled trade should not work, the trade should still be in CANCEL state
        List<Trade> rawTrades6 = List.of(
                new Trade("Test", "TestBBGCode", Currency.GBP, Side.B, BigDecimal.valueOf(1000), 1000,
                        "portfolio1", Action.AMEND, "Account1", "Strategy1", "User6", LocalDateTime.MIN, LocalDate.MIN)
        );
        cleaned = tradeLoadingService.cleanTrades(rawTrades6, cleaned);
        assertEquals(2, cleaned.keySet().size());
        assertEquals(Action.CANCEL, cleaned.get("Test").getAction());
    }
}