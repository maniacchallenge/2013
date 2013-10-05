package de.fu_berlin.maniac.strategies;

import java.util.List;

import de.fu_berlin.maniac.exception.ManiacException;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.general.ManiacStrategyInterface;
import de.fu_berlin.maniac.packet_builder.*;
import java.net.Inet4Address;
import java.util.HashMap;

public class cheapRandomStrategy implements ManiacStrategyInterface {

          Bid winner;
          HashMap<Integer, Integer> trans_price = new HashMap<Integer, Integer>();

          @Override
          public Long onRcvAdvert(Advert adv) {
	        trans_price.put(adv.getTransactionID(), adv.getCeil());
	        
	        return null;
          }

          @Override
          public void onRcvBid(Bid bid) {
	         // TODO Auto-generated method stub
          }

          @Override
          public void onRcvBidWin(BidWin bidwin) {
          }

          @Override
          public AuctionParameters onRcvData(Data packet) {
	         return (new AuctionParameters(trans_price.get(packet.getTransactionID()), packet.getFine()));
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
	         // send Random Value
	         return (int) (Math.random() * adv.getCeil() + 1);
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
}
