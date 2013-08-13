/**
 * 
 */
package de.fu_berlin.maniac.strategies;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import de.fu_berlin.maniac.exception.ManiacException;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.general.ManiacStrategyInterface;
import de.fu_berlin.maniac.network_manager.NetworkManager;
import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.Bid;
import de.fu_berlin.maniac.packet_builder.BidWin;
import de.fu_berlin.maniac.packet_builder.Data;

/**
 * @author asya
 *
 */
public class OffloadingRwthStrategy implements ManiacStrategyInterface {
	
	// constants 
	private static final double SMALLEST_BIT_RATIO = 0.9; // 70% from the budget of the auctioneer
	private static final int INDEX = 9;
	private static final String TEAM_MATE = "172.16.17.234";
	private static final String IP = "172.16.17.233";
	
	// number of hops left to the destination (according to the deadline defined by initial auctioneer) 
	private int pktTimeout;
	
	// the smallest distance between our node and the final destination node 
	private double bestDist;
	
	// list of all possible routes from our node to the final destination node
	// private List<de.fu_berlin.maniac.network_manager.Route> routeLinks = new ArrayList<de.fu_berlin.maniac.network_manager.Route>();
	
	// list of all possible links from our node 
	private List<de.fu_berlin.maniac.network_manager.Link> lstLinks = new ArrayList<de.fu_berlin.maniac.network_manager.Link>();
	
	// map which collects for each node (using its IP address) its bid to a corresponding budget (used in the prediction based on historic records)
	// private Map<InetAddress, Map<Integer, Integer>> rcvBidBudget = new HashMap<InetAddress, Map<Integer, Integer>>(); 
	
	// map for collecting all advertisements received to the node and the corresponding time in milliseconds 
	private Map<Advert,Long> rcvAdverts = new HashMap<Advert,Long>();
	
	// map which collects: advert transaction ID, budget of the corresponding advert
	//@SuppressLint("UseSparseArrays")
	//private Map<Integer,Integer> bidAdv = new HashMap<Integer,Integer>();
	
	// list for collecting all BidWin packets received to the node 
	private List<BidWin> winNodes = new ArrayList<BidWin>();
	
	// list for collecting all predicted balances transmitted by the team mate 
	private Map<BidWin,Integer> predictbalances = new HashMap<BidWin,Integer>();
	
	// map for collecting all bids received and the corresponding time in milliseconds 
	private Map<Bid,Long> rcvBids = new HashMap<Bid,Long>();
	
	// list for collecting all rcvBids (which are also later transmitted by the team mate)
	private Map<Bid, Integer> lstBids = new HashMap<Bid, Integer>();
	
	// save the current biding price level (1,...,10) for the regret learning scheme
	private int l = INDEX;
	
	// round index used in regret learning scheme 
	private int r = 1;
	
	private double[][] regretMtx = new double[10][10];
	
	private double[][] potentialMtx = new double[10][10];

	@Override
	public Long onRcvAdvert(Advert adv) {
		
		long currentTimeout = 1000;
		
		rcvAdverts.put(adv, System.currentTimeMillis());
		Log.i("OffloadingRwthStrategy", "onRcvAdvert is called");
		Log.i("OffloadingRwthStrategy", "advert ID: " + adv.getTransactionID() + ", sourceIP: " + adv.getSourceIP());
		Log.i("OffloadingRwthStrategy", "advert budget: " + adv.getCeil() + " advert fine: " + adv.getFine() + "\n" + "current time in ms: " + System.currentTimeMillis());
		return currentTimeout;
	}

