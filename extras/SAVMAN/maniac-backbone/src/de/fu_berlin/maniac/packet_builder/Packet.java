package de.fu_berlin.maniac.packet_builder;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.nio.ByteBuffer;

/**
 * The superclass for all Packet types.
 * 
 * @author maniacchallenge
 * 
 */

public abstract class Packet {
	protected char type;
	protected int transactionID;

	// immediate source of the incoming packets
	protected Inet4Address sourceIP;
	protected Inet4Address destinationIP;
	protected Inet4Address broadcastAddr;
	protected final int PACKET_PORT = 8765;

	/**
	 * @return which kind of packet the object is. A : Advert, B : Bid, W :
	 *         BidWinm D : Data or C : Check (i.e. Bank Data)
	 */
	public char getType() {
		return type;
	}

	/**
	 * @return the transactionID of the transaction the packet belongs to.
	 */
	public int getTransactionID() {
		return transactionID;
	}

	// this ID is set by the PacketBuilder
	protected void setTransactionID(int id) {
		this.transactionID = id;
	}

	/**
	 * @return the last hop the packet has traveled (is this correct?) TODO
	 */
	public Inet4Address getSourceIP() {
		return sourceIP;
	}

	// this is set by the PacketBuilder
	protected void setSourceIP(Inet4Address sourceIP) {
		this.sourceIP = sourceIP;
	}

	/**
	 * @return the next hop of the packet (is this correct?) TODO
	 */
	public Inet4Address getDestinationIP() {
		return destinationIP;
	}

	// this is set by the PacketBuilder
	protected void setDestinationIP(Inet4Address destinationIP) {
		this.destinationIP = destinationIP;
	}

	protected void setBroadcastAddr(Inet4Address broadcastAddr) {
		this.broadcastAddr = broadcastAddr;
	}

	// wrap Packet's data into a DatagramPacket
	protected DatagramPacket getDatagramPacket() {
		return null;
	}

	/*
	 * return Packet's Data so that it can be sent through an OutputStream (for
	 * TCP conn.) The Strings start with the type ID, then the information
	 * divided by spaces. a $ marks the end of a chunk of data. example:
	 * "W transactionID winnerIP winningBid fine $"
	 */

	protected String getStreamableData() {
		return null;
	}

	@Override
	public abstract String toString();

	// helper functions
	protected static byte[] intToByteArray(int i) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(i);
		return buffer.array();
	}

	protected static int byteArrayToInt(byte[] b) {
		return ByteBuffer.wrap(b).getInt();
	}

	protected static void saveToByteArray(int value, byte[] array, int start) {
		byte[] buffer = intToByteArray(value);
		for (int i = 0; i < 4; i++)
			array[start + i] = buffer[i];
	}

	protected static int loadFromByteArray(byte[] array, int start) {
		byte[] buffer = new byte[4];
		for (int i = 0; i < 4; i++)
			buffer[i] = array[start + i];
		return byteArrayToInt(buffer);
	}

}
