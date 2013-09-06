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

import com.google.protobuf.InvalidProtocolBufferException;

import de.fu_berlin.maniac.exception.NegativeBidException;
import de.fu_berlin.maniac.packet_builder.ProtoPackets.PacketMessage;

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
	public Packet buildPacket(byte[] rawdata, Inet4Address sourceIP) throws InvalidProtocolBufferException;

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
	 * Builds a Check from a protobuf message
	 * 
	 * @param rawData
	 *            Packet data, received in a protobuf message
	 * @return Check built from given Data
	 */
	public Packet buildCheck(PacketMessage rawData);

	// data doesn't need a buildpacket, because data packets only get updated by
	// nodes

	/**
	 * Builds a general purpose message Format that users can use for communication 
	 * between their nodes.
	 * @param message 
	 * 			Whatever you want to send
	 * @return GeneralPurposePacket containing your message
	 */
	public Packet buildGeneralPurposePacket(String message);
	
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
	public byte[] getStreamableData(Packet packet);

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
