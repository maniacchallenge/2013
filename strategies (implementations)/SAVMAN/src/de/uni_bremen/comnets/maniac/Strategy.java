package de.uni_bremen.comnets.maniac;

import android.util.Log;

import com.android.internal.util.Predicate;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.fu_berlin.maniac.exception.ManiacException;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.general.ManiacStrategyInterface;
import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.Bid;
import de.fu_berlin.maniac.packet_builder.BidWin;
import de.fu_berlin.maniac.packet_builder.Data;
import de.uni_bremen.comnets.maniac.collections.Predicates;
import de.uni_bremen.comnets.maniac.dummies.DummyAdvert;
import de.uni_bremen.comnets.maniac.log.ResultTracer;
import de.uni_bremen.comnets.maniac.log.Tracer;
import de.uni_bremen.comnets.maniac.ui.OptionsActivity;
import de.uni_bremen.comnets.maniac.util.MutableInteger;

/**
 *
 *
 * Created by Isaac Supeene on 5/28/13
 */
public class Strategy implements ManiacStrategyInterface {
    private static final String TAG = "Maniac Strategy";

	private Brain brain;
    private Maniac maniac;

    private final long AUCTION_TIMEOUT = Brain.getAuctionTimeout();
    private static final long AUCTION_TIMEOUT_BUFFER = 500;
	
	public Strategy(Brain brain, Maniac maniac) {
        Tracer t = new Tracer(TAG, brain);

		this.brain = brain;
        this.maniac = maniac;
		brain.start();

        t.finish();
	}
	
	@Override
	public Long onRcvAdvert(Advert adv) {
        ResultTracer<Long> t = new ResultTracer<Long>(TAG, adv);

        brain.onRcvAdvert(adv);

        if (brain.isWorthBiddingOn(adv)) {
            brain.addFocus(adv);
            return t.finish(AUCTION_TIMEOUT - AUCTION_TIMEOUT_BUFFER);
        }
        else {
            brain.addFocus(adv);
            if (maniac.getSharedPreferences(Maniac.SHARED_PREFERENCES_NAME, 0).getBoolean(OptionsActivity.OPTION_BID_ON_EVERYTHING, false)) {
                return t.finish(AUCTION_TIMEOUT - AUCTION_TIMEOUT_BUFFER);
            }
            else {
                return t.finish(AUCTION_TIMEOUT);
            }
        }
	}

	@Override
	public Integer sendBid(Advert adv) {
        ResultTracer<Integer> t = new ResultTracer<Integer>(TAG, adv);
        int bid = brain.getOptimalBid(adv);
        brain.onSendBid(adv.getTransactionID(), bid);
		return t.finish(bid);
	}

	@Override
	public void onRcvBid(Bid bid) {
        Tracer t = new Tracer(TAG, bid);

        brain.onRcvBid(bid);

        t.finish();
	}

	@Override
	public void onRcvBidWin(BidWin bidwin) {
        Tracer t = new Tracer(TAG, bidwin);

		brain.onRcvBidWin(bidwin);

        t.finish();
	}

	@Override
	public AuctionParameters onRcvData(Data packet) {
        ResultTracer<AuctionParameters> t = new ResultTracer<AuctionParameters>(TAG, packet);

        AuctionParameters optimalParameters = brain.getOptimalParameters(packet);
        DummyAdvert adv = new DummyAdvert(packet.getFinalDestinationIP(), packet.getTransactionID(), optimalParameters.getMaxbid(),
                packet.getHopCount(),optimalParameters.getFine(), packet.getInitialBudget());
        brain.onSendAdvert(adv);

        return t.finish(optimalParameters);
	}

    @Override
    public boolean dropPacketBefore(Data data) {
        ResultTracer<Boolean> t = new ResultTracer<Boolean>(TAG, data);
        return t.finish(brain.dropPacketBefore(data));
    }

	@Override
	public Bid selectWinner(List<Bid> bids, int transactionID) {
        ResultTracer<Bid> t = new ResultTracer<Bid>(TAG, bids);
        try {
            // TODO: validate bids
            // TODO: if there are no bids, check if it would be beneficial to give it to the backbone, and if not, drop the packet
            // (Currently, it's always giving it to the backbone)
            if (bids.isEmpty()) {
                brain.onWinnerSelected(null, transactionID);
                return t.finish(null);
            }
            Bid partnerBid = Predicates.findAny(bids, new Predicate<Bid>() {
                @Override
                public boolean apply(Bid bid) {
                    return bid.getSourceIP().equals(brain.getPartnerIP());
                }
            });
            if (partnerBid != null) {
                brain.onWinnerSelected(partnerBid, transactionID);
                return t.finish(partnerBid);
            }

            // We create a fake bid equivalent to the cost of sending the packet through the backbone.
            Bid nullBid = brain.getNullBid(bids.get(0).getTransactionID());
            bids.add(nullBid);
            final MutableInteger bestValue = new MutableInteger(Integer.MIN_VALUE);
            Bid result = Collections.max(bids, new Comparator<Bid>() {
                @Override
                public int compare(Bid bid1, Bid bid2) {
                    int result = (int)Math.round(brain.getGainOnSuccess(bid1)*brain.getProbabilityOfSuccess(bid1) +
                                                 brain.getGainOnFailure(bid1)*brain.getProbabilityOfFailure(bid1) -
                                                 brain.getGainOnSuccess(bid2)*brain.getProbabilityOfSuccess(bid2) -
                                                 brain.getGainOnFailure(bid2)*brain.getProbabilityOfFailure(bid2));
                    if (result > bestValue.getValue()) {
                        bestValue.setValue(result);
                    }
                    return result;
                }
            });

            if (bestValue.getValue() < 0) {
                brain.onDropPacket(bids.get(0).getTransactionID());
                packetsToDropAfterAuction.add(bids.get(0).getTransactionID());
            }
            else {
                brain.onWinnerSelected(result, transactionID);
            }
            return t.finish(result.equals(nullBid) ? null : result);
        }
        catch (RuntimeException ex) { // I don't think this happens anymore, but I could be completely wrong :)
            ex.printStackTrace();
            brain.onWinnerSelected(bids.get(0), transactionID);
            return t.finish(bids.get(0));
        }
	}

    private Set<Integer> packetsToDropAfterAuction = new HashSet<Integer>();

    @Override
    public boolean dropPacketAfter(Data data) {
        ResultTracer<Boolean> t = new ResultTracer<Boolean>(TAG, data);
        if (packetsToDropAfterAuction.contains(data.getTransactionID())) {
            packetsToDropAfterAuction.remove(data.getTransactionID());
            return t.finish(true);
        }
        else {
            return t.finish(false);
        }
    }

    @Override
	public void onException(ManiacException ex, boolean fatal) {
        if (fatal) {
            Log.wtf(TAG, "A fatal ManiacException was thrown! Terminating the Brain...");
            brain.interrupt();
            ex.printStackTrace();
        }
        else {
            Log.e(TAG, "A non-fatal ManiacException was thrown.");
            ex.printStackTrace();
        }
	}
}
