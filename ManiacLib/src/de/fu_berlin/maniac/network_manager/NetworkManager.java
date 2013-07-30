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

import android.os.Environment;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.InvalidProtocolBufferException;

import de.fu_berlin.maniac.packet_builder.*;
import de.fu_berlin.maniac.packet_builder.ProtoPackets.PacketMessage;
import de.fu_berlin.maniac.bank.*;

/**
 * The NetworkManager is declared and started with the start of the app
 * (Mothership) and runs concurrently. The NetworkManager inherits methods for
 * detection, establishes and managing the connection with the backbone.
 * 
 * @author maniacchallenge
 * 
 */
public class NetworkManager extends Thread {

	private static NetworkManager networkManager;
	
	private PacketBuilder packetbuilder;
	private ArrayList<InetAddress> backbones; // list of all Backbone IPs
	
	BankManager bankman;
	static LinkedBlockingQueue<byte[]> packetqueue;
	
	// Variables for own Backbone
	private static volatile InetAddress myOwnBackbone;
	private static AtomicBoolean isConnectedToBbn;
	private AtomicInteger topChecker;
	private ArrayList<Link> links;
	
	private TCPHandler tcpHandler;
	private TCPSender tcpSender;

	private NetworkManager() {
		backbones = new ArrayList<InetAddress>();
		myOwnBackbone = null;
		isConnectedToBbn = new AtomicBoolean();
		isConnectedToBbn.getAndSet(false);
		topChecker = new AtomicInteger(0);
		packetqueue = new LinkedBlockingQueue<byte[]>();
		bankman = new BankManager();
		packetbuilder = PacketBuilder.getInstance();
	}
	
	public static NetworkManager getInstance(){
		if (networkManager == null){
			networkManager = new NetworkManager();
		}
		return networkManager;
	}
	
	public static void setZero() {
		isConnectedToBbn.getAndSet(false);
	}
	
	public void run() {
		//First read the backbonefile -> generic.txt
		try {
			readBackboneFile();
		} catch (IOException e1) {
			System.err.println("No backbonelist found.");
			e1.printStackTrace();
		}
        
		//Starts the Thread which searches each second for a new backbone
		BackBoneHandler backboneHandler = new BackBoneHandler();
		backboneHandler.start();

		//Starts the Thread which manages the connection with the backbone.
		tcpHandler = new TCPHandler();
		tcpHandler.start();	
	}
	
	class BackBoneHandler extends Thread {
				
		public BackBoneHandler() {}
		
		public void run() {
			for(;;) {
				if(!isConnectedToBbn.get() || myOwnBackbone==null) {
					try {
						findBestBackbone();
						if(topChecker.get()<3) {
							topChecker.getAndIncrement();
						} else {
							topChecker.set(0);
						}
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					System.out.println("No my backbone found.");
					e.printStackTrace();
				}	
			}
		}
	}
	
	
	/**
	 * Establishes the TCP-Connection with the server and manages connection on
	 * timeout, reads line and searches for new backbone in case of timeout.
	 */
	
	class TCPHandler extends Thread{
		DataOutputStream dataOutputStream; 
		BufferedReader bufferedReader;
		
		private Socket clientSocket;
		// Variable for BankManager to update actual bank status

		/**
		 * Reads one line from incoming TCP-Stream and interprets it depending on
		 * different scenarios. Acts like a filter.
		 * 
		 * @param socket
		 *            TCP-Socket from backbone-connection
		 * @return String, depending on input interpretation
		 * @throws IOException
		 *             Error on connection with backbone.
		 */
		public void run() {
			for(;;) {
				if(isConnectedToBbn.get() || myOwnBackbone==null) {
					try {
						if(myOwnBackbone !=null) {
							readPackets(clientSocket.getInputStream());							
						}
						System.out.println("I´m connected to my backbone: "+myOwnBackbone);
						Thread.sleep(1000);
					} catch (Exception e) {
						isConnectedToBbn.getAndSet(false);
						System.out.println("my backbone has an exception");
						e.printStackTrace();
					}
				} else {
					try {
						clientSocket = new Socket(myOwnBackbone, 51113);
						isConnectedToBbn.getAndSet(true);
						// if you haven't received a Check in 1 sec (+1 sec error tolerance),
						// your connection is lost.
						clientSocket.setSoTimeout(5000);
						//clientSocket.setKeepAlive(true);
						tcpSender = new TCPSender(clientSocket);
						tcpSender.start();
						readPackets(clientSocket.getInputStream());

					} catch (SocketTimeoutException e2) {
						System.out.println("my backbone is timeout lost");
						isConnectedToBbn.getAndSet(false);
					} catch (IOException e2) {
						isConnectedToBbn.getAndSet(false);
						try {
							if(clientSocket !=null) {
								System.out.println("my backbone is finally lost");
								clientSocket.close();								
							} else {
								System.out.println("my backbone is finally io lost");
							}
							
						} catch (IOException e) {
							
							e.printStackTrace();
						}
						e2.printStackTrace();
					}
					
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
						
						
				}
			}
		}
		
