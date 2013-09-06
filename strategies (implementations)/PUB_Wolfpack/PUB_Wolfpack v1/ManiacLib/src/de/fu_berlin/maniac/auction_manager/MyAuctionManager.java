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

import java.util.ArrayList;
import java.util.Hashtable;


import de.fu_berlin.maniac.network_manager.Sender;
import de.fu_berlin.maniac.packet_builder.*;

/**
 * This Class manages your auctions. It provides the function to start a new
 * Auction and to get all the bids for a certain transaction. You don't need to
 * call getBidsByID() or getAuctions() by yourself, this will be done by the
 * class Auction, which calls your selectWinner() function.
 * 
 * @author maniacchallenge
 */
public final class MyAuctionManager {

	/**
	 * Hashtable of all the lists of bids for all your transactions
	 */
	Hashtable<Integer, ArrayList<Bid>> auctions;

	/**
	 * Just the constructor
	 */
	public MyAuctionManager() {
		auctions = new Hashtable<Integer, ArrayList<Bid>>(100);
	}

	/**
	 * 
	 * @return The complete Hashtable with all bids from all transactions
	 */
	public Hashtable<Integer, ArrayList<Bid>> getAuctions() {
		return auctions;
	}

	/**
	 * 
	 * @param transID
	 *            The Transaction-ID of the auction
	 * @return Just the Bids for a certain Auction
	 */
	public ArrayList<Bid> getBidsByID(int transID) {
		return auctions.get(transID);
	}

	/**
	 * Use this function to start a new Auction. You will need an Advert-Packet
	 * to start one.
	 * 
	 * @param adv
	 *            The new Advert-Packet you've created
	 * @param sender
	 *            The Sender-Class which will broadcast the new advert
	 */
	public void handleAuction(Advert adv, Sender sender) {
		auctions.put(adv.getTransactionID(), new ArrayList<Bid>());
		sender.send(adv);
	}

}
