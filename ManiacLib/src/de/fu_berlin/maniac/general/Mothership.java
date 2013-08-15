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

package de.fu_berlin.maniac.general;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;

import de.fu_berlin.maniac.auction_manager.Auction;
import de.fu_berlin.maniac.auction_manager.MyAuctionManager;
import de.fu_berlin.maniac.exception.MalformedPacketException;
import de.fu_berlin.maniac.exception.NegativeBidException;
import de.fu_berlin.maniac.network_manager.Link;
import de.fu_berlin.maniac.network_manager.NetworkManager;
import de.fu_berlin.maniac.network_manager.Receiver;
import de.fu_berlin.maniac.network_manager.Sender;
import de.fu_berlin.maniac.network_manager.TopologyInfo;
import de.fu_berlin.maniac.packet_builder.*;
import de.fu_berlin.maniac.strategies.DefaultStrategy;

/**
 * Main control-class doing packet processing and calling the user strategy.
 * All incoming packets go through this class. Depending on the type, Mothership will do whatever is
 * necessary, i. e. calls your onReceiveAdvert() if an Advert-Packet for you comes in.
 * @author maniacchallenge
 * 
 */
public class Mothership extends Thread {

	/**
	 * Amount of time the program waits until it chooses a winner for the
	 * Auction, in ms
	 */
	final static int AUCTION_TIMEOUT = 3000;

	/** Polling rate for packet queue */
	final long POLLING_RATE = 100L;

	/** Initial ThreadPool capacity */
	final int THREAD_CAPACITY = 8;
	
	/**Initial Hashtable size */
	final int HASHTABLE_SIZE = 100;
	
	/** IP Address of the backbone currently associated with this node */
	private InetAddress myOwnBackbone;

	
	Receiver receiver;
	Sender sender;

	/**
	 * Packet queue where the Receiver puts the packets and Mothership polls it
	 * to get them
	 */
	LinkedBlockingQueue<Packet> packetqueue;

	/**
	 * This Hashtable keeps track of all the transactionIDs of Data packets that
	 * this node has won (so the node can not bid for Data it has already won
	 * previously
	 */
	Hashtable<Integer, Boolean> won_transactionIds;

	
	/** Keeps track of all the AuctionParamters from received adverts */
	Hashtable<Integer, AuctionParameters> recv_adverts;

	
	ManiacStrategyInterface strategy;
	MyAuctionManager auctionmanager;

	ScheduledThreadPoolExecutor auctionThreads;
	ScheduledThreadPoolExecutor biddelayThreads;

	/** Buffer objects for processing */
	Packet buffer_packet;
	Data buffer_data;
	Advert buffer_adv;
	Bid buffer_bid;
	BidWin buffer_bidwin;
	Long bid_delay;
	boolean dropPacket;
	
	
	BidDelayer delayer;
	

	/**
	 * All received packets are put in this ArrayList so the UI (Test app) can
	 * display incoming traffic. For test purposes, not important for functionality
	 */
	ArrayList<Packet> received_packets;
 
	/** List of backbones, gathered from a txt file on this device*/
	ArrayList<InetAddress> backbones;
	
	NetworkManager netman;
	PacketBuilder packetbuilder;
	
	ManiacLogger mlogger;


	public Mothership(Context con) throws SocketException {
		receiver = new Receiver();
		receiver.start();
		sender = new Sender();
		packetqueue = receiver.getPacketQueue();
		won_transactionIds = new Hashtable<Integer, Boolean>(HASHTABLE_SIZE);
		auctionmanager = new MyAuctionManager();
		received_packets = new ArrayList<Packet>();
		auctionThreads = new ScheduledThreadPoolExecutor(THREAD_CAPACITY);
		biddelayThreads = new ScheduledThreadPoolExecutor(THREAD_CAPACITY);
		netman = NetworkManager.getInstance();
		packetbuilder = PacketBuilder.getInstance();
		recv_adverts = new Hashtable<Integer, AuctionParameters>(HASHTABLE_SIZE);
		backbones = netman.getBackbones();
		mlogger = new ManiacLogger(con);
	}


