package com.tobycc.ghcoTrading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class GHCOTradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(GHCOTradingApplication.class, args);
    }

}
