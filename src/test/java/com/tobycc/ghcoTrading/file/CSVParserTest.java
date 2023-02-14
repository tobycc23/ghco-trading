package com.tobycc.ghcoTrading.file;

import com.tobycc.ghcoTrading.model.PnLAggregationRequest;
import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.model.enums.Action;
import com.tobycc.ghcoTrading.model.enums.Currency;
import com.tobycc.ghcoTrading.model.enums.Side;
import com.tobycc.ghcoTrading.props.FileProps;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static com.tobycc.ghcoTrading.model.enums.AggregateField.*;
import static com.tobycc.ghcoTrading.model.enums.AggregateField.USER;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = { CSVParser.class, FileProps.class })
@EnableConfigurationProperties
public class CSVParserTest {

    @Autowired
    private FileProps fileProps;

    @Autowired
    private CSVParser csvParser;

    @Test
    public void checkFileAndReadTrades_Failures() {
        //Is directory
        assertTrue(csvParser.checkFileAndReadTrades(fileProps.getBaseDirectory() + "/testFiles").isEmpty());
        //Not a csv
        assertTrue(csvParser.checkFileAndReadTrades(fileProps.getBaseDirectory() + "/testFiles/not_csv.txt").isEmpty());
        //Csv has wrong headers
        assertTrue(csvParser.checkFileAndReadTrades(fileProps.getBaseDirectory() + "/testFiles/bad_headers.csv").isEmpty());
    }

    @Test
    public void checkFileAndReadTrades_Success() {
        Optional<List<Trade>> trades = csvParser.checkFileAndReadTrades(fileProps.getBaseDirectory() + "/input/sample_1.csv");
        assertTrue(trades.isPresent());
        assertEquals(9, trades.get().size());
    }

    @Test
    public void readTradesFromCsv_Failure() {
        //Failure due to bad input... like a "#" present on the end of a ValueDate field
        assertTrue(csvParser.readTradesFromCsv(fileProps.getBaseDirectory() + "/testFiles/sample_1_bad_input.csv").isEmpty());
    }

    @Test
    public void readTradesFromCsv_Success() {
        Optional<List<Trade>> trades = csvParser.readTradesFromCsv(fileProps.getBaseDirectory() + "/input/sample_1.csv");
        assertTrue(trades.isPresent());
        assertEquals(9, trades.get().size());
    }

    @Test
    public void writeTradesIntoCsv_Success() {
        List<Trade> newTrades = List.of(
                new Trade("Test", "TestBBGCode", Currency.USD, Side.B, BigDecimal.valueOf(1000), 1000,
                        "portfolio1", Action.NEW, "Account1", "Strategy1", "User1", LocalDateTime.MIN, LocalDate.MIN),
                new Trade("Test2", "TestBBGCode", Currency.USD, Side.B, BigDecimal.valueOf(2000), 2000,
                        "portfolio1", Action.NEW, "Account1", "Strategy1", "User2", LocalDateTime.MIN, LocalDate.MIN)
        );

        assertEquals(List.of("Test", "Test2"), csvParser.writeTradesIntoCsv(newTrades));

        String dir = fileProps.getBaseDirectory() + "/" + fileProps.getInputDirectory();
        try (Stream<Path> paths = Files.walk(Paths.get(dir))) {
            //Assert the file is present
            assertEquals(1, paths.filter(f -> f.getFileName().toString().startsWith("trades_via_api_")).count());

            //Clear down
            for(File file: Objects.requireNonNull(new File(dir).listFiles())) {
                if (file.getAbsolutePath().contains("trades_via_api_")) {
                    file.delete();
                }
            }
        } catch (IOException e) {
            //Shouldn't happen
            fail();
        }
    }

    @Test
    public void writeAggregationPositionsIntoCsv_Success() {
        Map<String, List<PnLPosition>> aggregations = Map.of(
                "Test1", List.of(new PnLPosition(LocalDateTime.MIN, BigDecimal.ONE), new PnLPosition(LocalDateTime.MIN.plusMonths(1), BigDecimal.ONE)),
                "Test2", List.of(new PnLPosition(LocalDateTime.now(), BigDecimal.ONE))
        );

        PnLAggregationRequest request = new PnLAggregationRequest(
                Optional.of(new TreeSet<>(Arrays.asList(BBG_CODE, PORTFOLIO, STRATEGY, USER))),
                Optional.of(Currency.USD),
                Optional.empty()
        );

        csvParser.writeAggregationPositionsIntoCsv(aggregations, request);

        String dir = fileProps.getBaseDirectory() + "/" + fileProps.getOutputDirectory();
        File[] files = Objects.requireNonNull(new File(dir).listFiles());
        assertEquals(1, files.length);
        assertTrue(files[0].isDirectory());

        File[] output = Objects.requireNonNull(files[0].listFiles());
        assertEquals(2, output.length);
        FileSystemUtils.deleteRecursively(files[0]);
    }
}