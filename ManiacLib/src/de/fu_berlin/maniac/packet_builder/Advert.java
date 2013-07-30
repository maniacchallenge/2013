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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import de.fu_berlin.maniac.packet_builder.ProtoPackets.AdvertMessage;
import de.fu_berlin.maniac.packet_builder.ProtoPackets.PacketMessage;

/**
 * Adverts are sent ( by the AuctionManager ) to start the bidding process for a
 * Data packet that has to be forwarded.
 * 
 * @author maniacchallenge
 * 
 */

public class Advert extends Packet {
	private int ceil;
	private int deadline;                  // hops left
	private int fine;
	private Inet4Address finalDestinationIP; // BBN router where the advertised packet has to end up at
	private int initialBudget;         // initial budget for the transmission
	  									   // (also the fine for retransmission via BBN)
	
	protected Advert(Inet4Address finalDest, int transactionID, int maxBid,
			int deadline, int fine, int initialBudget) {
		this.type = 'A';
		this.finalDestinationIP = finalDest;
		this.transactionID = transactionID;
		this.ceil = maxBid;
		this.deadline = deadline;
		this.transactionID = transactionID;
		this.fine = fine;
		this.initialBudget = initialBudget;
	}
	
	protected Advert(PacketMessage packetMessage) throws UnknownHostException {
		parse(packetMessage);
	}

	/**
	 * @return the IP of the Backbone the Data should arrive at
	 */
	public Inet4Address getFinalDestinationIP() {
		return finalDestinationIP;
	}

	/**
	 * @return the deadline (in hops).
	 */
	public int getDeadline() {
		return this.deadline;
	}

	/**
	 * @return the ceiling (maximum bid) for this Advert.
	 */
	public int getCeil() {
		return this.ceil;
	}

	/**
	 * @return the fine specified in the Advert.
	 */
	public int getFine() {
		return fine;
	}
	
	public int getInitialBudget() {
		return initialBudget;
	}
	
	@Override
	protected DatagramPacket getDatagramPacket() throws IOException {
		
		byte[] payload = buildPayload();
		return new DatagramPacket(payload, payload.length, this.destinationIP,
				PACKET_PORT);
	}

	private byte[] buildPayload() throws IOException {
				
		AdvertMessage advertMessage = AdvertMessage.newBuilder()
				.setFinalDestinationIP(this.finalDestinationIP.getHostName())
				.setCeil(this.ceil)
				.setDeadline(this.deadline)
				.setFine(this.fine)
				.setInitialBudget(this.initialBudget)
				.build(); 
		
		PacketMessage packetMessage = PacketMessage.newBuilder()
				.setType(PacketMessage.packetType.ADVERT)
				.setTransactionID(this.transactionID)		
				.setAdvertMessage(advertMessage)
				.build();

		System.out.println("built Advert: "+packetMessage);
		
		return packetMessage.toByteArray();
		
	}
	
	private void parse(PacketMessage packetMessage) throws UnknownHostException{
		this.type = 'A';
		this.transactionID = packetMessage.getTransactionID();
		AdvertMessage advertMessage = packetMessage.getAdvertMessage();
		this.finalDestinationIP = (Inet4Address) Inet4Address.getByName(advertMessage.getFinalDestinationIP());
		this.ceil = advertMessage.getCeil();
		this.deadline = advertMessage.getDeadline();
		this.fine = advertMessage.getFine();
		this.initialBudget = advertMessage.getInitialBudget();
	}
	
	@Override
	public String toString() {
		return "ADVERT \n transactionID: " + this.transactionID
				+ " \n SourceIP: " + this.sourceIP + " \n destinationIP: "
				+ this.destinationIP + " \n ceil: " + this.ceil
				+ " \n deadline: " + this.deadline + " \n fine: " + this.fine;
	}
}