package de.fu_berlin.maniac.strategies;

import java.util.List;

import de.fu_berlin.maniac.exception.ManiacException;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.general.ManiacStrategyInterface;
import de.fu_berlin.maniac.network_manager.TopologyInfo;
import de.fu_berlin.maniac.packet_builder.*;
import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

public class slowstartStrategy implements ManiacStrategyInterface {

          Bid winner;
          // HashMap<Inet4Address, Integer> ip_trans = new HashMap<Inet4Address, Integer>();
          HashMap<Inet4Address, Integer> ip_price = new HashMap<Inet4Address, Integer>();
          HashMap<Integer, Inet4Address> trans_ip = new HashMap<Integer, Inet4Address>();
          HashMap<Integer, Integer> trans_price = new HashMap<Integer, Integer>();
          Vector openAuctions = new Vector();

          @Override
          public Long onRcvAdvert(Advert adv) {
	         if (!ip_price.containsKey(adv.getSourceIP())) {
		        ip_price.put(adv.getSourceIP(), 1);
	         }

	         trans_price.put(adv.getTransactionID(), adv.getCeil());
	         trans_ip.put(adv.getTransactionID(), adv.getSourceIP());
	         // ip_trans.put(adv.getSourceIP(), adv.getTransactionID());
	         openAuctions.add(adv.getTransactionID());

	         System.out.println("\t ip_pice:" + ip_price.toString());
	         System.out.println("\t trans_ip:" + trans_ip.toString());

	         return null;
          }

          @Override
          public void onRcvBid(Bid bid) {
	         // TODO Auto-generated method stub
          }

          @Override
          public void onRcvBidWin(BidWin bidwin) {
	         Inet4Address _t = trans_ip.get(bidwin.getTransactionID());
	         if (openAuctions.contains(bidwin.getTransactionID()) && !(bidwin.getWinnerIP().equals(TopologyInfo.getOwnIP()))) {
		        System.err.println(" --------------->---------------> I bid on: Transaction" + bidwin.getTransactionID() + " from: " + _t + " and didnt win!");
		        System.err.println(" --------------->---------------> Winner was: " + bidwin.getWinnerIP());
		        openAuctions.remove(new Integer(bidwin.getTransactionID()));

		        trans_ip.remove(bidwin.getTransactionID());
		        System.err.println(" --------------->---------------> remove: Transaction " + bidwin.getTransactionID());

		        //Inet4Address _t = getKeyByValue(ip_trans, bidwin.getTransactionID());
		        System.err.println(" --------------->---------------> I Lost: price is now: " + (int) (0.75 * ip_price.get(_t) + 1));
		        ip_price.put(_t, (int) (0.75 * ip_price.get(_t) + 1));

		        System.out.println("\t ip_pice:" + ip_price.toString());
		        System.out.println("\t trans_ip:" + trans_ip.toString());

	         }
          }

          @Override
          public AuctionParameters onRcvData(Data packet) {

	         // work around cause the source ip is null in the current version of the api - yawn 
	         openAuctions.remove(new Integer(packet.getTransactionID()));
	         Inet4Address _t = trans_ip.get(packet.getTransactionID());
	         trans_ip.remove(packet.getTransactionID());
	         int old_price = ip_price.get(_t);
	         int last_trans = trans_price.get(packet.getTransactionID());
	         int new_price = (old_price*2) > last_trans ? last_trans : (old_price*2);
	         System.err.println(" --------------->---------------> I WON: price is now: " + new_price);
	         ip_price.put(_t, new_price);
	         
	         return (new AuctionParameters((int)(0.17*last_trans), packet.getFine()));
	         
	         /*
	         System.err.println(" --------------->---------------> I WON: price is now: " + old_price * 2);
	         System.out.println("\t ip_pice:" + ip_price.toString());

	         // workaround: just double it
	         ip_price.put(_t, old_price * 2);


	         System.out.println("\t ip_pice:" + ip_price.toString());
	         System.out.println("\t trans_ip:" + trans_ip.toString());
	         int auction = (int) (old_price * 0.17);
	         System.out.println("\t auction price:" + auction);
	         return (new AuctionParameters(auction, packet.getFine()));
	         */
          }

          @Override
          public Bid selectWinner(List<Bid> bids) {
	         if (bids.isEmpty()) {
		        return null;
	         } else {
		        int lowest = Integer.MAX_VALUE;
		        int bidId = -1;
		        for (int i = 0; i < bids.size(); i++) {
			       if (bids.get(i).getBid() < lowest) {
				      lowest = bids.get(i).getBid();
				      bidId = i;
			       }
		        }

		        return bids.get(bidId);
	         }
          }

          @Override
          public void onException(ManiacException ex, boolean fatal) {
	         // TODO Auto-generated method stub
          }

          @Override
          public Integer sendBid(Advert adv) {
	         // i bid on that advert
	         System.out.println("  --------------->---------------> sendBid Price for Node:  " + adv.getSourceIP() + " is: " + ip_price.get(adv.getSourceIP()));
	         int myprice = ip_price.get(adv.getSourceIP());
	         if (myprice > adv.getCeil()) {
		        myprice = (int) (0.75 * adv.getCeil());
	         }
	         if (myprice <= 0) {
		        myprice = 1;
	         }
	         ip_price.put(adv.getSourceIP(), myprice);
	         System.err.println("Changed Price table: ");
	         System.out.println("\t ip_pice:" + ip_price.toString());
	         System.out.println("\t trans_ip:" + trans_ip.toString());
	         return ip_price.get(adv.getSourceIP());
          }

          @Override
          public boolean dropPacketBefore(Data buffer_data) {
	         // TODO Auto-generated method stub
	         return false;
          }

          @Override
          public boolean dropPacketAfter(Data buffer_data) {
	         // TODO Auto-generated method stub
	         return false;
          }

          private static <T, E> T getKeyByValue(Map<T, E> map, E value) {
	         for (Entry<T, E> entry : map.entrySet()) {
		        if (value.equals(entry.getValue())) {
			       return entry.getKey();
		        }
	         }
	         return null;
          }
}
