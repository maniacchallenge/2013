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

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import de.fu_berlin.maniac.exception.NegativeBidException;
import de.fu_berlin.maniac.packet_builder.ProtoPackets.BidMessage;
import de.fu_berlin.maniac.packet_builder.ProtoPackets.PacketMessage;

/**
 * 
 * Bids are a node's answer to an Advert.
 * 
 * @author maniacchallenge
 * 
 */

public class Bid extends Packet {
	private int bid;
	
	protected Bid( int transactionID, int bid) throws NegativeBidException {

		// check for illegal bids
		if (bid < 0)
			throw new NegativeBidException();

		this.type = 'B';
		this.bid = bid;
		this.transactionID = transactionID;

	}

	protected Bid(PacketMessage rawdata) throws UnknownHostException {
		parse(rawdata);
	}

	/**
	 * @return the value of the bid
	 */
	public int getBid() {
		return this.bid;
	}
	
	@Override
	protected DatagramPacket getDatagramPacket() {
		byte[] payload = buildPayload();
		return new DatagramPacket(payload, payload.length, this.destinationIP,
				PACKET_PORT);
	}

	private byte[] buildPayload() {
		BidMessage bidMessage = BidMessage.newBuilder()
				.setBid(this.bid)
				.build();
		
		PacketMessage packetMessage = PacketMessage.newBuilder()
				.setType(PacketMessage.packetType.BID)
				.setTransactionID(this.transactionID)
				.setBidMessage(bidMessage)
				.build();
		
		System.out.println("built Bid: "+packetMessage);
		
		return packetMessage.toByteArray();
	}
	
	private void parse(PacketMessage packetMessage ) throws UnknownHostException {
		this.type = 'B';
		this.transactionID = packetMessage.getTransactionID();
		BidMessage bidMessage = packetMessage.getBidMessage();
		this.bid = bidMessage.getBid();

	}
	

	@Override
	public String toString() {
		return "BID\n transactionID: " + this.transactionID + "\n SourceIP: "
				+ this.sourceIP + "\n destinationIP: " + this.destinationIP
				+ "\n bid: " + this.bid;
	}
}