	@Override
	public Integer sendBid(Advert adv) {
		
		Log.i("OffloadingRwthStrategy", "sendBid called");
		int sentBid;
		
		//int predBid; // a bid calculated for a current auction with the prediction based on historic records 
		int regretBid; // a bid calculated for a current auction with the regret learning scheme 
		
		//ArrayList<Bid> localBids = new ArrayList<Bid>();
		//ArrayList<Integer> finalBids = new ArrayList<Integer>();
		//int localSentBid; //bidToBackbone;
		
		pktTimeout = adv.getDeadline();
		bestDist = bestDistance();
		
		Log.i("OffloadingRwthStrategy", "packet timeout = " + pktTimeout);
		Log.i("OffloadingRwthStrategy", "best distance = " + bestDist);
		
		if(pktTimeout < bestDist) {
			sentBid = adv.getCeil();
			Log.i("OffloadingRwthStrategy", "pktTimeout < bestDist, sentBid = " + sentBid + "\n");
			
		} else {
					
			/*for (Map.Entry<Advert, Long> advert : rcvAdverts.entrySet()) {
				for (Map.Entry<Bid, Long> currentBid : rcvBids.entrySet()) {
					if(currentBid.getValue() - advert.getValue() <= 1900) {
						localBids.add(currentBid.getKey());
						Log.i("OffloadingRwthStrategy", "bid received before 1900ms = " + currentBid.getKey().getBid());
					}
				}
			}*/
			
			// prediction based on historic records
			Log.i("OffloadingRwthStrategy", "pktTimeout >= bestDist, prediction based on historic records is called" + "\n");
			//predBid = histRecordPred(adv, pktTimeout, bestDist);
			//finalBids.add(predBid);
			
			// regret learning scheme 
			Log.i("OffloadingRwthStrategy", "pktTimeout >= bestDist, regret learning scheme is called" + "\n");
			regretBid = regretLearning(adv);
			//finalBids.add(regretBid);
			
			/*if (!localBids.isEmpty()) {
				localSentBid = localBids.get(0).getBid();
				for(int i = 0; i < localBids.size(); i++) {
					if(localBids.get(i).getBid() < localSentBid) {
						localSentBid = localBids.get(i).getBid();
					}
				}
				bidToBackbone = localSentBid - 1;
				finalBids.add(bidToBackbone);
			
			} */
			
			/*int temp = finalBids.get(0);
			for(int i = 0; i < finalBids.size(); i++) {
				if(finalBids.get(i) < temp) {
					temp = finalBids.get(i);
				}
			}
			
			sentBid = temp;*/
			
			/*if(predBid < regretBid) {
				localSentBid = predBid;
				Log.i("OffloadingRwthStrategy", "predBid < regretBid, sentBid = " + localSentBid);
			} else {
				localSentBid = regretBid;
				Log.i("OffloadingRwthStrategy", "predBid > regretBid, sentBid = " + localSentBid + "\n");
			}*/
			if (regretBid > ((int) (0.4 * adv.getCeil())) + 1) {
				sentBid = regretBid;
			} else {
				sentBid = (int) (0.4 * adv.getCeil()) + 1;
			}
		}
		Log.i("OffloadingRwthStrategy", "at the end of sendBid function, sentBid = " + sentBid);
		
		return sentBid;
	}

	@SuppressLint("UseSparseArrays")
	@Override
	public void onRcvBid(Bid bid) {
		
		Log.i("OffloadingRwthStrategy", "onRcvBid is called");
		
		rcvBids.put(bid, System.currentTimeMillis());
		
		if(rcvAdverts.isEmpty()){
			Log.i("OffloadingRwthStrategy", "rcvAdverts is EMPTY");
		} else {
			for (Map.Entry<Advert, Long> advert : rcvAdverts.entrySet()) {
				for (Map.Entry<Bid, Long> currentBid : rcvBids.entrySet()) {
					if((currentBid.getValue() - advert.getValue() <= 3000) && currentBid.getKey().getBid() > (int) (0.2 * advert.getKey().getCeil())) {
						lstBids.put(bid, advert.getKey().getCeil());
						//rcvLstBids = new RcvBids(currentBid.getKey().getTransactionID(), null, currentBid.getKey().getSourceIP(), currentBid.getKey().getBid(), advert.getKey().getCeil());
						//lstBids.add(rcvLstBids);
						Log.i("OffloadingRwthStrategy", "for a node " + currentBid.getKey().getSourceIP().toString() + ": bid = " + currentBid.getKey().getBid() + ", budget = " + advert.getKey().getCeil() + " are added");
					}
				}
			}
		}
	}

