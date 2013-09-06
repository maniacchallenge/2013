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

package de.fu_berlin.maniac.packet_builder;

import java.util.HashMap;

import android.annotation.SuppressLint;
import de.fu_berlin.maniac.packet_builder.ProtoPackets.CheckMessage;
import de.fu_berlin.maniac.packet_builder.ProtoPackets.CheckMessage.BalanceUpdateMessage;
import de.fu_berlin.maniac.packet_builder.ProtoPackets.PacketMessage;

/**
 * Checks are sent to nodes over the TCP connection between a node and their
 * backbone router every time a node's bank account changes.
 * 
 * @author maniacchallenge
 * 
 */
@SuppressLint("UseSparseArrays")
public class Check extends Packet {

	private int newBalance;
	private HashMap<Integer,Integer> balanceUpdates;

	protected Check(PacketMessage packetMessage){
		this.balanceUpdates = new HashMap<Integer,Integer>();
		parse(packetMessage);
	}

	/**
	 * @return the amount which has just been added to or subtracted from your
	 *         bank account.
	 */
	public HashMap<Integer,Integer> getBalanceUpdates() {
		return this.balanceUpdates;
	}

	/**
	 * @return the new balance of your bank account
	 */
	public int getNewBalance() {
		return this.newBalance;
	}
	
	private void parse(PacketMessage packetMessage){
		this.type = 'C';
		this.transactionID = packetMessage.getTransactionID();
 		CheckMessage checkMessage = packetMessage.getCheckMessage();
 		this.newBalance = checkMessage.getNewBalance();
		
		if (!checkMessage.getBalanceUpdatesList().isEmpty()){
			
			// retrieve every Update and store it in our balanceUpdates-hashmap
			int transactionID_ = 0;
			int amount_ = 0;
			for (BalanceUpdateMessage update : checkMessage.getBalanceUpdatesList()){
				transactionID_ = update.getTransactionID();
				amount_ = update.getAmount();
				this.balanceUpdates.put(transactionID_, amount_);
			}
		}
		System.out.println("NEW CHECK: "+checkMessage);
	}

	@Override
	public String toString() {
		return "CHECK \n transactionID: " + this.transactionID+ "\n newbalance: " + this.newBalance+"\n sbalanceUpdates: "+this.balanceUpdates;
	}

}
