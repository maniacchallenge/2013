package de.uni_bremen.comnets.maniac.devices;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.fu_berlin.maniac.packet_builder.Advert;
import de.uni_bremen.comnets.maniac.util.Function2Var;

/**
 * Created by Isaac Supeene on 7/3/13.
 */
public class BiddingProfile {

    private Device device;
    private BiddingProfileImpl implementation;
    private List<BiddingProfileImpl> possibleImplementations = new ArrayList<BiddingProfileImpl>();

    public BiddingProfile(Device device) {
        this.device = device;

        this.possibleImplementations.add(new SimpleBiddingProfile());
        this.possibleImplementations.add(new BackboneProfile());

        this.implementation = new BackboneProfile();

        assert implementation != null;
    }

    public NormalDistribution getBiddingDistribution(int hopsRemaining, Inet4Address destination, int budget, int fine, int transactionID) {
        assert implementation != null;
        return implementation.getBiddingDistribution(hopsRemaining, destination, budget, fine, transactionID);
    }

    public Function2Var<Integer, Integer, Integer> getMostProbableBidFunction(int hopsRemaining, Inet4Address destination, int transactionID) {
        assert implementation != null;
        return implementation.getMostProbableBidFunction(hopsRemaining, destination, transactionID);
    }

    public Function2Var<Integer, Integer, Double> getProbabilityOfNoBidFunction(int hopsRemaining, Inet4Address destination, int transactionID) {
        assert implementation != null;
        return implementation.getProbabilityOfNoBidFunction(hopsRemaining, destination, transactionID);
    }

