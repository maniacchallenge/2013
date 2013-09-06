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

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;

import de.fu_berlin.maniac.exception.NegativeBidException;
import de.fu_berlin.maniac.network_manager.NetworkManager;
import de.fu_berlin.maniac.network_manager.TopologyInfo;
import de.fu_berlin.maniac.packet_builder.ProtoPackets.PacketMessage;

/**
 * Singleton. <br>
 * The PacketBuilder builds Packets (duh) either - from incoming raw data - from
 * the packet proceeding the packet being built (i.e. to build an Advert you
 * must've first received Data )
 * 
 * It is also being used to generate sendable data (UDP /TCP) from Packet
 * Objects.
 * 
 * @author maniacchallenge
 * 
 */

public class PacketBuilder implements PacketBuilderInterface {
	// since this is a singleton, we need _one_ instance of itself
	private static PacketBuilder instance = null;

	private Inet4Address deviceIP;
	private Inet4Address broadcastAddr;

	private PacketBuilder() {
		deviceIP = (Inet4Address) TopologyInfo.getInterfaceIpv4("wlan0");
		broadcastAddr = (Inet4Address) TopologyInfo
				.getBroadCastAddress(deviceIP);
	}

	public static PacketBuilder getInstance() {
		if (instance == null) {
			instance = new PacketBuilder();
		}
		return instance;
	}

	/**
	 * build packet from raw data
	 * 
	 * @param rawdata
	 *            Packet data, received as a byte[]
	 * @param sourceIP
	 *            IP from which the data originated
	 * @return Packet (Advert, Bid, BidWin or Data) built from given data
	 * @throws InvalidProtocolBufferException 
	 */
	@Override
	public Packet buildPacket(byte[] rawdata, Inet4Address sourceIP) throws InvalidProtocolBufferException {
		PacketMessage packetMessage = PacketMessage.parseFrom(rawdata);
		Packet packet = parseAccordingToType(packetMessage, sourceIP);
		return packet;
	}
	