	/**
	 * 
	 */
	public void run() {
	
		if (this.getStrategy().equals(null)) {
			this.setStrategy(new DefaultStrategy());
		}
		netman.start();
	
		
		while (true) {
		
			/** Refresh backbone*/
			myOwnBackbone = netman.getMyOwnBackbone();
			
			
			/**Get new packet*/
			try {
				buffer_packet = packetqueue.poll(POLLING_RATE,
						TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	
			if (buffer_packet == null) {
				/**Start loop from the beginning*/
			} else {
				
				/**Log packet*/
				try {
					mlogger.sendToLogger(buffer_packet);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
	
				/** Add packet so the UI can display */
				received_packets.add(buffer_packet);
	
				
				/**
				 * Packet handling occurs here, not all packets are passed to
				 * the user strategy
				 */
				switch (buffer_packet.getType()) {
	
				/** Advert */
				case 'A':
					try {
	
						buffer_adv = (Advert) buffer_packet;
						
						
						
						
						
						if (isForUser(buffer_adv)) {	
							
							/**Add the adverts AuctionParameters to the list of received adverts*/
							recv_adverts.put(buffer_adv.getTransactionID(),
									new AuctionParameters(buffer_adv.getCeil(),
											buffer_adv.getFine()));
							
							
							/** If the advert is for the user, call onRcvAdvert() and delay the bid sent out by 
							 * the return value.*/
							
							bid_delay = strategy.onRcvAdvert(buffer_adv);
							if (bid_delay == null || bid_delay <= 0) {
								Integer bid = strategy.sendBid(buffer_adv);
	
								if (bid == null || bid < 0
										|| bid > buffer_adv.getCeil()) {
									bid = buffer_adv.getCeil();
								}
	
								sender.send(packetbuilder.buildBid(buffer_adv,
										bid));
	
							} else {
								if (bid_delay > AUCTION_TIMEOUT) {
									Integer long_timeout = AUCTION_TIMEOUT;
									bid_delay = long_timeout.longValue();
								}
								delayer = new BidDelayer(strategy,
										buffer_adv, sender);
								biddelayThreads.schedule(delayer, bid_delay, TimeUnit.MILLISECONDS);
								
							}
	
						}
	
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (NegativeBidException e) {
	
						strategy.onException(e, false);
	
					}
	
					break;
	
				/** Bid */
				case 'B':
	
					buffer_bid = (Bid) buffer_packet;
	
					/**
					 * If the bid is for a Data packet currently being auctioned
					 * by this node, add it to the appropriate auctionmanager
					 * list
					 */
					if (auctionmanager.getAuctions().containsKey(
							buffer_packet.getTransactionID())) {
						auctionmanager.getAuctions()
								.get(buffer_packet.getTransactionID())
								.add((Bid) buffer_packet);
					}
	
					/** Pass every bid to user so they can log */
					strategy.onRcvBid((Bid) buffer_packet);
	
					break;
	
				/** BidWin */
				case 'W':
	
					buffer_bidwin = (BidWin) buffer_packet;
	
					/** Pass every BidWin to user so they can log */
					strategy.onRcvBidWin(buffer_bidwin);
	
					break;
	
				/** Data */
				case 'D':
					
					buffer_data = (Data) buffer_packet;
					if(buffer_data.getHopCount() != 0){
						
						/**Ask user if he wants to drop the packet*/
						dropPacket = strategy.dropPacketBefore(buffer_data);
						
						if(!dropPacket){
							
							
							/**If the data packet is destined for the backbone associated with this node,
							 * send it there.*/
							
							if (buffer_data.getFinalDestinationIP().equals(myOwnBackbone)) {
								packetbuilder.updateData(buffer_data,
										(Inet4Address) myOwnBackbone);
								sender.send(buffer_data);
							}
							
							
							/** If the data packet hasnt been won before AND the user does not want to drop it, start an auction:*/
							else if (!won_transactionIds.containsKey(buffer_data
									.getTransactionID())) {
								
								AuctionParameters parameters;
								
								/** Get the AuctionParameters with which the data packet should be auctioned from the user*/
								parameters = strategy.onRcvData(buffer_data);
								
								/**
								 * If user returns null then take the winning bid for
								 * this data as maxBid and the fine from the last
								 * auction (included in data)
								 */
								if (parameters == null) {
									parameters = recv_adverts.get(buffer_data
											.getTransactionID());
								} else {
									/**If the user return was not null, update the fine in the data packet*/
									buffer_data = (Data)packetbuilder.updateData(buffer_data, parameters.getFine());
								}
								
								
								int last_fine = recv_adverts.get(
										buffer_data.getTransactionID()).getFine();
								
								/**
								 * If fine is bigger than the fine from last auction,
								 * set it to fine from last auction*/
								
								if (buffer_data.getFine() > last_fine) {
									buffer_data = (Data) packetbuilder.updateData(buffer_data, last_fine);
								}
								
								/** Register Auction at AuctionManager*/
								auctionmanager.handleAuction((Advert) packetbuilder
										.buildAdvert(buffer_data,
												parameters.getMaxbid(),
												parameters.getFine()), sender);
								
								
								/** Start Auction*/
								auctionThreads.schedule(new Auction(buffer_data,
										sender, strategy, auctionmanager,
										this.myOwnBackbone), AUCTION_TIMEOUT,
										TimeUnit.MILLISECONDS);
								
								
							}
							
						}
						/**
						 * Remember that this data has been processed so it
						 * cannot be bid on in the future!
						 */
						won_transactionIds.put(buffer_data.getTransactionID(),
								true);
					}
					
					
					break;
	
				default:
					strategy.onException(new MalformedPacketException(), false);
	
				}
			}
		}
	}

	/**
	 * A advert is for the user if the data packet being advertised has not already been won by this node
	 * at some other auction and also if the advert is either from this nodes associated backbone OR any other node.
	 * (So that excludes advert from other backbones)
	 * @param dg_pck
	 * @return
	 * @throws UnknownHostException
	 */
	public boolean isForUser(Packet dg_pck) throws UnknownHostException {

		boolean isForUser = false;
		
		InetAddress advertiser = dg_pck.getSourceIP();
		ArrayList<Link> links = TopologyInfo.getLinks();
		
		for(int i = 0;i<links.size();i++){
			if(links.get(i).getIp() == advertiser){
				isForUser = true;
			}
			
		}
		
		/*
		if (backbones.contains(dg_pck.getSourceIP())
				&& !dg_pck.equals(myOwnBackbone)) {
			return false;
		} else 
		*/
		if (!won_transactionIds.containsKey(dg_pck.getTransactionID()) && isForUser) {
	
			return isForUser;
		}
	
		return false;
	}

	/**
	 * Use this function to give Mothership your strategy.
	 * DO THIS BEFORE SENDING OR RECEIVING OR ANYTHING ELSE!
	 * (If you don't, the defaul strategy is used, and you don't want that...trust me, you don't.)
	 */
	public void setStrategy(ManiacStrategyInterface strat) {
		this.strategy = strat;
	}


	public InetAddress getMyOwnBackbone() {
		return myOwnBackbone;
	}

	/**This is public so users can get the AUCTION_TIMEOUT constant*/
	public static int getAuctionTimeout() {
		return AUCTION_TIMEOUT;
	}
	
	/**This is public so users can verify that they Mothership is using the rigth strategy*/
	public ManiacStrategyInterface getStrategy() {
		return this.strategy;
	}

	public ArrayList<Packet> getReceived_packets() {
		return received_packets;
	}
	
	/* These are all just there for the debug function "startAuction" in SophisticatedActivity,
	 * Are not needed for the challenge
	public ArrayList<Packet> getReceived_packets() {
		return received_packets;
	}
	public Sender getSender() {
		return sender;
	}
	

	public MyAuctionManager getAuctionManager() {
		return this.auctionmanager;

	}

	protected ScheduledThreadPoolExecutor getThreadPoolExecutor() {
		return this.auctionThreads;
	}
	*/

}
