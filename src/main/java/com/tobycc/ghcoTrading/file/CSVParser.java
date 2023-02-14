package com.tobycc.ghcoTrading.file;

import com.opencsv.bean.*;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.tobycc.ghcoTrading.model.PnLAggregationRequest;
import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.props.FileProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CSVParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVParser.class);

    private static final String EXPECTED_HEADERS =
            "TradeID,BBGCode,Currency,Side,Price,Volume,Portfolio,Action,Account,Strategy,User,TradeTimeUTC,ValueDate";
    private static final String[] HEADERS =
            new String[]{"TradeID","BBGCode","Currency","Side","Price","Volume","Portfolio","Action","Account", "Strategy","User","TradeTimeUTC","ValueDate"};

    private final FileProps fileProps;

    public CSVParser(FileProps fileProps) {
        this.fileProps = fileProps;
    }

    /**
     * Small robustness checks on file (ensure it is a csv and the headers are as expected - rest left to the csv to bean parser)
     * @param file
     * @return
     * @throws IOException
     */
    public Optional<List<Trade>> checkFileAndReadTrades(String file) {
        if (Files.isDirectory(Paths.get(file))) {
            LOGGER.info("File: " + file + ". File is not a directory, will be ignored");
            return Optional.empty();
        }
        if(!file.endsWith(".csv")) {
            LOGGER.info("File: " + file + ". File is not a csv, will not be parsed");
            return Optional.empty();
        }

        try(BufferedReader in = new BufferedReader(new FileReader(file))) {
            if(!in.readLine().equals(EXPECTED_HEADERS)) {
                LOGGER.info("File: " + file + ". Csv has unexpected headers, will not be parsed");
                return Optional.empty();
            }

            return readTradesFromCsv(file);
        } catch(IOException e) {
            LOGGER.error("Error reading from file: " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<List<Trade>> readTradesFromCsv(String file) {
        LOGGER.info("Reading trades from file: "  + file);
        try {
            FileReader in = new FileReader(file);
            CsvToBean<Trade> csvToBean = new CsvToBeanBuilder<Trade>(in)
                    .withType(Trade.class)
                    //The initial file is in no logical order, and we sort later anyway when aggregating, this speeds up read
                    .withOrderedResults(false)
                    .withSkipLines(1)
                    .build();
            List<Trade> trades = csvToBean.parse();
            in.close();
            LOGGER.info("Completed read of trades from file: " + file);
            return Optional.of(trades);
        } catch (Exception e) {
            LOGGER.error("Trades could not be read in due to: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Write trades coming in via REST call to new csv files. Place these in the input directory, so they are loaded in
     * on next startup too.
     * @param rawTrades
     */
    public List<String> writeTradesIntoCsv(List<Trade> rawTrades) {
        String inputDirectory = fileProps.getBaseDirectory() + "/" + fileProps.getInputDirectory();
        String outputFile = inputDirectory + "/" +
                "trades_via_api_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HH:mm:ss")) + ".csv";
        try {
            Files.createDirectories(Path.of(inputDirectory));
            Writer writer = new FileWriter(outputFile);

            ColumnPositionMappingStrategy<Trade> mappingStrategy = new ColumnPositionMappingStrategy<>() {
                @Override
                public String[] generateHeader(Trade bean) throws CsvRequiredFieldEmptyException {
                    super.generateHeader(bean);
                    return HEADERS;
                }
            };
            mappingStrategy.setType(Trade.class);

            StatefulBeanToCsv<Trade> beanToCsv = new StatefulBeanToCsvBuilder<Trade>(writer)
                    .withApplyQuotesToAll(false)
                    .withMappingStrategy(mappingStrategy)
                    .build();
            LOGGER.info("Writing trades into csv file " + outputFile);
            beanToCsv.write(rawTrades);
            writer.close();
            return rawTrades.stream().map(Trade::getTradeId).toList();
        }  catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
            LOGGER.error("Failed to create csv file " + outputFile + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public void writeAggregationPositionsIntoCsv(Map<String, List<PnLPosition>> pnlAggregated, PnLAggregationRequest request) {
        String outputDirectory = fileProps.getBaseDirectory() + "/" +
                fileProps.getOutputDirectory() + "/aggregation_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HH:mm:ss"));
        try {
            Files.createDirectories(Path.of(outputDirectory));
            LOGGER.info("Output directory " + outputDirectory + " created successfully");

            pnlAggregated.forEach((key,value) -> {
                String outputFile = outputDirectory + "/" + key + request.convertForTitle() + ".csv";
                try {
                    Writer writer = new FileWriter(outputFile);
                    HeaderColumnNameMappingStrategy<PnLPosition> mappingStrategy = new HeaderColumnNameMappingStrategy<>(){
                        @Override
                        public String[] generateHeader(PnLPosition bean) throws CsvRequiredFieldEmptyException {
                            super.generateHeader(bean);
                            return new String[]{"TradeTimeUTC","PnLPosition"};
                        }
                    };
                    mappingStrategy.setType(PnLPosition.class);

                    StatefulBeanToCsv<PnLPosition> beanToCsv = new StatefulBeanToCsvBuilder<PnLPosition>(writer)
                            .withApplyQuotesToAll(false)
                            .withMappingStrategy(mappingStrategy)
                            .build();
                    LOGGER.info("Writing csv file for " + outputFile);
                    beanToCsv.write(value);
                    writer.close();
                }  catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
                    LOGGER.error("Failed to create csv file " + outputFile + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to create directory " + outputDirectory + ": " + e.getMessage());
        }
    }

    //TODO for future iterations, could add a loader for aggregations which can then be visualised
}
