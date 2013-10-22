package de.fu_berlin.maniac.packet_builder;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import de.fu_berlin.maniac.exception.NegativeBidException;

/**
 * 
 * Bids are a node's answer to an Advert.
 * 
 * @author maniacchallenge
 * 
 */

public class Bid extends Packet {
	private final static int BID_LENGTH = 13;
	private int bid;
	
	protected Bid( int transactionID, int bid) throws NegativeBidException {

		// check for illegal bids
		if (bid < 0)
			throw new NegativeBidException();

		this.type = 'B';
		this.bid = bid;
		this.transactionID = transactionID;
	}

	protected Bid(byte[] rawdata) {
		parseFromByteArray(rawdata);
	}

	/**
	 * @return the value of the bid
	 */
	public int getBid() {
		return this.bid;
	}

	private void parseFromByteArray(byte[] payload) {

		this.transactionID = loadFromByteArray(payload, 1); // Transaction ID
		this.bid = loadFromByteArray(payload, 5); // Bid

		// Destination
		byte[] address = new byte[4];
		for (int i = 0; i < 4; i++)
			address[i] = payload[9 + i];
		try {
			this.destinationIP = (Inet4Address) InetAddress
					.getByAddress(address);
		} catch (UnknownHostException e) {
			this.destinationIP = null;
			e.printStackTrace();
		}
		this.type = (char) payload[0];

	}

	private byte[] parseToByteArray() {
		byte[] payload = new byte[BID_LENGTH];

		payload[0] = (byte) this.type; // Insert packet type
		saveToByteArray(getTransactionID(), payload, 1); // Insert transaction
															// ID
		saveToByteArray(getBid(), payload, 5); // Insert bid

		// Insert destination address
		byte[] address = getDestinationIP().getAddress();
		for (int i = 0; i < 4; i++)
			payload[9 + i] = address[i];

		return payload;
	}

	@Override
	protected DatagramPacket getDatagramPacket() {
		byte[] payload = parseToByteArray();
		return new DatagramPacket(payload, BID_LENGTH, this.destinationIP,
				PACKET_PORT);
	}

	@Override
	protected String getStreamableData() {
		String s = "B " + this.transactionID + " " + this.bid + " "
				+ this.destinationIP + " $";
		return s;
	}

	@Override
	public String toString() {
		return "BID\n transactionID: " + this.transactionID + "\n SourceIP: "
				+ this.sourceIP + "\n destinationIP: " + this.destinationIP
				+ "\n bid: " + this.bid;
	}
}
