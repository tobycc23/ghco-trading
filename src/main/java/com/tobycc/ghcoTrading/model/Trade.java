package com.tobycc.ghcoTrading.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvDate;
import com.tobycc.ghcoTrading.model.enums.Action;
import com.tobycc.ghcoTrading.model.enums.Currency;
import com.tobycc.ghcoTrading.model.enums.Side;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Trade{

        @NotEmpty
        @CsvBindByName(column = "TradeID", required = true)
        @CsvBindByPosition(position = 0)
        private String tradeId;

        @NotEmpty
        @CsvBindByName(column = "BBGCode", required = true)
        @CsvBindByPosition(position = 1)
        private String bbgCode;

        @NotNull
        @CsvBindByName(column = "Currency", required = true)
        @CsvBindByPosition(position = 2)
        private Currency ccy;

        @NotNull
        @CsvBindByName(column = "Side", required = true)
        @CsvBindByPosition(position = 3)
        private Side side;

        @NotNull
        @CsvBindByName(column = "Price", required = true)
        @CsvBindByPosition(position = 4)
        private BigDecimal price;

        @NotNull
        @CsvBindByName(column = "Volume", required = true)
        @CsvBindByPosition(position = 5)
        private Integer volume;

        @NotEmpty
        @CsvBindByName(column = "Portfolio", required = true)
        @CsvBindByPosition(position = 6)
        private String portfolio;

        @NotNull
        @CsvBindByName(column = "Action", required = true)
        @CsvBindByPosition(position = 7)
        private Action action;

        @NotEmpty
        @CsvBindByName(column = "Account", required = true)
        @CsvBindByPosition(position = 8)
        private String account;

        @NotEmpty
        @CsvBindByName(column = "Strategy", required = true)
        @CsvBindByPosition(position = 9)
        private String strategy;

        @NotEmpty
        @CsvBindByName(column = "User", required = true)
        @CsvBindByPosition(position = 10)
        private String user;

        @NotNull
        @CsvBindByName(column = "TradeTimeUTC", required = true)
        @CsvBindByPosition(position = 11)
        @CsvDate("yyyy-MM-dd\'T\'HH:mm:ss.SSSSSS")
        private LocalDateTime dateTime;

        @NotNull
        @CsvBindByName(column = "ValueDate", required = true)
        @CsvBindByPosition(position = 12)
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

        public LocalDate getDate() {
                return date;
        }
}