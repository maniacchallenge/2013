package de.uni_bremen.comnets.maniac.agents;

import android.util.Log;

import com.android.internal.util.Predicate;

import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.Bid;
import de.uni_bremen.comnets.maniac.collections.Predicates;
import de.uni_bremen.comnets.maniac.devices.Device;

/**
 * The BenefitAnalysisAgent determines the net gain or loss associated
 * with a particular outcome.  This takes into account not only immediate
 * loss or gain to our credit, but more generally, how the outcome will
 * affect our overall probability of victory.
 *
 * Created by Isaac Supeene on 6/11/13.
 */
public class BenefitAnalysisAgent extends Agent {

    private HistoryAgent historyAgent;      public void setHistoryAgent(HistoryAgent agent) {
        historyAgent = agent;
    }
    private TopologyAgent topologyAgent;    public void setTopologyAgent(TopologyAgent agent) {
        topologyAgent = agent;
    }

    public int getGainOnSuccess(Bid bid) {
        // TODO: Consider more abstract things like our reputation, and the gain of other nodes.
        return historyAgent.getOurBid(bid.getTransactionID()) - bid.getBid();
    }

    // Expected to be negative.
    public int getGainOnFailure(Bid bid) {
        // TODO: Consider more abstract things like our reputation, and the gain of other nodes.
        if (historyAgent.getOurAdvert(bid.getTransactionID()) != null && historyAgent.getPreviousAdvert(bid.getTransactionID()) != null) {
            return historyAgent.getOurAdvert(bid.getTransactionID()).getFine() - historyAgent.getPreviousAdvert(bid.getTransactionID()).getFine();
        }
        else {
            if (historyAgent.getOurAdvert(bid.getTransactionID()) == null) {
                Log.w(TAG(), "Calculating gainOnFailure for a bid with no known current auction (ID " + bid.getTransactionID() + ")!");
            }
            if (historyAgent.getPreviousAdvert(bid.getTransactionID()) == null) {
                Log.w(TAG(), "Calculating gainOnFailure for a bid with no known previous auction (ID " + bid.getTransactionID() + ")!");
            }
            return 0; // TODO: Make sure this doesn't happen.
        }
    }

    public boolean isWorthBiddingOn(final Advert advert) {
        return topologyAgent.getLeastNumberOfHops(topologyAgent.getOurIP(), advert.getFinalDestinationIP(), advert.getTransactionID()) > advert.getDeadline() || Predicates.any(topologyAgent.getNeighbors(), new Predicate<Device>() {
            @Override
            public boolean apply(Device device) {
                Device source = topologyAgent.getDevice(advert.getSourceIP());
                return !device.isBackbone() && device.isVulnerableToDeadPacketTrick() && (source.isBackbone() || source.getBalance() < device.getBalance());
            }
        });
    }
}