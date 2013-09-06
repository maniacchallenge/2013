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

import de.fu_berlin.maniac.packet_builder.ProtoPackets.DataMessage;
import de.fu_berlin.maniac.packet_builder.ProtoPackets.PacketMessage;

/**
 * The Data that is being forwarded through the mesh network.
 * 
 * @author maniacchallenge
 * 
 */

public class Data extends Packet {

	private Inet4Address finalDestinationIP; // not the next hop but rather the end node

	public Inet4Address getFinalDestinationIP() {
		return finalDestinationIP;
	}

	private int deadline; // hops left
	private String payload;
	private int fine;
	private int initialBudget;         // initial budget for the transmission
	   								   // (also the fine for retransmission via BBN)

	// create Data-Packet from raw data & automatically decr. hop count.
	public Data(PacketMessage rawdata) throws UnknownHostException {
		parse(rawdata);
	}

	// this exists only for testing purposes. TODO deleteme
	public Data(int transactionID, int deadline, int fine, String payload,
			Inet4Address finalDestination, int initialBudget) {
		this.transactionID = transactionID;
		this.deadline = deadline;
		this.payload = payload;
		this.finalDestinationIP = finalDestination;
		this.type = 'D';
		this.fine = fine;
		this.initialBudget = initialBudget;
	}

	/**
	 * @return the number of hops this Data packet has traveled so far
	 */
	public int getHopCount() {
		return this.deadline;
	}

	/**
	 * @return the fine for the failed delivery of this Data packet
	 */
	public int getFine() {
		return this.fine;
	}
	
	/**
	 * 
	 * @return the initial budget for the transmission. This also the 
	 * fine for retransmission via a Backbone router instead of a Node.
	 */
	public int getInitialBudget(){
		return this.initialBudget;
	}

	// Should this really be public?! TODO
	protected void setFine(int fine) {
		this.fine = fine;
	}

	@Override
	protected DatagramPacket getDatagramPacket() {
		byte[] payload = buildPayload();
		return new DatagramPacket(payload, payload.length, this.destinationIP,
				PACKET_PORT);
	}
	
	protected byte[] getData(){
		return buildPayload();
	}

	private byte[] buildPayload() {
		
		DataMessage dataMessage = DataMessage.newBuilder()
				.setFinalDestinationIP(this.finalDestinationIP.getHostAddress())
				.setDeadline(this.deadline)
				.setFine(this.fine)
				.setInitialBudget(this.initialBudget)
				.setPayload(this.payload)
				.build();
		
		PacketMessage packetMessage = PacketMessage.newBuilder()
				.setType(PacketMessage.packetType.DATA)
				.setTransactionID(this.transactionID)
				.setDataMessage(dataMessage)
				.build();
		
		System.out.println("built Data: "+packetMessage);
		
		return packetMessage.toByteArray();
	}
	
	private void parse(PacketMessage packetMessage ) throws UnknownHostException {
		this.type = 'D';
		this.transactionID = packetMessage.getTransactionID();
		DataMessage dataMessage = packetMessage.getDataMessage();
		this.finalDestinationIP = (Inet4Address) Inet4Address.getByName(dataMessage.getFinalDestinationIP());
		this.deadline = dataMessage.getDeadline() - 1; // automagically decrement Hop Count
		this.fine = dataMessage.getFine();
		this.initialBudget = dataMessage.getInitialBudget();
		this.payload = dataMessage.getPayload();
		
	}

	@Override
	public String toString() {
		return "DATA \n transactionID: " + this.transactionID + "\n SourceIP:"
				+ this.sourceIP + "" + "\n destinationIP:" + this.destinationIP
				+ "\n finalDestination: " + this.finalDestinationIP
				+ "\n deadline: " + this.deadline + "\n payload: " + this.payload;
	}
}
