package de.uni_bremen.comnets.maniac;

import android.util.Log;

import java.net.Inet4Address;

import de.fu_berlin.maniac.exception.NegativeBidException;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.general.Mothership;
import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.Bid;
import de.fu_berlin.maniac.packet_builder.BidWin;
import de.fu_berlin.maniac.packet_builder.Data;
import de.uni_bremen.comnets.maniac.agents.AuctionAgent;
import de.uni_bremen.comnets.maniac.agents.BenefitAnalysisAgent;
import de.uni_bremen.comnets.maniac.agents.HistoryAgent;
import de.uni_bremen.comnets.maniac.agents.TopologyAgent;
import de.uni_bremen.comnets.maniac.dummies.DummyAdvert;
import de.uni_bremen.comnets.maniac.dummies.DummyBid;
import de.uni_bremen.comnets.maniac.dummies.DummyBidWin;

/**
 * This class is intended to run computations in the background so that the Strategy
 * can easily access required information on demand.  The strategy will inform the
 * Brain when any new information is acquired, and will call methods on the Brain
 * to get the values required for its computations.
 *
 * The Brain delegates many tasks to the other Agent classes, each of which have a
 * specific responsibility.  Some of the Agent classes may call methods on one
 * another to acquire necessary information, but the Strategy class itself has no
 * access to them, and manages everything via method calls to the Brain.
 *
 * Created by Isaac Supeene on 5/28/13
 */
public class Brain {
    private static final String TAG = "Strategy Brain";

	private Mothership mothership;

    TopologyAgent topologyAgent = new TopologyAgent();
    BenefitAnalysisAgent benefitAnalysisAgent = new BenefitAnalysisAgent();
    AuctionAgent auctionAgent = new AuctionAgent();
    HistoryAgent historyAgent = new HistoryAgent();

	
	public Brain(Mothership mothership, Maniac maniac) {
		this.mothership = mothership;

        this.topologyAgent = new TopologyAgent();
        this.benefitAnalysisAgent = new BenefitAnalysisAgent();
        this.auctionAgent = new AuctionAgent();
        this.historyAgent = new HistoryAgent();

        topologyAgent.setAuctionAgent(auctionAgent);
        topologyAgent.setHistoryAgent(historyAgent);
        benefitAnalysisAgent.setHistoryAgent(historyAgent);
        benefitAnalysisAgent.setTopologyAgent(topologyAgent);
        //historyAgent.setMothership(mothership);
        historyAgent.setTopologyAgent(topologyAgent);
        auctionAgent.setTopologyAgent(topologyAgent);
        auctionAgent.setHistoryAgent(historyAgent);

        maniac.setHistoryAgent(historyAgent);
        maniac.setAuctionAgent(auctionAgent);
        maniac.setBenefitAnalysisAgent(benefitAnalysisAgent);
        maniac.setTopologyAgent(topologyAgent);
	}

	public void start() {
        topologyAgent.start();
        benefitAnalysisAgent.start();
        auctionAgent.start();
        historyAgent.start();
	}

    public void interrupt() {
        topologyAgent.interrupt();
        benefitAnalysisAgent.interrupt();
        auctionAgent.interrupt();
        historyAgent.interrupt();
    }

	public void onRcvBidWin(BidWin bidwin) {
        historyAgent.onBidWinReceived(bidwin);
        auctionAgent.onBidWinReceived(bidwin);
	}
	
	public void onRcvBid(Bid bid) {
        historyAgent.onBidReceived(bid);
	}

    public void onRcvAdvert(Advert adv) {
        historyAgent.onAdvertReceived(adv);
    }

    public void onSendAdvert(DummyAdvert adv) {
        adv.setSourceIP(topologyAgent.getOurIP());
        historyAgent.onAdvertSent(adv);
    }

    public void onSendBid(int transactionID, int bid) {
        historyAgent.onBidSent(transactionID, bid);
    }

    public void addFocus(Advert adv) {
        // Starts a chain reaction - once the TopologyAgent is ready, it notifies the AuctionAgent, who in turn notifies the Bidding agent.
        topologyAgent.prepareToComputeParameters(adv);
    }

    public Inet4Address getPartnerIP() {
        return topologyAgent.getPartnerIP();
    }

    public int getGainOnSuccess(Bid bid) {
        return benefitAnalysisAgent.getGainOnSuccess(bid);
    }

    public int getGainOnFailure(Bid bid) {
        return benefitAnalysisAgent.getGainOnFailure(bid);
    }

    public double getProbabilityOfSuccess(Bid bid) {
        return topologyAgent.getProbabilityOfSuccess(bid);
    }

    public double getProbabilityOfFailure(Bid bid) {
        return 1 - getProbabilityOfSuccess(bid);
    }

    public AuctionParameters getOptimalParameters(Data data) {
        return auctionAgent.getOptimalParameters(data);
    }

    public int getOptimalBid(Advert adv) {
        return auctionAgent.getOptimalBid(adv);
    }

    public boolean isWorthBiddingOn(Advert adv) {
        return benefitAnalysisAgent.isWorthBiddingOn(adv);
    }

    public boolean dropPacketBefore(Data data) {
        return auctionAgent.dropPacketBefore(data.getTransactionID());
    }

    public void onWinnerSelected(Bid bid, int transactionID) {
        if (bid != null) {
            DummyBidWin bidWin = new DummyBidWin(bid.getTransactionID(), bid.getSourceIP(), bid.getBid(), historyAgent.getOurAdvert(bid.getTransactionID()).getFine());
            bidWin.setSourceIP(topologyAgent.getOurIP());
            historyAgent.onBidWinReceived(bidWin);
        }
        else {
            historyAgent.onBackboneUsed(transactionID);
        }
        historyAgent.onAuctionFinished(transactionID, bid == null ? null : bid.getSourceIP());
    }

    public void onDropPacket(int transactionId) {
        historyAgent.onDropPacket(transactionId);
        historyAgent.onAuctionFinished(transactionId, null);
    }

    public Bid getNullBid(int transactionId) {
        try { // TODO: use the real initial budget when they add a getter.
            return new DummyBid(transactionId, historyAgent.getCurrentAdvert(transactionId).getInitialBudget());
        }
        catch (NegativeBidException ex) {
            Log.wtf(TAG, String.format("The original advert for bid %d had a negative budget!?", transactionId));
            ex.printStackTrace();
            return null;
        }
    }

    public static int getAuctionTimeout() {
        return Mothership.getAuctionTimeout();
    }
}
