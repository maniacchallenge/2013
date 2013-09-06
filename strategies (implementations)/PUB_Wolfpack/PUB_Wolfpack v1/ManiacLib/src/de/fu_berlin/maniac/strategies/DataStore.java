package de.fu_berlin.maniac.strategies;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import android.util.SparseArray;
import android.util.SparseIntArray;
import de.fu_berlin.maniac.packet_builder.*;
import de.fu_berlin.maniac.bank.BankManager;
import de.fu_berlin.maniac.network_manager.*;

public class DataStore {
	// TODO check this values
	static final int [] MERGE_WEIGHT = {40, 10, 40, 10};
	static final int MERGE_WEIGHT_SUM = 100;
	static final int LOG_SIZE = 300;
	
	static int maxMaxBid = 0;
	static int maxHopCount = 0;
	
	static public Inet4Address myAddress;  

	static private ArrayList<Advert> adverts = new ArrayList<Advert>();
	static private ArrayList<Data> dataPackets = new ArrayList<Data>();
	static public SparseIntArray myBidWins = new SparseIntArray();
	
	static private HashMap<Inet4Address, LinkedList<Behavior>> nodeBehaviors = new HashMap<Inet4Address, LinkedList<Behavior>>();
	static public SparseArray<ArrayList<Bid>> bidsPerAdvert = new SparseArray<ArrayList<Bid>>();
	static private ArrayList<PotentialProfit> potentialProfits = new ArrayList<PotentialProfit>();

	static private HashMap<Inet4Address, Long> richness = new HashMap<Inet4Address, Long>();
	static private HashMap<Inet4Address, Long> trustfulness = new HashMap<Inet4Address, Long>();
	

	static void addAdvert (Advert adv) {
		adverts.add(adv);
		bidsPerAdvert.put(adv.getTransactionID(), new ArrayList<Bid>());
	}

	static void addBid (Bid bid) {
		Random rand = new Random(System.currentTimeMillis());
		LinkedList<Behavior> behaviors;
		if(nodeBehaviors.containsKey(bid.getSourceIP())) {
			behaviors = nodeBehaviors.get(bid.getSourceIP());
		} else {
			behaviors = new LinkedList<Behavior>();
			nodeBehaviors.put(bid.getSourceIP(), behaviors);
		}
		
		ArrayList<Bid> aux = bidsPerAdvert.get(bid.getTransactionID());
		if(aux != null) {
			aux.add(bid);
			Advert advert = null;
			for(Advert adv : adverts)
				if(adv.getTransactionID() == bid.getTransactionID())
					advert = adv;
			behaviors.add(new Behavior(advert.getCeil(), bid.getBid(), advert.getDeadline()));
			if(maxMaxBid < advert.getCeil())
				maxMaxBid = advert.getCeil();
			if(maxHopCount < advert.getDeadline())
				maxHopCount = advert.getDeadline();
		} else {
			if(maxMaxBid < bid.getBid()) // this happens if we did not get the advert and it should reset maxMaxBid
				maxMaxBid = bid.getBid()+1;
			if(maxMaxBid <= 0)
				maxMaxBid = 1;
			if(maxHopCount <= 0)
				maxHopCount = 1;
			behaviors.add(new Behavior(rand.nextInt(maxMaxBid) , bid.getBid(), rand.nextInt(maxHopCount)));
		}
		if(behaviors.size() > LOG_SIZE) {
			Behavior behavior = behaviors.remove(0);
			if(behavior.maxBid == maxMaxBid)
				updateMax();
			if(behavior.hopCount == maxHopCount)
				updateMax();
		}
	}

	public static void updateMax() {
		maxHopCount = 0;
		maxMaxBid = 0;
		for(Entry<Inet4Address, LinkedList<Behavior>> el : nodeBehaviors.entrySet()) {
			for(Behavior b : el.getValue()) {
				if(maxMaxBid < b.maxBid)
					maxMaxBid = b.maxBid;
				if(maxHopCount < b.hopCount)
					maxHopCount = b.hopCount;
			}
		}
	}

	public static void addBidWin (BidWin bidWin) {
		// clear Advert data
		bidsPerAdvert.delete(bidWin.getTransactionID());
		Advert toRemove = null;
		for(Advert advert : adverts) {
			if(advert.getTransactionID() == bidWin.getTransactionID()) {
				toRemove = advert;
				break;
			}
		}
		adverts.remove(toRemove);
		
		// add richness to winner
		if(richness.containsKey(bidWin.getWinnerIP())) {
			Long value = richness.get(bidWin.getWinnerIP());
			value = value + bidWin.getWinningBid();
			richness.put(bidWin.getWinnerIP(), value);
		} else {
			richness.put(bidWin.getWinnerIP(), (long) (bidWin.getWinningBid()));
		}
		
		// remove richness from giver
		if(richness.containsKey(bidWin.getSourceIP())) {
			Long value = richness.get(bidWin.getSourceIP());
			value = value - bidWin.getWinningBid();
			richness.put(bidWin.getSourceIP(), value);
		} else {
			richness.put(bidWin.getSourceIP(), (long) - (bidWin.getWinningBid()));
		}
		
		if(bidWin.getWinnerIP().equals(myAddress)) {
			myBidWins.put(bidWin.getTransactionID(), bidWin.getWinningBid());
		}
	}

