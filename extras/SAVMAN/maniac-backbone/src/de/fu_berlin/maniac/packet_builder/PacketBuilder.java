package de.fu_berlin.maniac.packet_builder;

import java.net.DatagramPacket;
import java.net.Inet4Address;

import de.fu_berlin.maniac.exception.NegativeBidException;
import de.fu_berlin.maniac.network_manager.NetworkManager;
import de.fu_berlin.maniac.network_manager.TopologyInfo;

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
	// since this is a singleton, we ned _one_ instance of itself
	private static PacketBuilder instance = null;

	private Inet4Address deviceIP;
	private Inet4Address broadcastAddr;
	
	private TopologyInfo tp;
	
	private String intf;

	/*private PacketBuilder() {
		deviceIP = (Inet4Address) TopologyInfo.getInterfaceIpv4("wlan0");
		broadcastAddr = (Inet4Address) TopologyInfo
				.getBroadCastAddress(deviceIP);
	}*/
	
	private PacketBuilder(String intf) {
		this.intf = intf;
		deviceIP = (Inet4Address) TopologyInfo.getInterfaceIpv4(intf);
		System.out.println("Requesting IP for interface: (Set interface in Backbone.java): " + intf );
		System.out.println("Interface IP: " + deviceIP );
		broadcastAddr = (Inet4Address) TopologyInfo
				.getBroadCastAddress(deviceIP);
		//broadcastAddr = (Inet4Address) tp.getBroadCastAddress(deviceIP);
	}
	
	public static PacketBuilder getInstance(String intf) {
		if (instance == null) {
			instance = new PacketBuilder(intf);
		}		
		//System.err.println("NEW PACKETBUILDER => " + deviceIP);
		//instance = new PacketBuilder(intf);
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
	 */
	@Override
	public Packet buildPacket(byte[] rawdata, Inet4Address sourceIP) {
		char type = (char) rawdata[0];
		Packet packet;
		switch (type) {
		case 'A':
			packet = new Advert(rawdata);
			packet.setSourceIP(sourceIP);
			break;
		case 'B':
			packet = new Bid(rawdata);
			packet.setSourceIP(sourceIP);
			break;
		case 'W':
			packet = new BidWin(rawdata);
			packet.setSourceIP(sourceIP);
			break;
		case 'D':
			packet = new Data(rawdata);
			break;
		// case 'C':
		// packet = new Check(rawdata);
		// break;
		default:
			packet = null;
		}

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
		return buildPacket(data.getData(), (Inet4Address) data.getAddress());
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

		Advert advert = new Advert(data.getFinalDestination(), transactionID,
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
		Inet4Address backboneIP= (Inet4Address) NetworkManager.getInstance(this.deviceIP, this.intf).getMyOwnBackbone();
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
	@Override
	public Packet buildCheck(String rawData) {
		return new Check(rawData);
	}

	// build Data. This only exists for testing purposes.
	public Data buildData(int transactionID, Inet4Address destinationIP, int hopCount, 
			int fine, byte[] data, Inet4Address finalDestination, int initialBudget) {
		Data d = new Data(transactionID, hopCount, fine, data, finalDestination, initialBudget);
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
	public String getStreamableData(Packet packet) {
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
		return packet.getDatagramPacket();
	}

	/**
	* @return the deviceIP
	*/
	public Inet4Address getDeviceIP() {
		return deviceIP;
	}
}