	@SuppressLint("UseSparseArrays")
	@Override
	public void onRcvBidWin(BidWin bidwin) {
		
		double revenue;
		bestDist = bestDistance();
			
		winNodes.add(bidwin);
		
		/*try {
			routeLinks = de.fu_berlin.maniac.network_manager.TopologyInfo.getRoutes();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		bestDist = routeLinks.get(0).getLinkQuality();
		for(int i = 0; i < routeLinks.size(); i++) {
			if(routeLinks.get(i).getLinkQuality() < bestDist) {
				bestDist = routeLinks.get(i).getLinkQuality();
			}
		}*/
		
		if(pktTimeout < bestDist){
			revenue = (bidwin.getWinningBid() * (double) (pktTimeout/bestDist)) - (bidwin.getFine() * (1 - (double) (pktTimeout/bestDist)));
		} else {
			revenue = bidwin.getWinningBid();
		}
		
		predictbalances.put(bidwin, (int) revenue);
		
		//Pair predictBlnc = new Pair(bidwin.getTransactionID(), null, bidwin.getWinnerIP(), (int) revenue);
		Log.i("OffloadingRwthStrategy", "saved in PAIR packet: ID = " + bidwin.getTransactionID() + " winner = " + bidwin.getWinnerIP() + " revenue = " + (int) revenue);
		//predictbalances.add(predictBlnc);
		
		// only for testing purposes
		//for (int i = 0; i < winNodes.size(); i++) {
			//Log.i("OffloadingRwthStrategy", "saved bidwin IP = " + winNodes.get(i).getWinnerIP() + ", transID = "+ winNodes.get(i).getTransactionID()); 
			//}
			
	}

	@SuppressWarnings("unused")
	@Override
	public AuctionParameters onRcvData(Data packet) {
		
		Log.i("OffloadingRwthStrategy", "onRcvData is called");
		Log.i("OffloadingRwthStrategy", "we are the auctioneer, onRcvData is called");
		
		long time = System.currentTimeMillis();
		int newFine;
		int newMaxBid;
		int lastBid = 0;
		pktTimeout = packet.getHopCount();
		bestDist = bestDistance();
		
		for (Map.Entry<Bid,Long> currBid : rcvBids.entrySet()) {
			if (currBid.getKey().getSourceIP().toString() == IP && (System.currentTimeMillis() - currBid.getValue() <= 3000) ) {
				lastBid = currBid.getKey().getBid();
			}
		}
		/*for (Map.Entry<Advert,Long> advert : rcvAdverts.entrySet()) {
			// for this verification we need a new API version with fixed getIP functions
			if (advert.getKey().getTransactionID() == packet.getTransactionID() && advert.getKey().getSourceIP() == packet.getSourceIP()) {
				lastBudget = advert.getKey().getCeil();
				//Log.i("OffloadingRwthStrategy", "last advert's budget = " + lastBudget);
				break;
			}
		}
		Log.i("OffloadingRwthStrategy", "our current auctioneer budget = " + lastBudget);*/
		
		//for(Map.Entry<Integer, Integer> bidAdvert : bidAdv.entrySet()) {
			//if(bidAdvert.getKey() == packet.getTransactionID()) {
				//lastBudget = bidAdvert.getValue();
				//break;
			//}
		//}
		
		if(pktTimeout >= bestDist){
			newMaxBid = (int)((lastBid * (bestDist/ (double) pktTimeout)));
				//newMaxBid = (int) (lastBudget * (packet.getHopCount()*8));
				Log.i("OffloadingRwthStrategy", "pktTimeout >= bestDist, min[(lastBudget * (packet.getHopCount()*8))], newMaxBid = " + newMaxBid);
			/*} else {
				newMaxBid = (int) (lastBudget * 0.8);
				Log.i("OffloadingRwthStrategy", "pktTimeout >= bestDist, min[(lastBudget * 0.8)], newMaxBid = " + newMaxBid);
			}*/
		} else {
			newMaxBid = lastBid;
			Log.i("OffloadingRwthStrategy", "pktTimeout < bestDist, newMaxBid = " + newMaxBid);
		}
		
		newFine = newMaxBid - 1;
		
		Log.i("OffloadingRwthStrategy", "newFine = " + newFine);
		
		if(pktTimeout < bestDist) {
			newFine = newMaxBid;
			Log.i("OffloadingRwthStrategy", "pktTimeout < bestDist, newFine = " + newFine);
		}
		
		AuctionParameters currentAuctionParam = new AuctionParameters(newMaxBid, newFine);
		
		return currentAuctionParam;
	}