	public static ArrayList<NodeValue> createRichnessList(List<Bid> bids) {
		ArrayList<NodeValue> ret = new ArrayList<NodeValue>();
		for(Bid bid : bids) {
			Long nodeRichness = richness.get(bid.getSourceIP());
			if(nodeRichness == null)
				ret.add(new NodeValue(bid.getSourceIP(), 0));
			else
				ret.add(new NodeValue(bid.getSourceIP(), nodeRichness));
		}
		Collections.sort(ret, new CompareNodeValue());
		return ret;
	}

	public static ArrayList<NodeValue> createDistanceList(List<Bid> bids) {
		Inet4Address finalDestination = null;
		Data toRemove = null;
		for(Data d: dataPackets) {
			if(d.getTransactionID() == bids.get(0).getTransactionID()) {
				finalDestination = d.getFinalDestinationIP();
				toRemove = d;
				break;
			}
		}
		dataPackets.remove(toRemove);
		ArrayList<NodeValue> ret = new ArrayList<NodeValue>();
		for(Bid bid : bids)
			ret.add(new NodeValue(bid.getSourceIP(),calculateTopologyDistance(bid.getSourceIP(), finalDestination)));
		Collections.sort(ret, new CompareNodeValue());
		return ret;
	}
	
	public static ArrayList<NodeValue> createBidsList(List<Bid> bids) {
		ArrayList<NodeValue> ret = new ArrayList<NodeValue>();
		for(Bid bid : bids)
			ret.add(new NodeValue(bid.getSourceIP(), bid.getBid()));
		Collections.sort(ret, new CompareNodeValue());
		return ret;
	}
	
	public static ArrayList<NodeValue> createTrustfulnessList(List<Bid> bids) {
		ArrayList<NodeValue> ret = new ArrayList<NodeValue>();
		for(Bid bid : bids)
			ret.add(new NodeValue(bid.getSourceIP(), getTrustfulness(bid.getSourceIP())));
		Collections.sort(ret, new CompareNodeValue());
		return ret;
	}
	
	public static double getTrustfulness(Inet4Address sourceIP) {
		
		// update trustfulness
		ArrayList<PotentialProfit> remove = new ArrayList<PotentialProfit>();
		for(PotentialProfit el : potentialProfits) {
			Integer realProfit = BankManager.getAmount(el.transactionId);
			if(null != realProfit) {
				if(realProfit != el.potentialProfit) {
					Long currentTrust = trustfulness.get(el.nextHop);
					if(currentTrust == null)
						trustfulness.put(el.nextHop, (long)1);
					else
						trustfulness.put(el.nextHop, currentTrust+1);
				}
				remove.add(el);
			}
		}
		potentialProfits.removeAll(remove);
		
		Long ret = trustfulness.get(sourceIP);
		if (ret == null)
			return 0;
		return ret;
	}

