package de.fu_berlin.maniac.strategies;

import java.util.List;

import de.fu_berlin.maniac.exception.ManiacException;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.general.ManiacStrategyInterface;
import de.fu_berlin.maniac.packet_builder.*;

public class DefaultStrategy implements ManiacStrategyInterface {

	Bid winner;

	@Override
	public Long onRcvAdvert(Advert adv) {
		// dont wait
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
		if(bids.isEmpty()){
			return null;
		}
		return bids.get(0);
	}

	@Override
	public void onException(ManiacException ex, boolean fatal) {
		// TODO Auto-generated method stub

	}

	@Override
	public Integer sendBid(Advert adv) {
		// just bid something random
		double rand = Math.random() * adv.getCeil();
		return (int) rand;
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
