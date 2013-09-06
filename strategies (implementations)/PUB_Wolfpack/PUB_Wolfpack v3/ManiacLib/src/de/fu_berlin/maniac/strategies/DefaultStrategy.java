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
		// TODO Auto-generated method stub
		return null;
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
