package de.fu_berlin.maniac.packet_builder;

import java.net.DatagramPacket;
import java.net.Inet4Address;

import de.fu_berlin.maniac.exception.NegativeBidException;

public interface PacketBuilderInterface {
	/**
	 * Builds a Packet from raw data
	 * 
	 * @param rawdata
	 *            Packet data, received as a byte[]
	 * @param sourceIP
	 *            IP from which the data originated
	 * @return Packet (Advert, Bid, BidWin or Data) built from given data
	 */
	public Packet buildPacket(byte[] rawdata, Inet4Address sourceIP);

	/**
	 * Builds a Packet from a Datagram
	 * 
	 * @param data
	 *            the received Datagram
	 * @return Packet (Advert, Bid, BidWin or Data) built from given data
	 */
	public Packet buildPacket(DatagramPacket data);

	/**
	 * Builds an Advert from scratch
	 * 
	 * @param data
	 *            the Data which the Advert is advertising
	 * @param maxBid
	 *            maximum bid accepted in the auction
	 * @param fine
	 *            fine in case of transaction failure
	 * @return Advert built from given data
	 */
	public Packet buildAdvert(Data data, int maxBid, int fine);

	/**
	 * Builds a Bid from scratch
	 * 
	 * @param advert
	 *            the Advert to which the Bid repsonds
	 * @param bid
	 *            value of the bid
	 * @return Bid built from given data
	 * @throws NegativeBidException
	 */
	public Packet buildBid(Advert advert, int bid) throws NegativeBidException;

	/**
	 * Builds a dummy Bid in case no Bid was received
	 * 
	 * @param transactionID
	 * @param bid
	 * @return dummy Bid
	 * @throws NegativeBidException
	 */
	public Packet buildDummyBid(int transactionID, int bid)
			throws NegativeBidException;

	/**
	 * Builds a BidWin from scratch
	 * 
	 * @param bid
	 *            value of the winning bid
	 * @param fine
	 *            fine in case of transaction failure
	 * @return BidWin built from given Data
	 */
	public Packet buildBidWin(Bid bid, int fine);

	/**
	 * Builds a Check from a String (since all Data sent over TCP is sent as
	 * Strings this method uses Strings rather than byte[]).<br>
	 * The String's format is as follows:<br>
	 * Starting with the Packet type, followed by the information divided by
	 * spaces. A $ marks the end of a chunk of data. example:<br>
	 * "C transactionID amount balance $"
	 * 
	 * @param rawData
	 *            Packet data, received as a String
	 * @return Check built from given Data
	 */
	public Packet buildCheck(String rawData);

	// data doesn't need a buildpacket, because data packets only get updated by
	// nodes

	/**
	 * refresh TTL and the like of an old packet
	 * 
	 * @param packet
	 *            Data Packet that needs to be updated
	 * @param destinationIP
	 *            destination of the Data packet
	 * @return updated Data Packet
	 */
	public Packet updateData(Data packet, Inet4Address destinationIP);

	/**
	 * turn Packet into data that can be sent over TCP
	 * 
	 * @param packet
	 *            Packet to be sent
	 * @return String starting with the Packet type, followed by the information
	 *         divided by spaces. A $ marks the end of a chunk of data. example:<br>
	 *         "W transactionID winnerIP winningBid fine $"
	 */
	public String getStreamableData(Packet packet);

	/**
	 * turn Packet into data that can be sent over UDP
	 * 
	 * @param packet
	 *            Packet to be sent
	 * @return DatagramPacket containing all of the Packet's info squeezed into
	 *         a byte[]
	 */
	public DatagramPacket getDatagramPacket(Packet packet);
}
