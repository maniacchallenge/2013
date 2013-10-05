package de.fu_berlin.maniac.general;

import java.util.Hashtable;

import de.fu_berlin.maniac.exception.NegativeBidException;
import de.fu_berlin.maniac.network_manager.Sender;
import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.PacketBuilder;

/**
 * This class makes Mothership wait the amount of time you specified
 * in the return value of onRcvAdv(), before it sends the Bid.
 * @author maniacchallenge
 *
 */
public class BidDelayer implements Runnable {
	ManiacStrategyInterface strategy;
	Advert adv;
	Integer bid;
	Sender sender;
	PacketBuilder packetbuilder;
	Hashtable<Integer, Integer> sent_bids;

	public BidDelayer(ManiacStrategyInterface strat, Advert adv,
			Sender sender) {
		this.strategy = strat;
		this.adv = adv;
		this.sender = sender;
		this.packetbuilder = PacketBuilder.getInstance();

	}

	public void run() {

		
		bid = strategy.sendBid(adv);

		if (bid == null || bid < 0 || bid > adv.getCeil()) {
			bid = adv.getCeil();
		}

		try {
			sender.send(packetbuilder.buildBid(adv, bid));
		} catch (NegativeBidException e) {
			e.printStackTrace();
		}
	}

}
