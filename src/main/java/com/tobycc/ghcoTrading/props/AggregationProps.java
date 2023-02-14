package com.tobycc.ghcoTrading.props;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="aggregation")
public class AggregationProps {

    @NotNull
    private Boolean outputToCsv;

    @NotEmpty
    private Boolean outputToConsole;

    public Boolean isOutputToCsv() {
        return outputToCsv;
    }

    public void setOutputToCsv(Boolean outputToCsv) {
        this.outputToCsv = outputToCsv;
    }

    public Boolean isOutputToConsole() {
        return outputToConsole;
    }

    public void setOutputToConsole(Boolean outputToConsole) {
        this.outputToConsole = outputToConsole;
    }
}