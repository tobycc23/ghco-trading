package com.tobycc.ghcoTrading.file;

import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.props.FileProps;
import com.tobycc.ghcoTrading.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;

@Service
public class FileWatcherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileWatcherService.class);

    @Autowired
    private Map<String, Trade> existingTrades;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private FileProps fileProps;

    @Autowired
    private WatchService watchService;

    @EventListener(ContextRefreshedEvent.class)
    public void watcher() throws InterruptedException, IOException {
        WatchKey key;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                String fullPath = fileProps.getBaseDirectory() + "/" + fileProps.getInputDirectory() + "/" + event.context();
                LOGGER.info("File added: " + fullPath);
                tradeService.loadNewTradesFromFile(fullPath, existingTrades);
            }
            key.reset();
        }
    }

}