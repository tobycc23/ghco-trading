package com.tobycc.ghcoTrading.file;

import com.tobycc.ghcoTrading.props.FileProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.*;

@Configuration
@Profile("!(test)")
public class FileWatcherConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileWatcherConfig.class);

    @Autowired
    private FileProps fileProps;

    /**
     * This is an oversimplified version where we only check new files being added with minimal robustness checks
     * In reality would need checks for different modify/delete actions as well as ensuring no concurrency issues
     */
    @Bean
    public WatchService watchService() {
        WatchService watchService = null;
        String inputDir = fileProps.getBaseDirectory() + "/" + fileProps.getInputDirectory();

        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(inputDir);

            if (!Files.isDirectory(path)) {
                throw new RuntimeException("This is not a directory to be monitored: " + path);
            }

            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            LOGGER.info("Starting listening for files added to " + inputDir);
        } catch (IOException e) {
            LOGGER.error("Could not begin the file watcher");
        }
        return watchService;
    }
}
