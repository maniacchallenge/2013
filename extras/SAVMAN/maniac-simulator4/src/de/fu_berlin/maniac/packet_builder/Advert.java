package de.fu_berlin.maniac.packet_builder;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Adverts are sent ( by the AuctionManager ) to start the bidding process for a
 * Data packet that has to be forwarded.
 * 
 * @author maniacchallenge
 * 
 */

public class Advert extends Packet {
	private final int ADVERT_LENGTH = 21;
	private int ceil;
	private int deadline;                  // hops left
	private int fine;
	private Inet4Address finalDestination; // BBN router where the advertised packet has to end up at
	private int initialBudget;         // initial budget for the transmission
	  									   // (also the fine for retransmission via BBN)
	
	protected Advert(Inet4Address finalDest, int transactionID, int maxBid,
			int deadline, int fine, int initalBudget) {
		this.type = 'A';
		this.finalDestination = finalDest;
		this.transactionID = transactionID;
		this.ceil = maxBid;
		this.deadline = deadline;
		this.transactionID = transactionID;
		this.fine = fine;
		this.initialBudget = initialBudget;
	}

	/**
	 * @return the IP of the Backbone the Data should arrive at
	 */
	public Inet4Address getFinalDestination() {
		return finalDestination;
	}

	protected Advert(byte[] rawdata) {
		parseFromByteArray(rawdata);
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

	private void parseFromByteArray(byte[] payload) {

		this.type = (char) payload[0];
		this.transactionID = loadFromByteArray(payload, 1); // Transaction ID

		// Destination IP
		byte[] address = new byte[4];
		for (int i = 0; i < 4; i++)
			address[i] = payload[5 + i];

		try {
			this.finalDestination = (Inet4Address) InetAddress
					.getByAddress(address);
		} catch (UnknownHostException e) {
			this.finalDestination = null;
			e.printStackTrace();
		}

		this.ceil = loadFromByteArray(payload, 9); // Maximum costs
		this.deadline = loadFromByteArray(payload, 13); // hops left
		this.fine = loadFromByteArray(payload, 17); // Fine
	}

	private byte[] parseToByteArray() {

		byte[] payload = new byte[ADVERT_LENGTH];

		payload[0] = (byte) this.type; // Packet type
		saveToByteArray(this.transactionID, payload, 1); // Insert transaction
															// ID

		// Destination IP TODO change to finalDestination
		if (destinationIP == null) {
			System.err.println("error: no destination address!");
		} else {
			byte[] address = destinationIP.getAddress();
			for (int i = 0; i < 4; i++)
				payload[5 + i] = address[i];
		}

		saveToByteArray(this.ceil, payload, 9); // Maximum costs
		saveToByteArray(this.deadline, payload, 13); // hops left
		saveToByteArray(fine, payload, 17); // fine

		return payload;
	}

	@Override
	protected DatagramPacket getDatagramPacket() {
		
//		AdvertMessage a = AdvertMessage.newBuilder()
//				.setDestinationIP(this.destinationIP.getHostAddress())
//				.setCeil(this.ceil)
//				.setDeadline(this.deadline)
//				.setFine(this.fine)
//				.build();
//		
//		PacketMessage packetMessage = PacketMessage.newBuilder()
//				
//				.setTransactionID(this.transactionID)
//				.setAdvertMessage(a)
//				.build();
//		
//		
//		System.out.println("new Packetmessage/AdvertMessage: \n"+packetMessage );
		
		byte[] payload = parseToByteArray();
		return new DatagramPacket(payload, ADVERT_LENGTH, this.destinationIP,
				PACKET_PORT);
	}

	@Override
	public String toString() {
		return "ADVERT \n transactionID: " + this.transactionID
				+ " \n SourceIP: " + this.sourceIP + " \n destinationIP: "
				+ this.destinationIP + " \n ceil: " + this.ceil
				+ " \n deadline: " + this.deadline + " \n fine: " + this.fine;
	}
}