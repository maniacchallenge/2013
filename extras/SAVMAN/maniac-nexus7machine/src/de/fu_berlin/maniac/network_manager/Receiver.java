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
			//System.out.println("Received packet.");
			if (packet.getAddress().equals(
					TopologyInfo.getInterfaceIpv4()))
				continue;
			//System.out.println("Packet was for me: " + TopologyInfo.getInterfaceIpv4());
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
