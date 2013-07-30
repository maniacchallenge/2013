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
import java.net.UnknownHostException;

import de.fu_berlin.maniac.packet_builder.ProtoPackets.BidWinMessage;
import de.fu_berlin.maniac.packet_builder.ProtoPackets.PacketMessage;

/**
 * The node that has started an auction by sending an Advert will announce the
 * winner by broadcasting a BidWin packet.
 * 
 * @author maniacchallenge
 * 
 */

public class BidWin extends Packet {
	private Inet4Address winnerIP;
	private int winningBid;
	private int fine;

	protected BidWin(int transactionID, Inet4Address winnerIP, int winningBid,
			int fine) {
		this.type = 'W';
		this.winnerIP = winnerIP;
		this.transactionID = transactionID;
		this.winningBid = winningBid;
		this.fine = fine;
	}

	protected BidWin(PacketMessage rawdata) throws UnknownHostException {
		parse(rawdata);
	}

	/**
	 * @return the IP of the node that won the auction.
	 */
	public Inet4Address getWinnerIP() {
		return this.winnerIP;
	}

	/**
	 * @return the value of the winning bid
	 */
	public int getWinningBid() {
		return this.winningBid;
	}

	/**
	 * @return the fine
	 */
	public int getFine() {
		return this.fine;
	}

	@Override
	protected DatagramPacket getDatagramPacket() {
		byte[] payload = buildPayload();
		return new DatagramPacket(payload, payload.length, this.destinationIP,
				PACKET_PORT);
	}
	
	private byte[] buildPayload() {
		
		BidWinMessage bidWinMessage = BidWinMessage.newBuilder()
				.setWinnerIP(this.winnerIP.getHostAddress())
				.setWinningBid(this.winningBid)
				.setFine(this.fine)
				.build();
		
		PacketMessage packetMessage = PacketMessage.newBuilder()
				.setType(PacketMessage.packetType.BIDWIN)
				.setTransactionID(this.transactionID)
				.setBidWinMessage(bidWinMessage)
				.build();
				
		return packetMessage.toByteArray();
	}
	
	private void parse(PacketMessage packetMessage ) throws UnknownHostException {
		this.type = 'W';
		this.transactionID = packetMessage.getTransactionID();
		BidWinMessage bidWinMessage = packetMessage.getBidWinMessage();
		this.winnerIP = (Inet4Address) Inet4Address.getByName(bidWinMessage.getWinnerIP());
		this.winningBid = bidWinMessage.getWinningBid();
		this.fine = bidWinMessage.getFine();
		
	}

	protected byte[] getStreamableData() {
		return buildPayload();
	}

	@Override
	public String toString() {
		return "BIDWN \n transactionID: " + this.transactionID + "\n SourceIP:"
				+ this.sourceIP + "" + "\n destinationIP:" + this.destinationIP
				+ "\n WinnerIP: " + this.winnerIP;
	}
}
