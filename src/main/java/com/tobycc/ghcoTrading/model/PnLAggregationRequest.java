package com.tobycc.ghcoTrading.model;

import com.tobycc.ghcoTrading.model.enums.AggregateField;
import com.tobycc.ghcoTrading.model.enums.Currency;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * When actioning an aggregation of pnl positions for trades, we can dial it in by specifying some parameters
 * @param aggregationFields: the fields to aggregate on
 * @param convertIntoCurrency: a currency to convert into (if not set, CURRENCY will be added as an aggregation field)
 * @param filters: define trade information that we wish to filter on
 */
public record PnLAggregationRequest(
        Optional<TreeSet<AggregateField>> aggregationFields,
        Optional<Currency> convertIntoCurrency,
        Optional<Set<TradeFilter>> filters
) {

    public String convertForTitle() {
        return this.convertIntoCurrency().map(c -> " converted to " + c).orElse("") +
                this.filters().map(filters ->
                        " " + filters.stream().map(TradeFilter::toString).collect(Collectors.joining(","))
                ).orElse("");
    }
}
