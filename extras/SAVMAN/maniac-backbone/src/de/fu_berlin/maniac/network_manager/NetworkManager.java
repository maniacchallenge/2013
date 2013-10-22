package de.fu_berlin.maniac.network_manager;

//import android.os.Environment;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.fu_berlin.maniac.packet_builder.*;
import de.fu_berlin.maniac.bank.*;
import java.net.Inet4Address;

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
	static LinkedBlockingQueue<String> packetqueue;
	
	// Variables for own Backbone
	private static volatile InetAddress myOwnBackbone;
	private static AtomicBoolean isConnectedToBbn;
	private AtomicInteger topChecker;
	private ArrayList<Link> links;
	
	private TCPHandler tcpHandler;
	private TCPSender tcpSender;
	
	private Inet4Address IP;
	private TopologyInfo tp;

	private NetworkManager(Inet4Address IP,  String intf) {
		this.IP = IP;
		backbones = new ArrayList<InetAddress>();
		myOwnBackbone = null;
		isConnectedToBbn = new AtomicBoolean();
		isConnectedToBbn.getAndSet(false);
		topChecker = new AtomicInteger(0);
		packetqueue = new LinkedBlockingQueue<String>();
		bankman = new BankManager();
		packetbuilder = PacketBuilder.getInstance(intf);
	}
	
	public static NetworkManager getInstance(Inet4Address IP,  String intf){
		
		
		if (networkManager == null){
			networkManager = new NetworkManager(IP, intf);
		}
		return networkManager;
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
				if(myOwnBackbone==null) {
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
					System.err.println("No backbone found.");
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
				if(myOwnBackbone==null) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					isConnectedToBbn.getAndSet(true);
				// SIM: Dont connect to a backbone, simply say, we are connected
					/*try {
						System.out.println("my backbone: "+myOwnBackbone);
						clientSocket = new Socket(myOwnBackbone, 51113);
						isConnectedToBbn.getAndSet(true);
						clientSocket.setSoTimeout(3000);
						tcpSender = new TCPSender(clientSocket);
						tcpSender.start();
						readPackets(clientSocket.getInputStream());		
					} catch (SocketTimeoutException e1) {
						//e1.printStackTrace();
						isConnectedToBbn.getAndSet(false);
						myOwnBackbone = null;
					} catch (IOException e2) {
						e2.printStackTrace();
					} finally {
						try {
							if(clientSocket !=null) {
								clientSocket.close();								
							}
							isConnectedToBbn.getAndSet(false);
							myOwnBackbone = null;
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}	
					*/
				}
			}
		}
		
		/**
		 * reads incoming Checks from the TCP socket, parses them and sends them to the BankManager
		 */
		private void readPackets(InputStream in){
			try{
				bufferedReader = new BufferedReader(
					new InputStreamReader(in));
				while(true){
					if(in.available()>0){
						String rawData = bufferedReader.readLine();
						if(rawData!=null) { // brauchen wir das hier �berhaupt? die ist doch per definition nicht null..
							Check check = (Check) packetbuilder.buildCheck(rawData);	
							bankman.update(check);
						}
					}
				}
			}catch(IOException ioe){
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
			String pkt;
			try {
				while((pkt = packetqueue.take()) != null){					
					dataOutputStream.writeBytes(pkt);
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
		/*
		File sdcard = Environment.getExternalStorageDirectory();
		*/
		File file = new File( "generic.txt"); // working directory
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
		// Wow this is ugly, TODO replace
		/*
		System.err.println("networkmanager IP is: : " + this.IP);
		if(this.IP.equals(Inet4Address.getByName("127.0.0.10")) || this.IP.equals(Inet4Address.getByName("127.0.0.11")) || this.IP.equals(Inet4Address.getByName("127.0.0.12"))){
			myOwnBackbone = Inet4Address.getByName("127.0.0.5");
			System.err.println("backbone is: " + myOwnBackbone);
		} else if(this.IP.equals(Inet4Address.getByName("127.0.0.20"))){
			myOwnBackbone = Inet4Address.getByName("127.0.0.6");
			System.err.println("backbone is: " + myOwnBackbone);
		} else if(this.IP.equals(Inet4Address.getByName("127.0.0.30"))){
			myOwnBackbone = Inet4Address.getByName("127.0.0.7");
			System.err.println("backbone is: " + myOwnBackbone);
		} else{
			myOwnBackbone = null;
		}		
	*/
		
		
		
	
		if (topChecker.get()==0){ 
			links = tp.getLinks();
		}
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
		
		// myOwnBackbone = found ? links.get(bestBBNindex).getIp() : null;
		// Maybe(TM) breakpoints will work better, if we change it to this:
		if (found){
			myOwnBackbone = links.get(bestBBNindex).getIp();
			System.out.println("[" + this.IP + "] Set Backbone to: " +  myOwnBackbone);
		}else{
			myOwnBackbone = null;
		}
	}
	
	
	/**
	 * Sends a Packetstring via TCP to the backbone
	 * @param packetStream the stringified Packet to send
	 */
	protected void sendPacketStream(String packetStream){
		packetqueue.add(packetStream);
	}
}
