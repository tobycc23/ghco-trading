package com.tobycc.ghcoTrading;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
@OpenAPIDefinition(info=@Info(title="GHCO Trading Task"))
public class GHCOTradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(GHCOTradingApplication.class, args);
    }

}
