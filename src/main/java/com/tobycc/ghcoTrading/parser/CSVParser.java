package com.tobycc.ghcoTrading.parser;

import com.opencsv.CSVWriter;
import com.opencsv.bean.*;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.tobycc.ghcoTrading.model.PnLPosition;
import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.model.enums.Currency;
import com.tobycc.ghcoTrading.props.FileProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class CSVParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVParser.class);

    private final FileProps fileProps;

    public CSVParser(FileProps fileProps) {
        this.fileProps = fileProps;
    }

    @Bean
    public List<Trade> loadInitialTrades() throws IOException {
        LOGGER.info("Beginning load of initial sample of trades");
        return readTradesFromCsv(fileProps.getBaseLocation() + "/" + fileProps.getInitLoad());
    }

    public List<Trade> readTradesFromCsv(String file) throws IOException {
        LOGGER.info("Beginning load of trades from file: "  + file);
        try {
            FileReader in = new FileReader(file);
            CsvToBean<Trade> csvToBean = new CsvToBeanBuilder<Trade>(in)
                    .withType(Trade.class)
                    //The initial file is in no logical order, and we sort later anyway when aggregating, this speeds up read
                    .withOrderedResults(false)
                    .build();
            List<Trade> trades = csvToBean.parse();
            in.close();
            LOGGER.info("Completed load of trades from file: " + file);
            return trades;
        } catch (IOException e) {
            LOGGER.error("Trades could not be loaded in");
            throw e;
        }
    }

    public void writeAggregationPositionsIntoCsv(Map<String, List<PnLPosition>> pnlAggregated, Currency aggregatedIntoCurrency) {
        String outputDirectory = fileProps.getBaseLocation() + "/" + fileProps.getOutputLocation() + "/" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        try {
            Files.createDirectories(Path.of(outputDirectory));
            LOGGER.info("Output directory " + outputDirectory + " created successfully");

            pnlAggregated.forEach((key,value) -> {
                String outputFile = outputDirectory + "/" + key + "_in_currency_" + aggregatedIntoCurrency + ".csv";
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
                    LOGGER.info("Creating csv file for " + outputFile);
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


    static class CustomMappingStrategy<T> extends HeaderColumnNameTranslateMappingStrategy<T> {
        @Override
        public String getColumnName(int col) {
            return col==0 ? "taaer" : "asdfabdf";
        }
    }
}
