package de.uni_bremen.comnets.maniac.dummies;

import java.net.Inet4Address;

import de.fu_berlin.maniac.packet_builder.Advert;

/**
 * Created by Isaac Supeene on 7/12/13.
 */
public class DummyAdvert extends Advert {
    public DummyAdvert(Inet4Address finalDest, int transactionID, int maxBid, int deadline, int fine, int initalBudget) {
        super(finalDest, transactionID, maxBid, deadline, fine, initalBudget);
    }

    @Override
    public void setSourceIP(Inet4Address ip) {
        super.setSourceIP(ip);
    }
}
