package de.uni_bremen.comnets.maniac.dummies;

import java.net.Inet4Address;

import de.fu_berlin.maniac.packet_builder.BidWin;

/**
 * Created by Isaac Supeene on 7/13/13.
 */
public class DummyBidWin extends BidWin {
    public DummyBidWin(int transactionID, Inet4Address winnerIP, int winningBid, int fine) {
        super(transactionID, winnerIP, winningBid, fine);
    }

    @Override
    public void setSourceIP(Inet4Address ip) {
        super.setSourceIP(ip);
    }
}
