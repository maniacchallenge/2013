package de.fu_berlin.maniac.network_manager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

//import de.fu_berlin.maniac.general.*;
import de.fu_berlin.maniac.packet_builder.*;
import java.net.Inet4Address;

/**
 * An instance of this class is used to send UDP-Packets
 * 
 * @author maniacchallenge
 *
 */
public class Sender {

	private DatagramSocket datagramSocket;
	private PacketBuilder packetbuilder;
	private NetworkManager networkManager;
	private Inet4Address IP;
	private static final int PORT = 8766;

	public Sender(Inet4Address IP, PacketBuilder packetbuilder) throws SocketException {
		this.IP = IP;
		datagramSocket = new DatagramSocket(PORT, IP);
		this.packetbuilder = packetbuilder;
		networkManager = NetworkManager.getInstance();
	}

	/**
	 * Call yourSender.send(packet) fire your Advertise, Bid, Data or BidWin into
	 * the network.
	 * @param packet
	 */
	public void send(Packet packet) {
		
		// TODO: if BidWin, dann auch per TCP raus
		if (packet != null) {
			try {
				DatagramPacket dgram = packetbuilder.getDatagramPacket(packet);
				System.out.println("[" + this.IP + "} Sender: Send Packet Type: " + packet.getType());
				datagramSocket.send(dgram);
				// if it is a BidWin, send a copy to your Backbone
				if (packet.getType() == 'W'){
					String packetStream = packetbuilder.getStreamableData(packet);
					networkManager.sendPacketStream(packetStream);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
