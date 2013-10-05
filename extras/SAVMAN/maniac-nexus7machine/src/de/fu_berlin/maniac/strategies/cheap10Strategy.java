package de.fu_berlin.maniac.strategies;

import java.util.List;

import de.fu_berlin.maniac.exception.ManiacException;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.general.ManiacStrategyInterface;
import de.fu_berlin.maniac.packet_builder.*;

public class cheap10Strategy implements ManiacStrategyInterface {

          Bid winner;

          @Override
          public Long onRcvAdvert(Advert adv) {
	         return null;
          }

          @Override
          public void onRcvBid(Bid bid) {
	         // TODO Auto-generated method stub
          }

          @Override
          public void onRcvBidWin(BidWin bidwin) {
	         // TODO Auto-generated method stub
          }

          @Override
          public AuctionParameters onRcvData(Data packet) {

	         return null;

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
	         // check out 10% of the packet price and send a number in that range
	         return (int) (Math.random() * (0.1 * adv.getCeil())+1);
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