    /**
     * To be called when the node represented by this profile bids on an advert.
     * The profile will be updated to reflect the new bid.
     * @param advert
     * @param bid
     */
    public void update(Advert advert, int bid) {
        if (device.isBackbone())
            return;

        BiddingProfileImpl best = possibleImplementations.get(0);
        double bestValue = Double.MIN_VALUE;
        for (BiddingProfileImpl profile : possibleImplementations) {
            double newValue = profile.matchToDevice(advert, bid);
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

    private interface BiddingProfileImpl {
        /**
         * Calibrates whatever internal parameters are used in this object to
         * predict the bidding actions for this profile's device most accurately.
         *
         * The provided arguments represent the advert and bid which triggered
         * this update, and the implementation may optionally use them for its
         * calibration, as an adjustment to its existing state, rather than
         * calculating some parameters anew every time using the entire history
         * of the device.
         *
         * @return A measure of the accuracy of this profile's predictions once it
         *         has been calibrated
         */
        public double matchToDevice(Advert advert, int bid);

        /**
         * @param hopsRemaining The number of hops that will be remaining WHEN THE NODE RECEIVES THE DATA.
         *                      In other words, the number of hops it can use to deliver the packet.
         * @param destination The final destination of the packet.
         * @param budget The max bid for this packet.
         * @param fine The fine on this packet.
         * @return A distribution representing the probability of this node providing a particular bid.
         */
        public NormalDistribution getBiddingDistribution(int hopsRemaining, Inet4Address destination, int budget, int fine, int transactionID);

        /**
         *
         * @param hopsRemaining The number of hops that will be remaining WHEN THE NODE RECEIVES THE DATA.
         *                      In other words, the number of hops it can use to deliver the packet.
         * @param destination The final destination of the packet.
         * @return A function from the budget and fine for an auction to the most probable bid on that auction for this node.
         */
        public Function2Var<Integer, Integer, Integer> getMostProbableBidFunction(int hopsRemaining, Inet4Address destination, int transactionID);

        /**
         *
         * @param hopsRemaining The number of hops that will be remaining WHEN THE NODE RECEIVES THE DATA.
         *                      In other words, the number of hops it can use to deliver the packet.
         * @param destination The final destination of the packet.
         * @return A function from the budget and fine for an auction to the probability that this node will avoid bidding for this packet.
         */
        public Function2Var<Integer, Integer, Double> getProbabilityOfNoBidFunction(int hopsRemaining, Inet4Address destination, int transactionID);

        public String getProfileName();
        public String getProfileDetails();
    }

    private class SimpleBiddingProfile implements BiddingProfileImpl {

        List<Double> fractions = new ArrayList<Double>();
        double fraction = 0.5;
        double match = 0.5;

        @Override
        public double matchToDevice(Advert advert, int bid) {
            if (device.getTopologyAgent().getLeastNumberOfHops(device.getAddress(), advert.getFinalDestinationIP(), advert.getTransactionID()) <= advert.getDeadline()) {
                return match;
            }

            fractions.add(((double)bid)/advert.getCeil());
            fraction = getAverageFraction();

            List<Double> fractionalErrors = new ArrayList<Double>();
            for (Double d : fractions) {
                fractionalErrors.add((d - fraction)/fraction);
            }
            fractionalErrors = removeWorstOutliers(fractionalErrors);

            match = 1.0;
            for (Double d : fractionalErrors) {
                match *= (1.0/(d + 1));
            }
            return match;
        }

        private double getAverageFraction() {
            double sum = 0;
            for (Double d : fractions) {
                sum += d;
            }
            return sum/fractions.size();
        }

        private List<Double> removeWorstOutliers(List<Double> fractionalErrors) {
            Collections.sort(fractionalErrors, new Comparator<Double>() {
                @Override
                public int compare(Double d1, Double d2) {
                    return (int)(Math.abs(d1) - Math.abs(d2));
                }
            });
            int numberToRemove = fractionalErrors.size()/10;
            return fractionalErrors.subList(0, fractionalErrors.size() - numberToRemove);
        }

        @Override
        public NormalDistribution getBiddingDistribution(int hopsRemaining, Inet4Address destination, int budget, int fine, int transactionID) {
            if (device.getTopologyAgent().getLeastNumberOfHops(device.getAddress(), destination, transactionID) >= hopsRemaining) {
                return new NormalDistribution(budget * fraction, budget*match);
            }
            else {
                return new NormalDistribution(budget, 0.1);
            }
        }

        @Override
        public Function2Var<Integer, Integer, Integer> getMostProbableBidFunction(int hopsRemaining, Inet4Address destination, int transactionID) {
            return getMostProbableBidFunctionImpl(hopsRemaining, device.getTopologyAgent().getLeastNumberOfHops(device.getAddress(), destination, transactionID));
        }

        protected Function2Var<Integer, Integer, Integer> getMostProbableBidFunctionImpl(int hopsRemaining, int hopsToDestination) {
            if (hopsToDestination >= hopsRemaining) {
                return new Function2Var<Integer, Integer, Integer>() {
                    @Override
                    public Integer evaluate(Integer budget, Integer fine) {
                        return (int)(budget * fraction);
                    }
                };
            }
            else {
                return new Function2Var<Integer, Integer, Integer>() {
                    @Override
                    public Integer evaluate(Integer budget, Integer fine) {
                        return budget;
                    }
                };
            }
        }

        @Override
        public Function2Var<Integer, Integer, Double> getProbabilityOfNoBidFunction(int hopsRemaining, Inet4Address destination, int transactionID) {
            if (device.getTopologyAgent().getLeastNumberOfHops(device.getAddress(), destination, transactionID) >= hopsRemaining) {
                return new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer budget, Integer fine) {
                    return 0.0;
                }
            };
            }
            else {
                return  new Function2Var<Integer, Integer, Double>() {
                    @Override
                    public Double evaluate(Integer budget, Integer fine) {
                        return 1.0;
                    }
                };
            }
        }

        @Override
        public String getProfileName() {
            return "Simple Bidding Profile";
        }

        @Override
        public String getProfileDetails() {
            return "Typically bids the same fraction of the budget.\nFraction: " + fraction;
        }
    }

    private class BackboneProfile implements BiddingProfileImpl {

        @Override
        public double matchToDevice(Advert advert, int bid) {
            if (device.isBackbone()) {
                return 1;
            }
            else {
                return 0;
            }
        }

        @Override
        public NormalDistribution getBiddingDistribution(int hopsRemaining, Inet4Address destination, int budget, int fine, int transactionID) {
            return new NormalDistribution(0, 1); // Arbitrary - doesn't matter anyway.
        }

        @Override
        public Function2Var<Integer, Integer, Integer> getMostProbableBidFunction(int hopsRemaining, Inet4Address destination, int transactionID) {
            return new Function2Var<Integer, Integer, Integer>() {
                @Override
                public Integer evaluate(Integer x, Integer y) {
                    return 0;
                }
            };
        }

        @Override
        public Function2Var<Integer, Integer, Double> getProbabilityOfNoBidFunction(int hopsRemaining, Inet4Address destination, int transactionID) {
            return new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer x, Integer y) {
                    return 1.0;
                }
            };
        }

        @Override
        public String getProfileName() {
            return "Backbone Bidding Profile";
        }

        @Override
        public String getProfileDetails() {
            return "This node is a backbone, and will never bid on any auction.";
        }
    }
}