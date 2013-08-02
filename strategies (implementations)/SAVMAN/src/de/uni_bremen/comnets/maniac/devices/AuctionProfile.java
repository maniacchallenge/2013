package de.uni_bremen.comnets.maniac.devices;

import android.util.Log;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.Bid;
import de.uni_bremen.comnets.maniac.graphs.Connection;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;

/**
 * Created by Isaac Supeene on 7/11/13.
 */
public class AuctionProfile {
    private Device device;
    private AuctionProfileImpl implementation;
    private List<AuctionProfileImpl> possibleImplementations = new ArrayList<AuctionProfileImpl>();

    public AuctionProfile(Device device) {
        this.device = device;

        this.possibleImplementations.add(new RandomAuctionProfile());
        this.possibleImplementations.add(new SelectCheapestBid());
        this.possibleImplementations.add(new SelectCheapestBidInRange());
        this.possibleImplementations.add(new BackboneProfile());

        this.implementation = possibleImplementations.get(0);

        assert implementation != null;
    }

    public double getProbabilityOfChoosingOurBid(int bid, Advert advert, List<BiddingOpponent> biddingOpponents) {
        assert implementation != null;
        return implementation.getProbabilityOfChoosingOurBid(bid, advert, biddingOpponents);
    }

    public void update(Device.AuctionInfo info) {
        AuctionProfileImpl best = null;
        double bestValue = Double.MIN_VALUE;
        for (AuctionProfileImpl profile : possibleImplementations) {
            double newValue = profile.matchToDevice(info);
            if (newValue > bestValue) {
                best = profile;
                bestValue = newValue;
            }
        }
        implementation = best;
        assert implementation != null;
    }

    public String getProfileName() {
        return implementation.getProfileName();
    }
    public String getProfileDetails() {
        return implementation.getProfileDetails();
    }

    private interface AuctionProfileImpl {
        /**
         * Calibrates whatever internal parameters are used in this object to
         * predict the auction actions for this profile's device most accurately.
         *
         * The provided arguments represent an auction held by this node,
         * and the implementation may optionally use them for its calibration,
         * as an adjustment to its existing state, rather than calculating
         * some parameters anew every time using the entire history of the device.
         * This function will only receive complete AuctionInfos, and never
         * partial ones.
         *
         * @return The correlation coefficient of this profile's predictions when
         *         compared with real values for past events.
         */
        public double matchToDevice(Device.AuctionInfo info);
        public double getProbabilityOfChoosingOurBid(int bid, Advert advert, List<BiddingOpponent> biddingOpponents);

        public String getProfileName();
        public String getProfileDetails();
    }

    private class SelectCheapestBid implements AuctionProfileImpl {

        double cheapnessFactor = 0.5;

        @Override
        public double matchToDevice(Device.AuctionInfo info) {
            double cheapness = getCheapness(info);
            modifyCheapnessFactor(cheapness);
            if (cheapnessFactor < 0.0 || cheapnessFactor > 1.0) {
                Log.e("SelectCheapestBid", "Cheapness factor (" + cheapnessFactor + ") went out of the normal bounds!");
            }
            return cheapnessFactor;
        }

        protected double getCheapness(Device.AuctionInfo info) {
            return expensivenessRankOfWinningBid(info)/info.getBids().size();
        }

        private double expensivenessRankOfWinningBid(Device.AuctionInfo info) {
            int winningBid = info.getNextHopBid();
            int result = 1;
            for (Bid bid : info.getBids()) {
                if (bid.getBid() > winningBid) {
                    ++result;
                }
            }
            return result;
        }

        private void modifyCheapnessFactor(double cheapness) {
            cheapnessFactor = (2*cheapness + 3*cheapnessFactor)/5;
        }

        @Override
        public double getProbabilityOfChoosingOurBid(int bid, Advert advert, List<BiddingOpponent> biddingOpponents) {
            double ourProbableExpensivenessRank = 1;
            for (BiddingOpponent opponent : biddingOpponents) {
                ourProbableExpensivenessRank += opponent.getProbabilityOfHigherBidAsOpponent(advert, bid);
            }
            return Math.pow(cheapnessFactor, ourProbableExpensivenessRank);
        }

        @Override
        public String getProfileName() {
            return "Select Cheapest Bid Auction Profile";
        }

        @Override
        public String getProfileDetails() {
            return "Selects the cheapest bids with a probability of " + cheapnessFactor + "%";
        }
    }

    private class SelectCheapestBidInRange extends SelectCheapestBid {

        @Override
        public double getCheapness(Device.AuctionInfo info) {
            int winningBid = info.getNextHopBid();
            int result = 1;
            for (Bid bid : removeOutOfRangeBids(info.getBids(), info.getOriginalAdvert().getFinalDestinationIP(), info.getTransactionID(), info.getOriginalAdvert().getDeadline() - 2)) {
                if (bid.getBid() > winningBid) {
                    ++result;
                }
            }
            return result;
        }

        /**
         *
         * @param original
         * @param destination
         * @param transactionID
         * @param remainingHops The number of hops remaining WHEN THE NEXT NODE RECEIVES THE PACKET!!!
         * @return
         */
        private List<Bid> removeOutOfRangeBids(List<Bid> original, Inet4Address destination, int transactionID, int remainingHops) {
            List<Bid> result = new ArrayList<Bid>();
            for (Bid bid : original) {
                DijkstraShortestPath<Device, Connection> shortestPath = device.getTopologyAgent().getShortestPath(destination, transactionID, device.getTopologyAgent().getDevice(bid.getSourceIP()));
                synchronized (device.getTopologyAgent().topology) {
                    if (shortestPath.getPath(device.getTopologyAgent().getDevice(bid.getSourceIP()), device.getTopologyAgent().getDevice(destination)).size() <= remainingHops) {
                        result.add(bid);
                    }
                }
            }
            return result;
        }

        @Override
        public String getProfileName() {
            return "Select Cheapest Bid In Range Auction Profile";
        }

        @Override
        public String getProfileDetails() {
            return "Selects the cheapest bids that can deliver the packet normally within the deadline, with a probability of " + cheapnessFactor + "%";
        }
    }

    private class RandomAuctionProfile implements AuctionProfileImpl {

        @Override
        public double matchToDevice(Device.AuctionInfo info) {
            return 0.5;
        }

        @Override
        public double getProbabilityOfChoosingOurBid(int bid, Advert advert, List<BiddingOpponent> biddingOpponents) {
            return ((double)1)/(1 + biddingOpponents.size());
        }

        @Override
        public String getProfileName() {
            return "Random Auction Profile";
        }

        @Override
        public String getProfileDetails() {
            return "Selects an entirely random winner.";
        }
    }

    private class BackboneProfile implements AuctionProfileImpl {

        @Override
        public double matchToDevice(Device.AuctionInfo info) {
            if (device.isBackbone()) {
                return 1;
            }
            else {
                return 0;
            }
        }

        @Override
        public double getProbabilityOfChoosingOurBid(int bid, Advert advert, List<BiddingOpponent> biddingOpponents) {
            double result = 1;
            for (BiddingOpponent d : biddingOpponents) {
                result *= d.getProbabilityOfHigherBidAsOpponent(advert, bid);
            }
            return result;
        }

        @Override
        public String getProfileName() {
            return "Backbone Auction Profile";
        }

        @Override
        public String getProfileDetails() {
            return "This node is a backbone, and will always choose the lowest bid.";
        }
    }
}
