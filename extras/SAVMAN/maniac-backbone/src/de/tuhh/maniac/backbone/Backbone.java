/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.tuhh.maniac.backbone;

import de.fu_berlin.maniac.auction_manager.Auction;
import de.fu_berlin.maniac.auction_manager.MyAuctionManager;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.network_manager.NetworkManager;
import de.fu_berlin.maniac.network_manager.Receiver;
import de.fu_berlin.maniac.network_manager.Sender;
import de.fu_berlin.maniac.network_manager.TopologyInfo;
import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.Bid;
import de.fu_berlin.maniac.packet_builder.BidWin;
import de.fu_berlin.maniac.packet_builder.Data;
import de.fu_berlin.maniac.packet_builder.Packet;
import de.fu_berlin.maniac.packet_builder.PacketBuilder;
import de.fu_berlin.maniac.strategies.DefaultStrategy;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author NetFalcon
 */
public class Backbone extends Thread {

          final int AUCTION_TIMEOUT = 3000;
          final int THREAD_CAPACITY = 20;
          final long POLLING_RATE = 100L;
          Receiver receiver;
          NetworkManager netman;
          PacketBuilder packetbuilder;
          private Inet4Address IP;
          private final LinkedBlockingQueue<Packet> packetqueue;
          private final ArrayList<Packet> received_packets;
          private final ScheduledThreadPoolExecutor auctionThreads;
          private final Hashtable<Integer, AuctionParameters> recv_adverts;
          /**
           * Buffer objects for processing
           */
          Packet buffer_packet;
          Data buffer_data;
          Advert buffer_adv;
          Bid buffer_bid;
          BidWin buffer_bidwin;
          private final MyAuctionManager auctionmanager;
          Sender sender;
          // Be aware to set the right Interface-Name (e.g. net8)
          private static String interfacename = "eth1";
          // set the destination for one packet
          private static String nexus7destination = "192.168.0.132";
          private static int loops = 1;
          private static long delay = 3000;
          private static boolean isReceiver = false;
          private static int deadline = 5;
          private static int fine = 150;
          private static int initialBudget = 200;

          public static void main(String[] args) {




	         // List Network Interfaces, helpful if many unknown 
	          Enumeration<NetworkInterface> nets = null;
	          try {
	          System.out.println("List of available interfaces: Set right interface in Backbone.java!");
	          nets = NetworkInterface.getNetworkInterfaces();
	          for (NetworkInterface netint : Collections.list(nets))
	          displayInterfaceInformation(netint);
	          } catch (SocketException ex) {
	          Logger.getLogger(Backbone.class.getName()).log(Level.SEVERE, null, ex);
	          } 

	         try {
		        if (args.length > 0 && !args[0].isEmpty()) {
			       interfacename = args[0];
		        }
		         if (args.length > 1 && !args[1].isEmpty()) {
			       isReceiver = Boolean.parseBoolean(args[1]);
		        }
		        if (args.length > 2 && !args[2].isEmpty()) {
			       nexus7destination = args[2];
		        }
		        if (args.length > 3 && !args[3].isEmpty()) {
			       loops = Integer.parseInt(args[3]);
		        }
		        if (args.length > 4 && !args[4].isEmpty()) {
			       delay = Long.parseLong(args[4]);
		        }
		        if (args.length > 5 && !args[5].isEmpty()) {
			       deadline = Integer.parseInt(args[5]);
		        }
		        if (args.length > 6 && !args[6].isEmpty()) {
			       fine = Integer.parseInt(args[6]);
		        }
		        if (args.length > 7 && !args[7].isEmpty()) {
			       initialBudget = Integer.parseInt(args[7]);
		        }
		         
		        System.out.println("Is System Receiver: " + isReceiver);
		        if(!isReceiver)
		        System.out.println("Starting to send " + loops + " messages over interface " + interfacename + " with destination " + nexus7destination + " every " + delay + " milliseconds.\nPackets will "
			     + "have a deadline of " + deadline + ", a fine of: " + fine + " and initialBudget: " + initialBudget);
		        Backbone test = new Backbone();
		        test.start();
		        // sending 
		        if(!isReceiver){
			       test.sendAdvert((Inet4Address) Inet4Address.getByName(nexus7destination));
		        }
	         } catch (SocketException ex) {
		        Logger.getLogger(Backbone.class.getName()).log(Level.SEVERE, null, ex);
	         } catch (UnknownHostException ex) {
		        Logger.getLogger(Backbone.class.getName()).log(Level.SEVERE, null, ex);
	         }




          }

          static void displayInterfaceInformation(NetworkInterface netint) throws SocketException {
	         //System.out.printf("Display name: %s\n", netint.getDisplayName());
	         System.out.printf("Name: %s\n", netint.getName());
	         Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
	         for (InetAddress inetAddress : Collections.list(inetAddresses)) {
		        System.out.printf("InetAddress: %s\n", inetAddress);
	         }
          }

          public Backbone() throws SocketException {
	         this.IP = (Inet4Address) TopologyInfo.getInterfaceIpv4(interfacename);
	         //TopologyInfo tp = new TopologyInfo(IP);
	         packetbuilder = PacketBuilder.getInstance(interfacename);
	         //netman = NetworkManager.getInstance(IP, tp);
	         receiver = new Receiver(IP, packetbuilder, interfacename);
	         receiver.start();
	         sender = new Sender(IP, interfacename);

	         packetqueue = receiver.getPacketQueue();
	         auctionmanager = new MyAuctionManager();
	         received_packets = new ArrayList<Packet>();
	         auctionThreads = new ScheduledThreadPoolExecutor(THREAD_CAPACITY);
	         recv_adverts = new Hashtable<Integer, AuctionParameters>(50);

          }

