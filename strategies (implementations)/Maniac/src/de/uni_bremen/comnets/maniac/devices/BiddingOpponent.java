package de.uni_bremen.comnets.maniac.devices;

import de.fu_berlin.maniac.packet_builder.Advert;

/**
 * Created by Isaac Supeene on 7/3/13.
 */
public interface BiddingOpponent extends Bidder {
    public double getProbabilityOfHigherBidAsOpponent(Advert advert, int bid);
}
