package com.tobycc.ghcoTrading.service;

import com.tobycc.ghcoTrading.model.Trade;
import com.tobycc.ghcoTrading.model.enums.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeService.class);

    /**
     * Filtering the list of trades so that only the highest priority of each trade, in the following order, is present:
     * NEW -> AMEND (latest amended) -> CANCEL
     *
     * Note: We assume here that an AMEND cannot modify the TradeTimeInUTC information, as we use this to determine which is
     * the latest AMEND.
     * TODO: It would be good to have an extra column for "DateTimeCreated" to show when the record itself was created
     *  so that the TradeTimeInUTC could potentially be modified and we can still tell when the most recent AMEND arrives,
     *  irrespective of this.
     *
     * @return filtered Map of trades
     */
    @Bean
    public Map<String,Trade> filterTrades(List<Trade> unfilteredTrades) {
        Map<String,Trade> filteredTrades = new HashMap<>();

        unfilteredTrades.forEach(t ->
                //We filter the trades into a map
                filteredTrades.merge(
                    t.getTradeId(),
                    t,
                    //The following remapping could be replaced by calling `robustFiltering(currTrade, newTrade)`
                    //for thorough robustness checks on the data.
                    (currTrade, newTrade) -> {
                        //Current trade is NEW, lowest priority, must be overwritten. Or if new trade arriving is CANCEL,
                        //highest priority, it must overwrite.
                        if(currTrade.getAction().equals(Action.NEW) || newTrade.getAction().equals(Action.CANCEL)) {
                            return newTrade;
                        }

                        //Current trade is CANCEL, highest priority, cannot be overwritten
                        if(currTrade.getAction().equals(Action.CANCEL)) {
                            return currTrade;
                        }

                        //Current trade is otherwise AMEND. New trade to process could be NEW or AMEND with older time,
                        // in which case cannot overwrite. Otherwise, it is an AMEND with a newer time, so must overwrite.
                        return newTrade.getAction().equals(Action.NEW) || newTrade.getDateTime().isBefore(currTrade.getDateTime())
                                ? currTrade : newTrade;
                    }
            )
        );

        return filteredTrades;
    }

    /**
     * This is some robust filtering, which may be overkill, so I haven't used it in the main code but made a point to keep
     * it in for completeness.
     *
     * Potential outliers could exist if trades are not clocked in their correct action priority order, as described above.
     * There is a strong argument for any trade with an outlier described below to be removed until brought to the attention of a
     * business team and rectified. Examples:
     *
     * - A NEW trade, of same TradeID, clocked after an AMEND or CANCEL
     * - An AMEND trade clocked after a CANCEL
     * For now, this will be considered anomalous data and will be flagged via logging but not processed
     * (i.e. will not overwrite the existing trade).
     *
     * - Multiple NEWs or CANCELs of same TradeID
     * For now, the latest to arrive will be used and will be flagged via logging. For CANCELs this has no consequence to
     * overall aggregation calcs.
     *
     * - Multiple AMENDs (or even NEWs) of same TradeID have the exact same time but different data
     * This is a bigger issue that must be resolved with a business team. The aggregate calculations will be redundant
     * until the matter is resolved, especially if aspects like the price vary dramatically.
     * For now, the most recent to be processed will be used. Two CANCELs with the same time will be flagged, but of no consequence.
     *
     * - Only an AMEND or CANCEL exist without the presence of a NEW, of same TradeID
     * These will still be included as is. Harder to spot as we wouldn't know by looking at the map, as the higher priority
     * action trades replace the old, and trades can be processed in any order. Could sort first and check NEW is always
     * present first, but this wastes compute time.
     *
     * @return
     */
    public Trade robustFiltering(Trade currTrade, Trade newTrade) {
        if(currTrade.getAction().equals(Action.NEW)) {
            if(newTrade.getAction().equals(Action.NEW)) {
                LOGGER.warn("Trade " + newTrade.getTradeId() + " has multiple NEW records related to it.");
                return getTradeOfSameAction(currTrade, newTrade);
            }

            //Otherwise newTrade is AMEND or CANCEL so all good
            return newTrade;
        }
        else if(currTrade.getAction().equals(Action.AMEND)) {
            if(newTrade.getAction().equals(Action.NEW)) {
                if(!newTrade.getDateTime().isBefore(currTrade.getDateTime())) {
                    LOGGER.warn("Trade " + newTrade.getTradeId() + " has a NEW record clocked after or at same time as an AMEND. Ignoring for now.");
                }
                return currTrade;
            }
            if(newTrade.getAction().equals(Action.CANCEL)) {
                return newTrade;
            }
        }
        //Else the currTrade is CANCEL
        else {
            if(newTrade.getAction().equals(Action.NEW) || newTrade.getAction().equals(Action.AMEND)) {
                if(!newTrade.getDateTime().isBefore(currTrade.getDateTime())) {
                    LOGGER.warn("Trade " + newTrade.getTradeId() + " has a " + newTrade.getAction() +
                            " record clocked after or at same time as a CANCEL. Ignoring for now.");
                }
                return currTrade;
            }
        }

        //Otherwise newTrade has same action as currTrade (inconsequential which we return in case of CANCEL)
        return getTradeOfSameAction(currTrade, newTrade);
    }

    public Trade getTradeOfSameAction(Trade currTrade, Trade newTrade) {
        if (newTrade.getDateTime().isEqual(currTrade.getDateTime())) {
            LOGGER.warn("The trade's records are indistinguishable by time, using most recently processed");
            return newTrade;
        }

        return newTrade.getDateTime().isBefore(currTrade.getDateTime()) ? currTrade : newTrade;
    }
}
