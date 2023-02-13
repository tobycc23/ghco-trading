package com.tobycc.ghcoTrading.model;

import com.opencsv.bean.CsvBindByName;
import com.tobycc.ghcoTrading.model.enums.Currency;
import com.tobycc.ghcoTrading.model.enums.Side;

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