	@Override
	public Bid selectWinner(List<Bid> bids) {
		
		Log.i("OffloadingRwthStrategy", "selectWinner is called");
		ArrayList<InetAddress> backbones = new ArrayList<InetAddress>();
		Inet4Address finalDest = null;
		boolean foundFinalDestination = false;
		
		Bid bidWinner = bids.get(0);
		Inet4Address neighborIP = sendNeighborIP();
		bestDist = bestDistance();
		backbones = NetworkManager.getInstance().getBackbones();
		
		for (int i = 0; i < backbones.size(); i++) {
			if(neighborIP.equals(backbones.get(i)) && neighborIP != null) {
				foundFinalDestination = true;
				break;
			}
		}
		
			if(pktTimeout >= bestDist + 1) {
				for(int i = 0; i < bids.size(); i++) {
				
					// check if one of the bidders is our team mate and select it as bid winner
					if(neighborIP.equals(bids.get(i).getSourceIP()) && neighborIP != null) {
						bidWinner = bids.get(i);
						break;
					}
				
					if(bids.get(i).getBid() < bidWinner.getBid()) {
						bidWinner = bids.get(i);
					}
				}
				Log.i("OffloadingRwthStrategy", "pktTimeout >= bestDist + 1, bidWinner = " + bidWinner);
			} else if(pktTimeout == bestDist) {
				for(int i = 0; i < bids.size(); i++) {
					
					// check if one of the bidders is our team mate and select it as bid winner
					if(neighborIP.equals(bids.get(i).getSourceIP()) && neighborIP != null) {
						bidWinner = bids.get(i);
						break;
					}
				
					if(bids.get(i).getBid() < bidWinner.getBid()) {
						bidWinner = bids.get(i);
					}
				}
				//bidWinner = null;
				Log.i("OffloadingRwthStrategy", "pktTimeout == bestDist, bidWinner = " + bidWinner);
			} else if(pktTimeout < bestDist) {
				if(foundFinalDestination) {
					for (int i = 0; i < bids.size(); i++) {
						for (Map.Entry<Advert,Long> adv : rcvAdverts.entrySet()) {
							if(adv.getKey().getTransactionID() == bids.get(i).getTransactionID()) {
								finalDest = adv.getKey().getFinalDestinationIP();
								break;
							}
						}
						if(finalDest != null) {
							break;
						}
					}
				
					for (int i = 0; i < backbones.size(); i++) {
						if(neighborIP.equals(backbones.get(i)) && neighborIP != null) {
							if (neighborIP.equals(finalDest)) {
								bidWinner = null;
							}
						}
					}
				}
			}
			Log.i("OffloadingRwthStrategy", "pktTimeout < bestDist, bidWinner = " + bidWinner);
			
		return bidWinner;
		}

