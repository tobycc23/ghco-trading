package com.tobycc.ghcoTrading.props;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="file")
public class FileProps{

    @NotEmpty
    private String baseLocation;

    @NotEmpty
    private String initLoad;

    public String getBaseLocation() {
        return baseLocation;
    }

    public void setBaseLocation(String baseLocation) {
        this.baseLocation = baseLocation;
    }

    public String getInitLoad() {
        return initLoad;
    }

    public void setInitLoad(String initLoad) {
        this.initLoad = initLoad;
    }
}