	/**
	 * build Packet from Datagram
	 * 
	 * @param data
	 *            the received Datagram
	 * @return Packet (Advert, Bid, BidWin or Data) built from given data
	 */
	@Override
	public Packet buildPacket(DatagramPacket data) {
		try {
			DatagramPacket p = data;
			PacketMessage packetMessage = PacketMessage.parseFrom(CodedInputStream.newInstance(p.getData(), p.getOffset(), p.getLength()));
			
			Packet packet = parseAccordingToType(packetMessage, (Inet4Address) data.getAddress());
			return packet;
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		return null;
	}
	
	private Packet parseAccordingToType(PacketMessage packetMessage, Inet4Address sourceIP){
		int type = packetMessage.getType().getNumber();
		Packet packet = null;
		try{
			switch (type) {
			case 0:
				packet = new Advert(packetMessage);
				packet.setSourceIP(sourceIP);
				break;
			case 1:
				packet = new Bid(packetMessage);
				packet.setSourceIP(sourceIP);
				break;
			case 2:
				packet = new BidWin(packetMessage);
				packet.setSourceIP(sourceIP);
				break;
			case 3:
				packet = new Check(packetMessage);
				break;
			case 4:
				packet = new Data(packetMessage);
				break;
			default:
				packet = null;
			}
		}catch(UnknownHostException uhe){
			// TODO handle me properly!
			uhe.printStackTrace();
		}

		System.out.println("xoxoxo"+packet);
		return packet;
	}

	/**
	 * build Advert from scratch
	 * 
	 * @param data
	 *            the Data which the Advert is advertising
	 * @param maxBid
	 *            maximum bid accepted in the auction
	 * @param fine
	 *            fine in case of transaction failure
	 * @return Advert built from given data
	 */
	@Override
	public Packet buildAdvert(Data data, int maxBid, int fine) {
		assert data != null : "Adverts can only be made for valid data packets!";

		int transactionID = data.getTransactionID();
		int deadline = data.getHopCount();
		int initialBudget = data.getInitialBudget();

		Advert advert = new Advert(data.getFinalDestinationIP(), transactionID,
				maxBid, deadline, fine, initialBudget);
		advert.setSourceIP(this.deviceIP);
		advert.setDestinationIP(this.broadcastAddr);
		return advert;
	}

	/**
	 * build Bid from scratch
	 * 
	 * @param advert
	 *            the Advert to which the Bid repsonds
	 * @param bid
	 *            value of the bid
	 * @return Bid built from given data
	 * @throws NegativeBidException
	 */
	@Override
	public Packet buildBid(Advert advert, int bid) throws NegativeBidException {
		assert advert != null : "Bids can only be made for valid Advert packets!";

		int transactionID = advert.transactionID;

		Bid bidPkt = new Bid(transactionID, bid);
		bidPkt.setSourceIP(this.deviceIP);
		bidPkt.setDestinationIP(this.broadcastAddr);
		return bidPkt;
	}

	/**
	 * Builds a dummy Bid in case no Bid was received
	 * 
	 * @param TransactionID
	 * @param bid
	 * @return dummy Bid
	 * @throws NegativeBidException
	 */
	@Override
	public Packet buildDummyBid(int transactionID, int bid)
			throws NegativeBidException {
		Bid bidPkt = new Bid(transactionID, bid);
		Inet4Address backboneIP= (Inet4Address) NetworkManager.getInstance().getMyOwnBackbone();
		if(backboneIP == null){
			return null;
		}
		bidPkt.setSourceIP(backboneIP);
		bidPkt.setDestinationIP(this.broadcastAddr);
		return bidPkt;
	}

	/**
	 * build BidWin from scratch
	 * 
	 * @param bid
	 *            value of the winning bid
	 * @param fine
	 *            fine in case of transaction failure
	 * @return
	 */
	@Override
	public Packet buildBidWin(Bid bid, int fine) {

		int transactionID = bid.getTransactionID();
		Inet4Address winnerIP = bid.getSourceIP();
		int winningBid = bid.getBid();
		BidWin bidWin = new BidWin(transactionID, winnerIP, winningBid, fine);
		bidWin.setSourceIP(this.deviceIP);
		bidWin.setDestinationIP(this.broadcastAddr);
		return bidWin;
	}

	/**
	 * Builds a Check from a protobuf message
	 * 
	 * @param rawData
	 *            Packet data, received in a protobuf message
	 * @return Check built from given Data
	 */
	@Override
	public Packet buildCheck(PacketMessage packetMessage) {
		if (packetMessage == null){
			return null;
		}
		return new Check(packetMessage);
	}
	
	/**
	 * Builds a general purpose message Format that users can use for communication 
	 * between their nodes.
	 * @param message 
	 * 			Whatever you want to send
	 * @return GeneralPurposePacket containing your message
	 */
	@Override
	public Packet buildGeneralPurposePacket(String message) {
		return new GeneralPurposePacket(message);
	}

	// build Data. This only exists for testing purposes.
	public Data buildData(int transactionID, Inet4Address destinationIP, int deadline, 
			int fine, String payload, Inet4Address finalDestination, int initialBudget) {
		Data d = new Data(transactionID, deadline, fine, payload, finalDestination, initialBudget);
		d.setSourceIP(this.deviceIP);
		d.setDestinationIP(destinationIP);
		return d;
	}

	/**
	 * refresh TTL and the like of an old packet
	 * 
	 * @param packet
	 *            Data Packet that needs to be updated
	 * @param destinationIP
	 *            destination of the Data packet
	 * @return updated Data Packet
	 */
	@Override
	public Packet updateData(Data packet, Inet4Address destinationIP) {
		packet.setDestinationIP(destinationIP);
		return packet;
	}
	
	/**
	 * Updates fine for data packet before sending out
	 * @param packet Data packet that needs to be updated
	 * @param fine New fine
	 * @return
	 */
	public Packet updateData(Data packet,int fine){
		packet.setFine(fine);
		return packet;
		
	}

	/**
	 * turn Packet into data that can be sent over TCP
	 * 
	 * @param packet
	 *            Packet to be sent
	 * @return String starting with the Packet type, followed by the information
	 *         divided by spaces. A $ marks the end of a chunk of data. example:<br>
	 *         "W transactionID winnerIP winningBid fine $"
	 */
	@Override
	public byte[] getStreamableData(Packet packet) {
		return packet.getStreamableData();
	}
	

	/**
	 * turn Packet into data that can be sent over UDP
	 * 
	 * @param packet
	 *            Packet to be sent
	 * @return DatagramPacket containing all of the Packet's info squeezed into
	 *         a byte[]
	 */
	@Override
	public synchronized DatagramPacket getDatagramPacket(Packet packet) {
		try {
			return packet.getDatagramPacket();
		} catch (IOException e) {
			// TODO Handle properly!
			e.printStackTrace();
		}
		return null;
	}
}