	@Override
	public boolean dropPacketBefore(Data buffer_data) {
		
		/*boolean smallTimeout = false;
		Map<Advert,Long> localAdv = new HashMap<Advert,Long>();
		int lastBudget = 0;
		pktTimeout = buffer_data.getHopCount();
		bestDist = bestDistance();
		long time = 0;
		
		for (Map.Entry<Advert,Long> adv : rcvAdverts.entrySet()) {
			time = adv.getValue();
			if(adv.getKey().getTransactionID() == buffer_data.getTransactionID()) {
				localAdv.put(adv.getKey(), adv.getValue());
			}
		}
		
		for(Map.Entry<Advert,Long> adv1 : localAdv.entrySet()) {
			if(adv1.getValue() > time)
				time = adv1.getValue();
		}
		for (Map.Entry<Advert,Long> adv : rcvAdverts.entrySet()) {
			if(time == adv.getValue())
				lastBudget = adv.getKey().getCeil();
		}
		
		if(pktTimeout < bestDist || ((lastBudget < ((int) (0.1 * buffer_data.getInitialBudget()) + 1) && lastBudget != 0))) {
			smallTimeout = true;
		}
		
		return smallTimeout; */
		return false;
	}

	@Override
	public boolean dropPacketAfter(Data buffer_data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onException(ManiacException ex, boolean fatal) {
		// TODO Auto-generated method stub
		
	}
	
	private Inet4Address sendNeighborIP() {
		
		Inet4Address neighborIP = null;
		bestDist = bestDistance();
		
		try {
			lstLinks = de.fu_berlin.maniac.network_manager.TopologyInfo.getLinks();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		for(int i = 0; i < lstLinks.size(); i++) {
			Log.i("OffloadingRwthStrategy", "linkIps, neighborIP = " + lstLinks.get(i).getIp());
			if(lstLinks.get(i).getIp().getHostAddress().equals(TEAM_MATE)) {
				neighborIP = (Inet4Address) lstLinks.get(i).getIp();
				break;
			}
		}
		Log.i("OffloadingRwthStrategy", "sendNeighborIP is implemented, neighborIP = " + neighborIP);
		
		return neighborIP;
	}
	
	private double bestDistance() {
		
		List<de.fu_berlin.maniac.network_manager.Route> routeLinks = new ArrayList<de.fu_berlin.maniac.network_manager.Route>();
		
		try {
			routeLinks = de.fu_berlin.maniac.network_manager.TopologyInfo.getRoutes();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		bestDist = routeLinks.get(0).getLinkQuality();
		for(int i = 0; i < routeLinks.size(); i++) {
			if(routeLinks.get(i).getLinkQuality() < bestDist) {
				bestDist = routeLinks.get(i).getLinkQuality();
			}
		}
		
		return bestDist;
	}
	
	// implement prediction based on historic records
	private int histRecordPred(Advert adv, int timeout, double distance) {
		
		int n;
		int smallestBidRatio, bidRatioValue;
		ArrayList<Integer> bidRatios = new ArrayList<Integer>();
		boolean won = false;
		
		Set<Inet4Address> nodesIP = new HashSet<Inet4Address>();
		
		// variables used to implement line regression
		double slope, intercept;
		int bidBudgetMultiply; // bidMudgetMultiply = bid * budget
		int bidsSum; 
		int budgetSum;
		int bidSquare;
		
		int bidToSend = 0;
		
		if(lstBids.isEmpty()) {
			smallestBidRatio = (int) (SMALLEST_BIT_RATIO * adv.getCeil());
			Log.i("OffloadingRwthStrategy", "initial start of the prediction algorithm, bidratio = " + smallestBidRatio);
		} else {
			
			for (Map.Entry<Bid, Integer> bid : lstBids.entrySet()) {
				nodesIP.add(bid.getKey().getSourceIP());
			}
			
			// start line regression
			for (Inet4Address nodeIP : nodesIP) {
			  if(nodeIP.toString() != TEAM_MATE) {
				bidBudgetMultiply = 0; // bidMudgetMultiply = bid * budget
				bidsSum = 0; 
				budgetSum = 0;
				bidSquare = 0;
				for (Map.Entry<Bid, Integer> bid : lstBids.entrySet()) {
					if(nodeIP.equals(bid.getKey().getSourceIP())) {
						bidBudgetMultiply =+ bid.getKey().getBid() * bid.getValue();
						bidsSum =+ bid.getKey().getBid();
						budgetSum =+ bid.getValue();
						bidSquare =+ bid.getKey().getBid();
					}
				}
				
				Log.i("OffloadingRwthStrategy", "bidBudgetMultiply = " + bidBudgetMultiply);
				Log.i("OffloadingRwthStrategy", "bidsSum = " + bidsSum);
				Log.i("OffloadingRwthStrategy", "budgetSum = " + budgetSum);
				Log.i("OffloadingRwthStrategy", "bidSquare = " + bidSquare);
			
				slope = ((lstBids.size() * bidBudgetMultiply) - (bidsSum * budgetSum)) / (double) ((lstBids.size() * bidSquare) - (bidsSum * bidsSum));
				Log.i("OffloadingRwthStrategy", "slope = " + slope);
				intercept = (budgetSum - (slope * bidsSum)) / lstBids.size();
				Log.i("OffloadingRwthStrategy", "intercept = " + intercept);
				bidRatioValue = (int) ((int) intercept + (slope * adv.getCeil()));
				Log.i("OffloadingRwthStrategy", "bidRatioValue = " + bidRatioValue + "\n");
				bidRatios.add(bidRatioValue);
			  }
			}
		
		smallestBidRatio = bidRatios.get(0);
		for(Integer bratio : bidRatios) {
			if(bratio < smallestBidRatio) {
				smallestBidRatio = bratio;
			}
		}
		bidRatios.clear();
		}
		Log.i("OffloadingRwthStrategy", "smallest bidratio found in the list = " + smallestBidRatio);
		
		n = (int) (Math.log(smallestBidRatio)/(double) Math.log((double) (distance/timeout)));
		//if(Double.valueOf((Math.log(smallestBidRatio)/Math.log((double) timeout/distance))).equals(n)) {
			//n = (int) (Math.log(smallestBidRatio)/Math.log((double) timeout/distance) - 1);
		//}
		Log.i("OffloadingRwthStrategy", "n = " + n);
		
		if(winNodes.isEmpty()) {
			bidToSend = (int) (adv.getCeil() * Math.pow(timeout/distance, n));
			Log.i("OffloadingRwthStrategy", "we didn't receive any BidWin packet, n = " + n + ", bidToSend = " + bidToSend);
		} else {
			for(int i = 0; i < winNodes.size(); i++) {
				if((winNodes.get(i).getTransactionID() == adv.getTransactionID() - 1) && (i == winNodes.size() - 1)) {
					bidToSend = (int) (adv.getCeil() * Math.pow(timeout/distance, n));
					won = true;
					Log.i("OffloadingRwthStrategy", "we won last auction, n = " + n + ", bidToSend = " + bidToSend);
					break;
				}
			}
		}
		
			if(won == false) {
				bidToSend = (int) (adv.getCeil() * Math.pow(timeout/distance, n+1));
				Log.i("OffloadingRwthStrategy", "we did not win the last auction, n = " + (n+1) + ", bidToSend = " + bidToSend);
			}
		
	
		return bidToSend;
	}
	
	@SuppressWarnings("unused")
	private int regretLearning(Advert adv) {
		
		int bitToSend  = 0;
		double bidLevel;
		double middleRegretMtx[][] = regretMtx;
		List<Integer> bidLevels = new ArrayList<Integer>(10);
		boolean won = false;
		
		for(int i = 1; i <= 10; i++) {
			bidLevel = adv.getCeil() * (i / 10f);
			Log.i("OffloadingRwthStrategy", "(adv.budget * (i/10)) =" + bidLevel);
			bidLevels.add((int) (bidLevel));
		}

		if(winNodes.isEmpty()) {
			bitToSend = bidLevels.get(l);
			Log.i("OffloadingRwthStrategy", "regretLearningScheme, bidLevel(4) = " + bitToSend);
		} else {
			for(int k = 0; k < winNodes.size(); k++) {
				if((winNodes.get(k).getTransactionID() == adv.getTransactionID() - 1) && (k == winNodes.size() - 1)) {
						
					won = true;
					// compute the potential matrix
					for(int i = 0; i < potentialMtx.length; i++) {
						for(int j = 0; j < potentialMtx.length; j++) {
							if(i == l) {
								if(j > l) {
									potentialMtx[i][j] = j - l;
								} else {
									potentialMtx[i][j] = 0;
								}
							}
						}		
					}
				
					// compute the first part of the equation resulting the new regret matrix
					for(int i = 0; i < regretMtx.length; i++) {
						for(int j = 0; j < regretMtx.length; j++) {
							middleRegretMtx[i][j] = (1 - (1.0/(double)(r + 1))) * regretMtx[i][j]; 
						}
					}
				
					// compute the new regret matrix
					for(int i = 0; i < regretMtx.length; i++) {
						for(int j = 0; j < regretMtx.length; j++) {
							regretMtx[i][j] = middleRegretMtx[i][j] + potentialMtx[i][j];
						}
					}
				
					// find the biggest regret in the regret matrix
					double biggestRgt = regretMtx[0][0];
					for(int i = 0; i < regretMtx.length; i++) {
						for(int j = 0; j < regretMtx.length; j++) {
							if(regretMtx[i][j] > biggestRgt) {
								biggestRgt = regretMtx[i][j];
								l = j;
								Log.i("OffloadingRwthStrategy", "we won the last auction, l = " + l);
							}
						}
					}
							
					bitToSend = bidLevels.get(l);
					r++;
					Log.i("OffloadingRwthStrategy", "we won the last auction, l = " + l + ", r = " + r + ", bitToSend = " + bitToSend);
					break;
					}
				}
			} 
		
			if (won == false) {
			
			// compute the potential matrix
			for(int i = 0; i < potentialMtx.length; i++) {
				for(int j = 0; j < potentialMtx.length; j++) {
					if(i == l) {
						if(j < l) {
							potentialMtx[i][j] = j;
						} else {
							potentialMtx[i][j] = 0;
						}
					}
				}
			}
			
			// compute the first part of the equation resulting the new regret matrix
			for(int i = 0; i < regretMtx.length; i++) {
				for(int j = 0; j < regretMtx.length; j++) {
					middleRegretMtx[i][j] = (1 - (1.0/(double)(r + 1))) * regretMtx[i][j]; 
				}
			}
			
			// compute the new regret matrix
			for(int i = 0; i < regretMtx.length; i++) {
				for(int j = 0; j < regretMtx.length; j++) {
					regretMtx[i][j] = middleRegretMtx[i][j] + potentialMtx[i][j];
				}
			}
			
			// find the biggest regret in the regret matrix
			double biggestRgt = regretMtx[0][0];
			for(int i = 0; i < regretMtx.length; i++) {
				for(int j = 0; j < regretMtx.length; j++) {
					if(regretMtx[i][j] > biggestRgt) {
						biggestRgt = regretMtx[i][j];
						l = j;
						Log.i("OffloadingRwthStrategy", "we lose the auction, l = " + l);
					}
				}
			}
			
			bitToSend = bidLevels.get(l);
			r++;
			Log.i("OffloadingRwthStrategy", "we lose the auction, l = " + l + ", r = " + r + ", bitToSend = " + bitToSend);
		} 
		
		return bitToSend;
	}

}
