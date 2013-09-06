package de.fu_berlin.maniac.strategies;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import de.fu_berlin.maniac.exception.ManiacException;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.general.ManiacStrategyInterface;
import de.fu_berlin.maniac.general.Mothership;
import de.fu_berlin.maniac.network_manager.NetworkManager;
import de.fu_berlin.maniac.network_manager.TopologyInfo;
import de.fu_berlin.maniac.packet_builder.*;
import com.example.maniaclib.SophisticatedActivity;

public class MyStrategy implements ManiacStrategyInterface {
	LinkedList<String> log;
	boolean sw = true;
	Random rand;
	public MyStrategy(SophisticatedActivity sa) {
		super();
		rand = new Random(System.currentTimeMillis());
		DataStore.myAddress = (Inet4Address) TopologyInfo.getInterfaceIpv4("wlan0");
		
		Inet4Address myAddress1 = null;
		try {
			myAddress1 = (Inet4Address) Inet4Address.getByName("192.168.1.100");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		log = sa.getLog();
		ArrayList<String> sasa = new ArrayList<String>();
		sasa.add("sasa");
		sasa.remove(null);
		log.add("I have started "+DataStore.myAddress+" "+sasa + (DataStore.myAddress.equals(myAddress1)));
	}

	@Override
	public Long onRcvAdvert(Advert adv) {
		DataStore.addAdvert(adv);
		//log.add(">>>> I got an advert "+adv.getTransactionID());
		if(NetworkManager.myOwnBackbone != null && NetworkManager.myOwnBackbone.equals(adv.getFinalDestinationIP()))
			return (long) 1;
		else
			return (long) (Mothership.getAuctionTimeout());
	}

	@Override
	public void onRcvBid(Bid bid) {
			DataStore.addBid(bid);
	}

	@Override
	public void onRcvBidWin(BidWin bidwin) {
			DataStore.addBidWin(bidwin);
	}

	@Override
	public AuctionParameters onRcvData(Data packet) {
		AuctionParameters auctionParameters = null;
			int myBidWin = DataStore.myBidWins.get(packet.getTransactionID());
	
			int length = (int) DataStore.calculateTopologyDistance(DataStore.myAddress, packet.getFinalDestinationIP());
			length = 4;
	
			int myFairProfit = packet.getInitialBudget() / length;
			int maxBid = myBidWin - myFairProfit;
			if(maxBid < myBidWin / 2)
				maxBid = myBidWin / 2;
	
			int fine = maxBid - 1;
			auctionParameters = new AuctionParameters(maxBid, fine);
			DataStore.addDataPacket(packet);
			log.add(">>>>we did GET data" + packet.getTransactionID() +" "+ packet.getFinalDestinationIP());
		return auctionParameters;
	}

	@Override
	public Bid selectWinner(List<Bid> bids) {
		Bid ret = null;
			if(bids.isEmpty()){
				return null;
			}
			// Get the 4 relevant statistics
			ArrayList<NodeValue> nodesRichness = DataStore.createRichnessList(bids);
			ArrayList<NodeValue> nodesDistance = DataStore.createDistanceList(bids);
			ArrayList<NodeValue> nodesBids = DataStore.createBidsList(bids);
			ArrayList<NodeValue> nodesTrustfulness = DataStore.createTrustfulnessList(bids);
	
			// Merge the 4 statistics
			ArrayList<ArrayList<NodeValue>> allLists = new ArrayList<ArrayList<NodeValue>>();
			allLists.add(nodesRichness);
			allLists.add(nodesDistance);
			allLists.add(nodesBids);
			allLists.add(nodesTrustfulness);
			ArrayList<NodeValue> mergedOrder = DataStore.mergeLists(allLists);
	
			// Extract the winning bid
			for(Bid bid : bids) {
				if(bid.getSourceIP().equals(mergedOrder.get(0).nodeName)) {
					DataStore.addWinner(bid);
					log.add(">>>>we did SEND data");
					return bid;
				}
			}
			log.add(">>>>we did SEND data");
		return ret;
	}

	@Override
	public void onException(ManiacException ex, boolean fatal) {
	}

	@Override
	public Integer sendBid(Advert adv) {
		int myBid = Integer.MAX_VALUE;
		if(NetworkManager.myOwnBackbone != null && NetworkManager.myOwnBackbone.equals(adv.getFinalDestinationIP()))
			myBid = 1;
		else {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			log.add(">>>>I sleep");
		}
		
		log.add(">>>>I bidded " + myBid + " on " + adv.getTransactionID());
		
		return myBid;
	}

	@Override
	public boolean dropPacketBefore(Data buffer_data) {
		if(buffer_data.getHopCount() == 0) {
			return true;
		}
		return false;
	}

	@Override
	public boolean dropPacketAfter(Data buffer_data) {
		return false;
	}
}
