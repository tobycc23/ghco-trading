package com.tobycc.ghcoTrading.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PnLPosition(
        LocalDateTime dateTime,
        BigDecimal position) {

}
