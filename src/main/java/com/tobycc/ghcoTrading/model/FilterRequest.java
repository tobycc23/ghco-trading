package com.tobycc.ghcoTrading.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import com.tobycc.ghcoTrading.model.enums.Action;
import com.tobycc.ghcoTrading.model.enums.Currency;
import com.tobycc.ghcoTrading.model.enums.Side;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FilterRequest {

        @CsvBindByName(column = "BBGCode")
        private String bbgCode;

        @CsvBindByName(column = "Currency")
        private Currency ccy;

        @CsvBindByName(column = "Side")
        private Side side;

        @CsvBindByName(column = "Portfolio")
        private String portfolio;

        @CsvBindByName(column = "Account")
        private String account;

        @CsvBindByName(column = "Strategy")
        private String strategy;

        @CsvBindByName(column = "User")
        private String user;

}