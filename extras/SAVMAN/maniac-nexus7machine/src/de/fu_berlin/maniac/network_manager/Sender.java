package de.fu_berlin.maniac.network_manager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

//import de.fu_berlin.maniac.general.*;
import de.fu_berlin.maniac.packet_builder.*;
import java.util.Date;

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

          public Sender() throws SocketException {
	         datagramSocket = new DatagramSocket();
	         packetbuilder = PacketBuilder.getInstance();
	         networkManager = NetworkManager.getInstance();
          }

          /**
           * Call yourSender.send(packet) fire your Advertise, Bid, Data or BidWin into the network.
           *
           * @param packet
           */
          public void send(Packet packet) {
	         // TODO: if BidWin, dann auch per TCP raus
	         if (packet != null) {
		        try {
			       switch (packet.getType()) {
				      case 'A':
					     System.out.println("[" + new Date() + "]" + "[" + TopologyInfo.getOwnIP() + "] Sender: Sending Advert");
					     System.out.println(((Advert) packet).toString());
					     break;
				      case 'B':
					     System.out.println("[" + new Date() + "]" + "[" + TopologyInfo.getOwnIP()  + "] Sender: Sending Bid");
					     System.out.println(((Bid) packet).toString());
					     break;
				      case 'W':
					     System.out.println("[" + new Date() + "]" + "[" + TopologyInfo.getOwnIP()  + "] Sender: Sending BidWin");
					     System.out.println(((BidWin) packet).toString());
					     break;
				      case 'D':
					     System.out.println("[" + new Date() + "]" + "[" + TopologyInfo.getOwnIP()  + "] Sender: Sending Data");
					     System.out.println(((Data) packet).toString());
					     break;

				      default:
					     System.err.println("[" + new Date() + "]" + "[" + TopologyInfo.getOwnIP()  + "] Sender: Sending What ???");
			       }
			       DatagramPacket dgram = packetbuilder.getDatagramPacket(packet);
			       datagramSocket.send(dgram);
			       // if it is a BidWin, send a copy to your Backbone
			       if (packet.getType() == 'W') {
				      String packetStream = packetbuilder.getStreamableData(packet);
				      networkManager.sendPacketStream(packetStream);
			       }
		        } catch (IOException e) {
			       e.printStackTrace();
		        }
	         }
          }
}
