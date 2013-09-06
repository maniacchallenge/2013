package de.fu_berlin.maniac.strategies;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import de.fu_berlin.maniac.exception.ManiacException;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.general.ManiacStrategyInterface;
import de.fu_berlin.maniac.general.Mothership;
import de.fu_berlin.maniac.network_manager.TopologyInfo;
import de.fu_berlin.maniac.packet_builder.*;
import com.example.maniaclib.SophisticatedActivity;

public class MyStrategy implements ManiacStrategyInterface {
	LinkedList<String> log;

	public MyStrategy(SophisticatedActivity sa) {
		super();
		DataStore.myAddress = (Inet4Address) TopologyInfo.getInterfaceIpv4("wlan0");
		
		Inet4Address myAddress1 = null;
		try {
			myAddress1 = (Inet4Address) Inet4Address.getByName("192.168.1.100");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
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
		Long ret = (long) (Mothership.getAuctionTimeout() * 2 / 3); // This means 66.66% of AUCTION_TIMEOUT, may be changed
		synchronized (this) {
			DataStore.addAdvert(adv);
		}
		return ret;
	}

	@Override
	public void onRcvBid(Bid bid) {
		synchronized (this) {
			DataStore.addBid(bid);
		}
	}

	@Override
	public void onRcvBidWin(BidWin bidwin) {
		synchronized (this) {
			DataStore.addBidWin(bidwin);
		}
	}

	@Override
	public AuctionParameters onRcvData(Data packet) {
		AuctionParameters auctionParameters = null;
		synchronized (this) {
			int myBidWin = DataStore.myBidWins.get(packet.getTransactionID());
	
			int length = (int) DataStore.calculateTopologyDistance(DataStore.myAddress, packet.getFinalDestinationIP());
			int MAGIC_NUMBER = 10; // TODO THIS IS VERY VERY WRONG
			length += MAGIC_NUMBER - packet.getHopCount();
			if (length > 5)
				length = 5;
	
			int myFairProfit = packet.getInitialBudget() / length;
			int maxBid = myBidWin - myFairProfit;
			if(maxBid < myBidWin / 2)
				maxBid = myBidWin / 2;
	
			int fine = maxBid - 1;
			auctionParameters = new AuctionParameters(maxBid, fine);
			DataStore.addDataPacket(packet);
		}
		return auctionParameters;
	}

	@Override
	public Bid selectWinner(List<Bid> bids) {
		Bid ret = null;
		synchronized (this) {
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
					return bid;
				}
			}
		}
		return ret;
	}

	@Override
	public void onException(ManiacException ex, boolean fatal) {
	}

	@Override
	public Integer sendBid(Advert adv) {
		int myBid;
		synchronized (this) {
			int knownMin = adv.getCeil(); // This is the biggest possible bid
			ArrayList<Inet4Address> competition = DataStore.getCompetition(adv.getSourceIP());
			ArrayList<Bid> bidsPerAdvert = DataStore.bidsPerAdvert.get(adv.getTransactionID());
				
			if(bidsPerAdvert != null) {
				// Remove from competition list the nodes that have already bided and we have more accurate data
				for(Bid bid : bidsPerAdvert)
					competition.remove(bid.getSourceIP());
	
				// Get the smallest value from the existing bids
				for(Bid bid : bidsPerAdvert)
					if(knownMin > bid.getBid())
						knownMin = bid.getBid();
			}
			ArrayList<ArrayList<Behavior>> competitionBehavior = DataStore.getNodesBehavior(competition);
			int probabilisticMin = DataStore.createMyBid(competitionBehavior, adv);
	
			myBid = Math.min(probabilisticMin, knownMin) - 1;
		}
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