		/**
		 * reads incoming Checks from the TCP socket, parses them and sends them to the BankManager
		 */
		private void readPackets(InputStream in){
			try{
						PacketMessage packetMessage = PacketMessage.parseDelimitedFrom(in);
						Check check = (Check) packetbuilder.buildCheck(packetMessage);
						if (check != null){
							bankman.update(check);
						}
						System.out.println("my backbone has read");
			} catch(IOException ioe){
				System.out.println("my backbone is read io lost");
				isConnectedToBbn.getAndSet(false);
				ioe.printStackTrace();
			}
			
		}
	}	
	
	/**
	 * 
	 * Sends data buffered in the packetqueue to the backbone.
	 *
	 */
	
	// TODO test me: arbeite ich die queue korrekt ab?
	
	class TCPSender extends Thread{
		Socket clientSocket;
		DataOutputStream dataOutputStream;

		public TCPSender(Socket clientSocket) throws IOException{
			this.clientSocket = clientSocket;
			this.dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
		}
		
		public void run(){
			byte[] pkt;
			try {
				while((pkt = packetqueue.take()) != null){
					dataOutputStream.write(pkt);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch(IOException ioe){
				// TODO Auto-generated catch block
				ioe.printStackTrace();
			}
		}

	}

	
	/**
	 * This method searches for the "generic.txt" file on the SD-Card of the device
	 * and reads lines, each containing a InetAddress of one possible backbone.
	 * 
	 * @return String with Backbones derived from
	 *         ArrayList<InetAddress>.toString()
	 * @throws IOException
	 *             If file is not present on device
	 */
	private String readBackboneFile() throws IOException {

		String tmp = "";
		backbones.clear();

		File sdcard = Environment.getExternalStorageDirectory();
		File file = new File(sdcard, "generic.txt");
		FileInputStream fis = new FileInputStream(file);

		BufferedReader myReader = new BufferedReader(new InputStreamReader(fis));

		while ((tmp = myReader.readLine()) != null) {
			backbones.add(InetAddress.getByName(tmp));
		}

		myReader.close();
		return getBackbones().toString();

	}

	/**
	 * Method returns the ArrayList of Backbones, each index containing one
	 * InetAddress of one possible backbone
	 * 
	 * @return ArrayList with InetAddress in each index
	 */
	public ArrayList<InetAddress> getBackbones() {

		return backbones;
	}

	/**
	 * Method returns the personal backbone, to which the device is actually
	 * connected.
	 * 
	 * @return InetAddress of own connected backbone. If there is no connection,
	 *         the object is NULL
	 */
	public InetAddress getMyOwnBackbone() {
		if(isConnectedToBbn.get()) {
			return myOwnBackbone;
		} else {
			return null;
		}
	}
	
	/**
	 * This function finds the backbone with the best (relying on the ETX-Value of OLSR) connection,
	 * and sets "myOwnBackbone" to the according IP.
	 * @throws UnknownHostException
	 */
	private void findBestBackbone() throws UnknownHostException {
		links = TopologyInfo.getLinks();
		int bestBBNindex = backbones.size();
		float tempCost = 999;
		boolean found = false;
		int bbnIndex;
		
		for (int i = 0; i < links.size(); i++) {
			bbnIndex = 0;
			while (bbnIndex < backbones.size()) {
				if (links.get(i).getIp().equals(backbones.get(bbnIndex))) {
					found = true;
					if (links.get(i).getCost() < tempCost) {
						bestBBNindex = i;
						tempCost = links.get(i).getCost();
					}
				}
				bbnIndex++;
			}
		}
		if (found)
			myOwnBackbone = links.get(bestBBNindex).getIp();
		else
			myOwnBackbone = null;
	}
	
	
	/**
	 * Sends a Packetstring via TCP to the backbone
	 * @param packetStream the stringified Packet to send
	 */
	protected void sendPacketStream(byte[] packetStream){
		packetqueue.add(packetStream);
	}
}
