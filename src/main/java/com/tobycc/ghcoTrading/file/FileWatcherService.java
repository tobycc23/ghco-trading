package com.tobycc.ghcoTrading.file;

import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.props.FileProps;
import com.tobycc.ghcoTrading.service.TradeLoadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;

@Service
@Profile("!(test)")
public class FileWatcherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileWatcherService.class);

    private final TradeLoadingService tradeService;
    private final FileProps fileProps;
    private final WatchService watchService;

    public FileWatcherService(TradeLoadingService tradeService, FileProps fileProps, WatchService watchService) {
        this.tradeService = tradeService;
        this.fileProps = fileProps;
        this.watchService = watchService;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void watcher() throws InterruptedException, IOException {
        WatchKey key;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                String fullPath = fileProps.getBaseDirectory() + "/" + fileProps.getInputDirectory() + "/" + event.context();
                LOGGER.info("File added: " + fullPath);
                tradeService.loadNewTradesFromFile(fullPath);
            }
            key.reset();
        }
    }

}