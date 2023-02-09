package com.tobycc.ghcoTrading.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import com.tobycc.ghcoTrading.model.enums.Action;
import com.tobycc.ghcoTrading.model.enums.Currency;
import com.tobycc.ghcoTrading.model.enums.Side;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Trade{

        @CsvBindByName(column = "TradeID", required = true)
        private String tradeId;

        @CsvBindByName(column = "BBGCode", required = true)
        private String bbgCode;

        @CsvBindByName(column = "Currency", required = true)
        private Currency ccy;

        @CsvBindByName(column = "Side", required = true)
        private Side side;

        @CsvBindByName(column = "Price", required = true)
        private BigDecimal price;

        @CsvBindByName(column = "Volume", required = true)
        private Integer volume;

        @CsvBindByName(column = "Portfolio", required = true)
        private String portfolio;

        @CsvBindByName(column = "Action", required = true)
        private Action action;

        @CsvBindByName(column = "Account", required = true)
        private String account;

        @CsvBindByName(column = "Strategy", required = true)
        private String strategy;

        @CsvBindByName(column = "User", required = true)
        private String user;

        @CsvBindByName(column = "TradeTimeUTC", required = true)
        @CsvDate("yyyy-MM-dd\'T\'HH:mm:ss.SSSSSS")
        private LocalDateTime dateTime;

        @CsvBindByName(column = "ValueDate", required = true)
        @CsvDate("yyyyMMdd")
        private LocalDate date;

        public Trade() {
        }

        public String getTradeId() {
                return tradeId;
        }

        public LocalDateTime getDateTime() {
                return dateTime;
        }

        public Action getAction() {
                return action;
        }

        public String getBbgCode() {
                return bbgCode;
        }

        public String getPortfolio() {
                return portfolio;
        }

        public String getAccount() {
                return account;
        }

        public String getStrategy() {
                return strategy;
        }

        public String getUser() {
                return user;
        }

        public Currency getCcy() {
                return ccy;
        }

        public Side getSide() {
                return side;
        }

        public BigDecimal getPrice() {
                return price;
        }

        public Integer getVolume() {
                return volume;
        }
}