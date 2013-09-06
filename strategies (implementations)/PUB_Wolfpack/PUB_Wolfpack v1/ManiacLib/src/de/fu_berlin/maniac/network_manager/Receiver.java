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
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import de.fu_berlin.maniac.packet_builder.*;

/**
 * Receives all incoming UDP packets and adds them in a shared datastructure for
 * Mothership to process
 * 
 * @author maniacchallenge
 * 
 */
public class Receiver extends Thread {

	private static final int PAYLOAD_LENGTH = 128;
	private static final int SOCKET_TIMEOUT = 1000;
	private static final int PORT = 8765;

	private AtomicBoolean exit;
	DatagramSocket receiver;
	LinkedBlockingQueue<Packet> packetqueue;
	PacketBuilder packetbuilder;

	/**
	 * Just the constructor...
	 * @throws SocketException
	 */
	public Receiver() throws SocketException {
		exit = new AtomicBoolean(false);
		receiver = new DatagramSocket(PORT);
		packetqueue = new LinkedBlockingQueue<Packet>();
		packetbuilder = PacketBuilder.getInstance();
	}

	/**
	 * Tries to receive packets for ev0r until exit() is called.
	 */
	public void run() {
		DatagramPacket packet;
		byte[] payload = new byte[PAYLOAD_LENGTH];

		try {
			receiver.setSoTimeout(SOCKET_TIMEOUT);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}

		while (!exit.get()) {
			packet = new DatagramPacket(payload, payload.length);

			try {
				receiver.receive(packet);
			} catch (SocketTimeoutException t) {
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			if (packet.getAddress().equals(
					TopologyInfo.getInterfaceIpv4("wlan0")))
				continue;

			packetqueue.add(packetbuilder.buildPacket(packet));
		}

	}

	/**
	 * This function is used to stop receiving packets. It sets "exit" to true,
	 * so the while-loop in run() will stop.
	 */
	public void exit() {
		exit.set(true);
	}

	public LinkedBlockingQueue<Packet> getPacketQueue() {
		return packetqueue;
	}
}
