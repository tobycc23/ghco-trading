package com.tobycc.ghcoTrading.model.enums;

import com.tobycc.ghcoTrading.model.Trade;

import java.util.Set;

public enum AggregateField {
    BBG_CODE,
    ACCOUNT,
    PORTFOLIO,
    STRATEGY,
    USER,
    CURRENCY;

    public static String getAggregateCompositeKey(Trade t, Set<AggregateField> fieldsToGroup) {
        if(fieldsToGroup.isEmpty()) {
            return t.getBbgCode() + "," + t.getAccount() + "," + t.getPortfolio() + ","
                    + t.getStrategy() + "," + t.getUser() + "," + t.getCcy();
        }

        StringBuilder keyComp = new StringBuilder();
        for(AggregateField agg: fieldsToGroup) {
            if(!keyComp.toString().equals("")) keyComp.append(",");
            keyComp.append(switch (agg) {
                case BBG_CODE -> t.getBbgCode();
                case ACCOUNT -> t.getAccount();
                case PORTFOLIO -> t.getPortfolio();
                case STRATEGY -> t.getStrategy();
                case USER -> t.getUser();
                case CURRENCY -> t.getCcy();
            });
        }
        return keyComp.toString();
    }
}
