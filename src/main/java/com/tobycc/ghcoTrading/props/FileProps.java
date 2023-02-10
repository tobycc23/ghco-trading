package com.tobycc.ghcoTrading.props;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="file")
public class FileProps{

    @NotEmpty
    private String baseDirectory;

    @NotEmpty
    private String inputDirectory;

    @NotEmpty
    private String outputDirectory;

    @NotEmpty
    private boolean outputToCsv;

    @NotEmpty
    private int maxFilesToOutput;

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public String getInputDirectory() {
        return inputDirectory;
    }

    public void setInputDirectory(String inputDirectory) {
        this.inputDirectory = inputDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public boolean isOutputToCsv() {
        return outputToCsv;
    }

    public void setOutputToCsv(boolean outputToCsv) {
        this.outputToCsv = outputToCsv;
    }

    public int getMaxFilesToOutput() {
        return maxFilesToOutput;
    }

    public void setMaxFilesToOutput(int maxFilesToOutput) {
        this.maxFilesToOutput = maxFilesToOutput;
    }
}