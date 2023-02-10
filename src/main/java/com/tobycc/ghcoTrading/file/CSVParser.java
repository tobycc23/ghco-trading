package com.tobycc.ghcoTrading.file;

import com.opencsv.bean.*;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.model.enums.Currency;
import com.tobycc.ghcoTrading.props.FileProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CSVParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVParser.class);

    private static final String EXPECTED_HEADERS = "TradeID,BBGCode,Currency,Side,Price,Volume,Portfolio,Action,Account,Strategy,User,TradeTimeUTC,ValueDate";

    @Autowired
    private FileProps fileProps;

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

            return Optional.of(readTradesFromCsv(file));
        } catch(IOException e) {
            LOGGER.error("Error reading from file: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<Trade> readTradesFromCsv(String file) throws IOException {
        LOGGER.info("Reading trades from file: "  + file);
        try {
            FileReader in = new FileReader(file);
            CsvToBean<Trade> csvToBean = new CsvToBeanBuilder<Trade>(in)
                    .withType(Trade.class)
                    //The initial file is in no logical order, and we sort later anyway when aggregating, this speeds up read
                    .withOrderedResults(false)
                    .build();
            List<Trade> trades = csvToBean.parse();
            in.close();
            LOGGER.info("Completed read of trades from file: " + file);
            return trades;
        } catch (IOException e) {
            LOGGER.error("Trades could not be read in");
            throw e;
        }
    }

    public void writeAggregationPositionsIntoCsv(Map<String, List<PnLPosition>> pnlAggregated, Optional<Currency> convertIntoCurrency) {
        String outputDirectory = fileProps.getBaseDirectory() + "/" + fileProps.getOutputDirectory() + "/" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        try {
            Files.createDirectories(Path.of(outputDirectory));
            LOGGER.info("Output directory " + outputDirectory + " created successfully");

            pnlAggregated.forEach((key,value) -> {
                String outputFile = outputDirectory + "/" + key +
                        convertIntoCurrency.map(c -> " converted to " + c).orElse("") + ".csv";
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
                }  catch (IOException e) {
                    LOGGER.error("Failed to create csv file " + outputFile + ": " + e.getMessage());
                } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to create directory " + outputDirectory + ": " + e.getMessage());
        }
    }
}
