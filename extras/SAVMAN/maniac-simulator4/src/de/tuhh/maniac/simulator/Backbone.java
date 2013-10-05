/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.tuhh.maniac.simulator;

import de.fu_berlin.maniac.auction_manager.Auction;
import de.fu_berlin.maniac.auction_manager.MyAuctionManager;
import de.fu_berlin.maniac.exception.MalformedPacketException;
import de.fu_berlin.maniac.exception.NegativeBidException;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.general.BidDelayer;
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
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
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

            public Backbone(Inet4Address ownIP) throws SocketException {
                        this.IP = ownIP;
                        TopologyInfo tp = new TopologyInfo(IP);
                        packetbuilder = PacketBuilder.getInstance(ownIP, tp);
                        //netman = NetworkManager.getInstance(IP, tp);
                        receiver = new Receiver(IP, tp, packetbuilder);
                        receiver.start();
                                   sender = new Sender(IP,packetbuilder);         

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

                        //System.out.println("I run: " + this.IP);




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
                                                //System.out.println("[" + this.IP + "] Backbone: Received something" );


                                                switch (buffer_packet.getType()) {

                                                            /**
                                                             * Advert
                                                             */
                                                            case 'A':
                                                                      System.out.println("[" + this.IP + "] Backbone: Received advert - do nothing" );
                                                                   

                                                                        break;

                                                            /**
                                                             * Bid
                                                             */
                                                            case 'B':
                                                                            
                                                                        buffer_bid = (Bid) buffer_packet;


                                                                        if (auctionmanager.getAuctions().containsKey(
                                                                                buffer_packet.getTransactionID())) {
                                                                                   System.err.println("[" + this.IP + "] Backbone: Received bid  for a running auction" );
                                                                               
                                                                                    auctionmanager.getAuctions()
                                                                                            .get(buffer_packet.getTransactionID())
                                                                                            .add((Bid) buffer_packet);
                                                                        }


                                                                        break;

                                                            /**
                                                             * BidWin
                                                             */
                                                            case 'W':
                                                                       System.out.println("[" + this.IP + "] Backbone: received a Bidwin");
                                                                        buffer_bidwin = (BidWin) buffer_packet;

                                                                        /**
                                                                         * Pass
                                                                         * every
                                                                         * BidWin
                                                                         * to
                                                                         * user
                                                                         * so
                                                                         * they
                                                                         * can
                                                                         * log
                                                                         */
                                                                        // SIM have to check this out
                                                                        //strategy.onRcvBidWin(buffer_bidwin);

                                                                        break;

                                                            /**
                                                             * Data
                                                             */
                                                            case 'D':
                                                                        System.out.println("[" + this.IP + "] Backbone: received data - here we have to implement stuff!");
                                                                        buffer_data = (Data) buffer_packet;

                                                                        /**
                                                                         * Ask
                                                                         * user
                                                                         * if he
                                                                         * wants
                                                                         * to
                                                                         * drop
                                                                         * the packet
                                                                         */
                                                                      

                                                                        break;

                                                            default:
                                                              System.out.println("[" + this.IP + "] Backbone: Error!");

                                                }
                                    }

                                    // System.out.println("Still running and ok " + this.getName());
                        }

            }

            /**
             * A advert is for the user if the data packet being advertised has
             * not already been won by this node at some other auction and also
             * if the advert is either from this nodes associated backbone OR
             * any other node. (So that excludes advert from other backbones)
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
                        long t1, t0 = System.currentTimeMillis();
                        do {
                                    t1 = System.currentTimeMillis();
                        } while (t1 - t0 < 3000);

                        DatagramPacket packet = null;
                        DatagramSocket toSocket = null;

                                    Inet4Address backboneIP = this.IP;
                                    TopologyInfo backtop = new TopologyInfo(backboneIP);
                                    //PacketBuilder pb = PacketBuilder.getInstance(backboneIP, backtop);

                                    // Fake a data packet
                                    String s = "This is fake data";
                                    // Data int transactionID, int hopCount, int fine, byte[] data, Inet4Address finalDest, initialBugget
                                    Data dp = new Data(1000, 5, 150, s.getBytes(), destination, 200);
                                    
                                    // Data data, int maxBid, int fine)
                                    Packet adv = packetbuilder.buildAdvert(dp, 200, 100);

                                    System.out.println("[" + this.IP + "] Backbone: next destination: " + adv.getDestinationIP());
                                    System.out.println("[" + this.IP + "] Backbone:  final destination: " + ((Advert) adv).getFinalDestination());
                                    System.out.println("[" + this.IP + "] Backbone:  src ip: " + adv.getSourceIP());

                                    System.out.println("[" + this.IP + "] Backbone: Packet .toString: " + adv.toString());

                                    System.out.println("[" + this.IP + "] Backbone: Send packet from " + sender.toString());             
                                    
                                    /** Register Auction at AuctionManager*/
	

                                    auctionmanager.handleAuction((Advert)adv, sender);
                                    System.err.println("[" + this.IP + "] Backbone: START THREAD AUCTION AT: " + System.currentTimeMillis());
                                    auctionThreads.schedule(new Auction(dp,sender, new DefaultStrategy(), auctionmanager, this.IP, this.packetbuilder), AUCTION_TIMEOUT,TimeUnit.MILLISECONDS);
                                    
                                    //DatagramPacket dgram = pb.getDatagramPacket(adv);       
                                    //toSocket = new DatagramSocket(8767, backboneIP);
                                    
                                    
                                   // toSocket.send(dgram);
                      


            }
}
