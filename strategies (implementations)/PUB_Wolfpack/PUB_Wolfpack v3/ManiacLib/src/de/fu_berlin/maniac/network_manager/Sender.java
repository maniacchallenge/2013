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

package de.fu_berlin.maniac.network_manager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

//import de.fu_berlin.maniac.general.*;
import de.fu_berlin.maniac.packet_builder.*;

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
	 * Call yourSender.send(packet) fire your Advertise, Bid, Data or BidWin into
	 * the network.
	 * @param packet
	 */
	public void send(Packet packet) {
		if (packet != null) {
			try {
				DatagramPacket dgram = packetbuilder.getDatagramPacket(packet);
				datagramSocket.send(dgram);
				// if it is a BidWin, send a copy to your Backbone
				if (packet.getType() == 'W'){
					System.out.println("sending TCP win...");
					byte[] packetStream = packetbuilder.getStreamableData(packet);
					networkManager.sendPacketStream(packetStream);
					
					/*
					String packetStream = packetbuilder.getStreamableData(packet);
					networkManager.sendPacketStream(packetStream);
					*/
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
