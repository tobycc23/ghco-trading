package com.tobycc.ghcoTrading.service;

import com.tobycc.ghcoTrading.model.enums.Currency;

import java.math.BigDecimal;
import java.util.Map;

import static com.tobycc.ghcoTrading.model.enums.Currency.*;

/**
 * This could be done properly - calling out to FX provider to obtain historic or live FX rates (for both sides)
 * and storing these in a csv/db solution. As this is quite a big bit of work in itself, for now we will use simple
 * static mock data.
 *
 * This enables aggregating into a particular currency.
 */
public class FxService {

    public record FxPair(
        Currency from,
        Currency to
    ){ }

    /**
     * Simple static fx map
     */
    public static final Map<FxPair, BigDecimal> FX_MAP = Map.ofEntries(
            getMapEntry(JPY, NOK, "0.077531938"),
            getMapEntry(JPY, KRW, "9.60525"),
            getMapEntry(JPY, EUR, "0.0070910209"),
            getMapEntry(JPY, GBP, "0.0062874509"),
            getMapEntry(JPY, USD, "0.007632253"),

            getMapEntry(NOK, KRW, "123.94419"),
            getMapEntry(NOK, EUR, "0.091494017"),
            getMapEntry(NOK, GBP, "0.081121973"),
            getMapEntry(NOK, USD, "0.098477164"),
            getMapEntry(NOK, JPY, "12.894937"),

            getMapEntry(KRW, EUR, "0.00073831595"),
            getMapEntry(KRW, GBP, "0.00065457653"),
            getMapEntry(KRW, USD, "0.00079457416"),
            getMapEntry(KRW, JPY, "0.10410236"),
            getMapEntry(KRW, NOK, "0.0080737494"),

            getMapEntry(EUR, GBP, "0.88658051"),
            getMapEntry(EUR, USD, "1.076172"),
            getMapEntry(EUR, JPY, "140.98625"),
            getMapEntry(EUR, NOK, "10.933211"),
            getMapEntry(EUR, KRW, "1354.3574"),

            getMapEntry(GBP, USD, "1.214049"),
            getMapEntry(GBP, JPY, "159.05493"),
            getMapEntry(GBP, NOK, "12.334854"),
            getMapEntry(GBP, KRW, "1527.7671"),
            getMapEntry(GBP, EUR, "1.1282449"),

            getMapEntry(USD, JPY, "159.07764"),
            getMapEntry(USD, NOK, "12.334854"),
            getMapEntry(USD, KRW, "1528.2562"),
            getMapEntry(USD, EUR, "1.1283253"),
            getMapEntry(USD, GBP, "1.214049")
    );

    public static Map.Entry<FxPair, BigDecimal> getMapEntry(Currency to, Currency from, String rate) {
        return Map.entry(new FxPair(to, from), new BigDecimal(rate));
    }
}
