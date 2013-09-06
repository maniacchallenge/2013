/**
 * This file is part of the API for the Maniac Challenge 2013.
 *
 * The Maniac API is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Maniac API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

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