	public static double calculateTopologyDistance(Inet4Address sourceIP, Inet4Address destinationIP) {
		ArrayDeque<NodeValue> distance = new ArrayDeque<NodeValue>();

		// get topology
		ArrayList<NodePair> topology = null;
		try {
			topology = TopologyInfo.getTopology();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		if (topology == null)
			return 1000;
		
		// initialize distance list
		distance.add(new NodeValue(sourceIP, 0));
		ArrayList<Inet4Address> visited = new ArrayList<Inet4Address>();

		// BF the topology, mark nodes as visited to not go in infinite loop
		while(!distance.isEmpty()) {
			NodeValue el = distance.pollFirst();
			visited.add(el.nodeName);
			for(NodePair np : topology) {
				Inet4Address nodeA = (Inet4Address) np.getDestinationIP();
				Inet4Address nodeB = (Inet4Address) np.getLastHopIP();
				if(nodeA.equals(el.nodeName) && !visited.contains(nodeB)) {
					if(nodeB.equals(destinationIP))
						return el.value+1;
					distance.add(new NodeValue(nodeB, el.value + 1));
				}
				if(nodeB.equals(el.nodeName) && !visited.contains(nodeA)) {
					if(nodeA.equals(destinationIP))
						return el.value+1;
					distance.add(new NodeValue(nodeA, el.value + 1));
				}
			}
		}

		return 1000; // bigger then any longest route
	}

	public static void addWinner(Bid bid) {
		int transactionId = bid.getTransactionID();
		potentialProfits.add(new PotentialProfit(bid.getSourceIP(), transactionId, myBidWins.get(transactionId) - bid.getBid()));
		myBidWins.delete(transactionId);
	}

	public static ArrayList<NodeValue> mergeLists(ArrayList<ArrayList<NodeValue>> allLists) {
		ArrayList<NodeValue> ret = new ArrayList<NodeValue>();
		for(NodeValue nv : allLists.get(0)) {
			ret.add(new NodeValue(nv.nodeName, 0));
		}
		
		for(int i=0; i<4; i++) {
			int k = 1;
			for(NodeValue nvToAdd : allLists.get(i)) {
				for(NodeValue nv : ret) {
					if(nv.nodeName.equals(nvToAdd.nodeName)) {
						nv.value += k * MERGE_WEIGHT[i];
					}
				}
				k++;
			}
		}

		for(NodeValue nv: ret)
			nv.value /= MERGE_WEIGHT_SUM;
		
		Collections.sort(ret, new CompareNodeValue());
		return ret;
	}

	public static void addDataPacket(Data packet) {
		dataPackets.add(packet);
	}

	public static ArrayList<Inet4Address> getCompetition(Inet4Address sourceIP) {
		ArrayList<Inet4Address> ret = new ArrayList<Inet4Address>();
		ArrayList<NodePair> topology = null;
		try {
			topology = TopologyInfo.getTopology();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		if(topology == null)
			return ret;
		for(NodePair np : topology) {
			Inet4Address nodeA = (Inet4Address) np.getDestinationIP();
			Inet4Address nodeB = (Inet4Address) np.getLastHopIP();
			if(nodeA.equals(sourceIP) && !nodeB.equals(myAddress))
				ret.add(nodeB);
			if(nodeB.equals(sourceIP) && !nodeA.equals(myAddress))
				ret.add(nodeA);
		}
		return ret;
	}

	public static ArrayList<ArrayList<Behavior>> getNodesBehavior(ArrayList<Inet4Address> competition) {
		ArrayList<ArrayList<Behavior>> ret = new ArrayList<ArrayList<Behavior>>();
		for(Inet4Address address : competition)
			// nodes behavior will be sorted so new lists need to be created
			ret.add(new ArrayList<Behavior>(nodeBehaviors.get(address)));
		return ret;
	}

	public static int createMyBid(ArrayList<ArrayList<Behavior>> competitionBehavior, Advert auxAdv) {
		final Advert adv = auxAdv;
		int min = Integer.MAX_VALUE;
		for(ArrayList<Behavior> behaviors : competitionBehavior) {
			Collections.sort(behaviors, new Comparator<Behavior>() {
				@Override
				public int compare(Behavior arg0, Behavior arg1) {
					// normalize everything
					double advX = (double)(adv.getCeil()) / (double)maxMaxBid;
					double advY = (double)(adv.getDeadline()) / (double)maxHopCount;
					double arg0X = (double)arg0.maxBid / (double) maxMaxBid;
					double arg0Y = (double)arg0.hopCount / (double) maxHopCount;
					double arg1X = (double)arg1.maxBid / (double) maxMaxBid;
					double arg1Y = (double)arg1.hopCount / (double) maxHopCount;
					
					// calculate distance from arg0/1 to adv
					double arg0Distance = Math.sqrt( Math.pow(advX - arg0X, 2) + Math.pow(advY - arg0Y, 2) );
					double arg1Distance = Math.sqrt( Math.pow(advX - arg1X, 2) + Math.pow(advY - arg1Y, 2) );

					return (int) (arg0Distance - arg1Distance);
			}});

			for(int i = 0; i < Math.min(LOG_SIZE/10, behaviors.size()); i++ ) {
				int bid = behaviors.get(i).bid;
				if(min > bid)
					min = bid;
			}
		}
		return min;
	}
}

class NodeValue {
	Inet4Address nodeName;
	double value;
	
	public NodeValue(Inet4Address nodeName, double value) {
		super();
		this.nodeName = nodeName;
		this.value = value;
	}
}

class PotentialProfit {
	Inet4Address nextHop;
	int transactionId;
	int potentialProfit;

	public PotentialProfit(Inet4Address nextHop, int transactionId, int potentialProfit) {
		this.nextHop = nextHop;
		this.transactionId = transactionId;
		this.potentialProfit = potentialProfit;
	}
}

class Behavior {
	int maxBid;
	int bid;
	int hopCount;
	
	public Behavior(int maxBid, int bid, int hopCount) {
		super();
		this.maxBid = maxBid;
		this.bid = bid;
		this.hopCount = hopCount;
	}
}

class CompareNodeValue implements Comparator<NodeValue> {
	@Override
	public int compare(NodeValue arg0, NodeValue arg1) {
		return (int) (arg0.value - arg1.value);
	}
}