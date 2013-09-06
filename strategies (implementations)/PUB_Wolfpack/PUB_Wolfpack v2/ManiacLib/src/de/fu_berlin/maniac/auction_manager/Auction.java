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

package de.fu_berlin.maniac.auction_manager;

import java.net.Inet4Address;
import java.net.InetAddress;

import de.fu_berlin.maniac.exception.NegativeBidException;
import de.fu_berlin.maniac.general.ManiacStrategyInterface;
import de.fu_berlin.maniac.network_manager.Sender;
import de.fu_berlin.maniac.packet_builder.*;

/**
 * This class actually handles a specific auction. It determines the winner,
 * using your selectWinner() function, updates the Data-Packet, creates a new
 * BidWin-Packet and sends them.
 * @author maniacchallenge
 *
 */
public final class Auction implements Runnable {
	final int BACKBONE_MAXBID = 100;

	Data pack;
	Sender sender;
	ManiacStrategyInterface strat;
	MyAuctionManager auctionmanager;
	PacketBuilder packetbuilder;
	InetAddress myBackbone;

	public Auction(Data pack, Sender sender, ManiacStrategyInterface strat,
			MyAuctionManager auctionmanager, InetAddress backbone) {
		this.pack = pack;
		this.sender = sender;
		this.strat = strat;
		this.auctionmanager = auctionmanager;
		this.packetbuilder = PacketBuilder.getInstance();
		this.myBackbone = backbone;
	}

	@Override
	public void run() {

		
		if(!strat.dropPacketAfter(pack)){
			
			/* winner = null makes the node send the packet to the backbone */
			Bid winner = strat.selectWinner(auctionmanager.getBidsByID(pack
					.getTransactionID()));
			
			if (winner != null) {
				sender.send(packetbuilder.buildBidWin(winner, pack.getFine()));
				packetbuilder.updateData((Data) pack, winner.getSourceIP());
				sender.send(pack);
			} else {
				
				if(!myBackbone.equals(null)){
					packetbuilder.updateData((Data) pack,
							(Inet4Address) this.myBackbone);
					try {
						winner = (Bid) packetbuilder.buildDummyBid(
								pack.getTransactionID(), pack.getInitialBudget());
						if(!winner.equals(null)){
							sender.send(packetbuilder.buildBidWin(winner, pack.getFine()));
							sender.send(pack);
							
						}
					} catch (NegativeBidException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		}
		auctionmanager.getAuctions().remove(pack.getTransactionID());

	}

}