package de.fu_berlin.maniac.network_manager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import de.fu_berlin.maniac.packet_builder.*;
//import de.tuhh.maniac.simulator.NexusSeven;
import java.net.Inet4Address;
import java.util.Date;
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
	
	private Inet4Address IP;
	private TopologyInfo tp;
	private String intf;

	/**
	 * Just the constructor...
	 * @throws SocketException
	 */
	public Receiver(Inet4Address IP, PacketBuilder packetBuilder, String intf) throws SocketException {
		this.IP = IP;
		this.tp = tp;
		this.intf = intf;
		exit = new AtomicBoolean(false);
		System.out.println("[" + new Date() + "]" +"Receiver is bound to IP: " + packetBuilder.getDeviceIP() + " and Port: " + PORT);
		receiver = new DatagramSocket(PORT);
		packetqueue = new LinkedBlockingQueue<Packet>();
		packetbuilder = PacketBuilder.getInstance(intf);
		
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
					tp.getInterfaceIpv4(this.intf)))
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
