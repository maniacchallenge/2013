package de.fu_berlin.maniac.packet_builder;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The node that has started an auction by sending an Advert will announce the
 * winner by broadcasting a BidWin packet.
 * 
 * @author maniacchallenge
 * 
 */

public class BidWin extends Packet {
	private final int WIN_LENGTH = 17;
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

	protected BidWin(byte[] rawdata) {
		parseFromByteArray(rawdata);
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

	private void parseFromByteArray(byte[] payload) {
		this.transactionID = loadFromByteArray(payload, 1); // Transaction ID

		// Winner IP
		byte[] address = new byte[4];
		for (int i = 0; i < 4; i++)
			address[i] = payload[5 + i];

		try {
			this.winnerIP = (Inet4Address) InetAddress.getByAddress(address);
		} catch (UnknownHostException e) {
			this.winnerIP = null; // what happens when this happens? TODO
			e.printStackTrace();
		}

		// load winning bid
		this.winningBid = loadFromByteArray(payload, 9);
		this.fine = loadFromByteArray(payload, 13);
		this.type = (char) payload[0];

	}

	private byte[] parseToByteArray() {
		byte[] payload = new byte[WIN_LENGTH];

		// Packet type
		payload[0] = (byte) this.type;

		// Insert transaction ID
		saveToByteArray(this.transactionID, payload, 1);

		// Insert bid
		if (this.winnerIP == null) {
			System.err.println("error: winner not defined!"); // TODO properly
		} else {
			byte[] address = this.winnerIP.getAddress();
			for (int i = 0; i < 4; i++)
				payload[5 + i] = address[i];
		}

		// insert winning bid
		saveToByteArray(this.winningBid, payload, 9);
		saveToByteArray(this.fine, payload, 13);

		return payload;
	}

	@Override
	protected DatagramPacket getDatagramPacket() {
		byte[] payload = parseToByteArray();
		return new DatagramPacket(payload, WIN_LENGTH, this.destinationIP,
				PACKET_PORT);
	}

	@Override
	protected String getStreamableData() {
		String s = "W " + this.transactionID + " " + this.winnerIP + " "
				+ this.winningBid + " " + this.fine + " $";
		return s;
	}

	@Override
	public String toString() {
		return "BIDWN \n transactionID: " + this.transactionID + "\n SourceIP:"
				+ this.sourceIP + "" + "\n destinationIP:" + this.destinationIP
				+ "\n WinnerIP: " + this.winnerIP;
	}
}