          @SuppressWarnings("static-access")
          /**
           *
           */
          public void run() {

	         while (true) {

		        /**
		         * Get new packet
		         */
		        try {
			       buffer_packet = packetqueue.poll(POLLING_RATE,
				    TimeUnit.MILLISECONDS);
		        } catch (InterruptedException e) {
			       e.printStackTrace();
		        }

		        if (buffer_packet == null) {
			       // System.out.println("SIM: No packet received [" + this.IP + "]");
			       /**
			        * Start loop from the beginning
			        */
		        } else {
			       // System.out.println("[" + packetbuilder.getDeviceIP() + "] Backbone: Received something" );


			       switch (buffer_packet.getType()) {

				      /**
				       * Advert
				       */
				      case 'A':
					     System.out.println("[" + new Date() + "]" +"[" + packetbuilder.getDeviceIP() + "] Backbone: Received advert. do nothing.");


					     break;

				      /**
				       * Bid
				       */
				      case 'B':

					     buffer_bid = (Bid) buffer_packet;
					     

					     if (auctionmanager.getAuctions().containsKey(
						  buffer_packet.getTransactionID())) {
						    System.out.println("[" + new Date() + "]" +"[" + packetbuilder.getDeviceIP() + "] Backbone: Received bid for a running auction.");
						    System.out.println(buffer_packet.toString());
						    auctionmanager.getAuctions()
							 .get(buffer_packet.getTransactionID())
							 .add((Bid) buffer_packet);
					     }


					     break;

				      /**
				       * BidWin
				       */
				      case 'W':
					     System.out.println("[" + new Date() + "]" +"[" + packetbuilder.getDeviceIP() + "] Backbone: Received Bidwin. do nothing.");
					     buffer_bidwin = (BidWin) buffer_packet;

					     /**
					      * Pass every BidWin
					      * to user so they
					      * can log
					      */
					     // SIM have to check this out
					     //strategy.onRcvBidWin(buffer_bidwin);
					     break;

				      /**
				       * Data
				       */
				      case 'D':
					     System.out.println("[" + new Date() + "]" +"[" + packetbuilder.getDeviceIP() + "] Backbone: Received data!  -> end of trip?! implement it!");
					     buffer_data = (Data) buffer_packet;
					     System.out.println(buffer_data.toString());
					     /**
					      * Ask user if he
					      * wants to drop the
					      * packet
					      */
					     break;

				      default:
					     System.out.println("[" + new Date() + "]" +"[" + packetbuilder.getDeviceIP() + "] Backbone: Error!");

			       }
		        }

		        // System.out.println("Still running and ok " + this.getName());
	         }

          }

          /**
           * A advert is for the user if the data packet being advertised has
           * not already been won by this node at some other auction and also if
           * the advert is either from this nodes associated backbone OR any
           * other node. (So that excludes advert from other backbones)
           *
           * @param dg_pck
           * @return
           * @throws UnknownHostException
           */
          public boolean isForUser(Packet dg_pck) throws UnknownHostException {


	         // Not really sure what a backbone should do here ;)
	         // its always for him
	         return true;
          }

          public void sendAdvert(Inet4Address destination) {


	         int counter = 0;
	         //random number to start with different ids
	         int random = (int) (Math.random() * 1000);

	         while (counter < loops) {
		        long t1, t0 = System.currentTimeMillis();
		        do {
			       t1 = System.currentTimeMillis();
		        } while (t1 - t0 < delay);

		        DatagramPacket packet = null;
		        DatagramSocket toSocket = null;

		        Inet4Address backboneIP = this.IP;
		        //TopologyInfo backtop = TopologyInfo.getInterfaceIpv4(null);
		        //PacketBuilder pb = PacketBuilder.getInstance(backboneIP, backtop);

		        // Fake a data packet
		        String s = "This is fake data";
		        // Data int transactionID, int hopCount, int fine, byte[] data, Inet4Address finalDest, initialBugget
		        Data dp = new Data(counter + random, deadline, fine, s.getBytes(), destination, initialBudget);

		        // Data data, int maxBid, int fine)
		        Packet adv = packetbuilder.buildAdvert(dp, 200, fine);

		        //System.out.println("[" + packetbuilder.getDeviceIP() + "] Backbone: next destination: " + adv.getDestinationIP());
		        //System.out.println("[" + packetbuilder.getDeviceIP() + "] Backbone: final destination: " + ((Advert) adv).getFinalDestination());
		       // System.out.println("[" + packetbuilder.getDeviceIP() + "] Backbone: src ip: " + adv.getSourceIP());

		        //System.out.println("[" + new Date() + "]" +"[" + packetbuilder.getDeviceIP() + "] Backbone: Packet .toString: " + adv.toString());

		        //System.out.println("[" + packetbuilder.getDeviceIP() + "] Backbone: Send packet from " + sender.toString());

		        /**
		         * Register Auction at AuctionManager
		         */
		        auctionmanager.handleAuction((Advert) adv, sender);
		        DateFormat df = DateFormat.getDateTimeInstance();
		        System.err.println("[" + new Date() + "]" + "[" + packetbuilder.getDeviceIP() + "] Backbone: Start Auction at: " + df.format(System.currentTimeMillis()));
		        auctionThreads.schedule(new Auction(dp, sender, new DefaultStrategy(), auctionmanager, this.IP, this.packetbuilder), AUCTION_TIMEOUT, TimeUnit.MILLISECONDS);

		        //DatagramPacket dgram = pb.getDatagramPacket(adv);       
		        //toSocket = new DatagramSocket(8767, backboneIP);


		        // toSocket.send(dgram);
		        counter++;
	         }


          }
}
