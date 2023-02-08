package com.tobycc.ghcoTrading.parser;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.props.FileProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

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
        return loadTradesFromCsv(fileProps.getBaseLocation() + "/" + fileProps.getInitLoad());
    }

    public List<Trade> loadTradesFromCsv(String file) throws IOException {
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
}
