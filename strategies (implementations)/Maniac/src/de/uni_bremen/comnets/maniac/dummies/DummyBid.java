package de.uni_bremen.comnets.maniac.dummies;

import de.fu_berlin.maniac.exception.NegativeBidException;
import de.fu_berlin.maniac.packet_builder.Bid;

/**
 * Created by Isaac Supeene on 7/12/13.
 */
public class DummyBid extends Bid {
    public DummyBid(int transactionID, int bid) throws NegativeBidException {
        super(transactionID, bid);
    }
}
