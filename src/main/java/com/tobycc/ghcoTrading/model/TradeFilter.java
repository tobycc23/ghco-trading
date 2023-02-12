package com.tobycc.ghcoTrading.model;

import com.tobycc.ghcoTrading.model.enums.Currency;

import java.util.Optional;

public record TradeFilter(
        Optional<String> bbgCode,
        Optional<Currency> ccy,
        Optional<String> portfolio,
        Optional<String> account,
        Optional<String> strategy,
        Optional<String> user
){
        public boolean filter(Trade t) {
                return (this.bbgCode().isEmpty() || this.bbgCode().get().equals(t.getBbgCode())) &&
                        (this.ccy().isEmpty() || this.ccy().get().equals(t.getCcy())) &&
                        (this.portfolio().isEmpty() || this.portfolio().get().equals(t.getPortfolio())) &&
                        (this.account().isEmpty() || this.account().get().equals(t.getAccount())) &&
                        (this.strategy().isEmpty() || this.strategy().get().equals(t.getStrategy())) &&
                        (this.user().isEmpty() || this.user().get().equals(t.getUser()));
        }

        @Override
        public String toString() {
                return "TradeFilter{" +
                        (bbgCode.map(s -> "bbgCode=" + s + ",").orElse("")) +
                        (ccy.map(s -> "ccy=" + s + ",").orElse("")) +
                        (portfolio.map(s -> "portfolio=" + s + ",").orElse("")) +
                        (account.map(s -> "account=" + s + ",").orElse("")) +
                        (strategy.map(s -> "strategy=" + s + ",").orElse("")) +
                        (user.map(s -> "user=" + s).orElse("")) +
                        '}';
        }
}