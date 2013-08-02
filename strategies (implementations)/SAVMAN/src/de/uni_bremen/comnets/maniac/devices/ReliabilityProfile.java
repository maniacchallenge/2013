package de.uni_bremen.comnets.maniac.devices;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

import de.fu_berlin.maniac.packet_builder.Advert;
import de.uni_bremen.comnets.maniac.agents.HistoryAgent;
import de.uni_bremen.comnets.maniac.graphs.Connection;
import de.uni_bremen.comnets.maniac.util.Function2Var;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;

/**
 * For now, just a very simple profile that only keeps track of how many packets were successfully or unsuccessfully delivered.
 *
 * Created by Isaac Supeene on 7/3/13.
 */
public class ReliabilityProfile {
//    int successes = 0;
//    int failures = 0;

    private Device device;
    private ReliabilityProfileImpl implementation;
    private List<ReliabilityProfileImpl> possibleImplementations = new ArrayList<ReliabilityProfileImpl>();

    public ReliabilityProfile(Device device) {
        this.device = device;

        this.possibleImplementations.add(new MaliciousProfile());
        this.possibleImplementations.add(new BackboneProfile());
        this.possibleImplementations.add(new PracticalReliabilityProfile());
        this.possibleImplementations.add(new BetterPracticalReliabilityProfile());

        this.implementation = new BackboneProfile();

        assert implementation != null;
    }

    /**
     *
     * @param hopsRemaining The number of hops remaining WHEN THE NODE RECEIVES THE PACKET!
     * @param destination
     * @return
     */
    public Function2Var<Integer, Integer, Double> getProbabilityOfSuccessFunction(final int hopsRemaining, Inet4Address destination, int transactionID ,Advert advert) {
//        return new Function2Var<Integer, Integer, Double>() {
//            @Override
//            public Double evaluate(Integer x, Integer y) {
//                // For now, we are as simple as possible, and just base it on their history of success and failure.
//                return hopsRemaining > 0 ? ((double)successes) / (successes + failures) : 0.0;
//                // TODO: something smarter.
//            }
//        };
        assert implementation != null;
        return implementation.getProbabilityOfSuccessFunction(hopsRemaining, destination, transactionID, advert);
    }


    public void update(int transactionID, boolean success, HistoryAgent.DataPath.Hop ourHop) {
//        if (success) {
//            successes += 1;
//        }
//        else {
//            failures += 1;
//        }
        ReliabilityProfileImpl best = null;
        double bestValue = Double.MIN_VALUE;
        for (ReliabilityProfileImpl profile : possibleImplementations) {
            double newValue = profile.matchToDevice(transactionID, success, ourHop);
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

    private interface ReliabilityProfileImpl {
        /**
         * Calibrates whatever internal parameters are used in this object to
         * predict the forwarding actions for this profile's device most accurately.
         *
         * The provided arguments are the transactionID of a packet, and whether
         * or not that packet successfully reached its destination.  The implementation
         * may optionally use them for its calibration, as an adjustment to its
         * existing state, rather than calculating some parameters anew every
         * time using the entire history of the device.
         *
         * @param transactionID The id of a completed transaction in which this node took part.
         * @param success Whether or not the packet successfully reached its destination.
         * @return The correlation coefficient of this profile's predictions when
         *         compared with real values for past events.
         */
        public double matchToDevice(int transactionID, boolean success, HistoryAgent.DataPath.Hop ourHop);

        /**
         *
         * @param hopsRemaining
         * @param destination
         * @return A function from the budget and fine we put on the auction to this device's
         *         probability of successfully delivering the packet.
         */
        public Function2Var<Integer, Integer, Double> getProbabilityOfSuccessFunction(int hopsRemaining, Inet4Address destination, int transactionID, Advert advert);

        public String getProfileName();
        public String getProfileDetails();
    }

    private class MaliciousProfile implements ReliabilityProfileImpl {

        int total = 0;
        int successes = 0;

        @Override
        public double matchToDevice(int transactionID, boolean success, HistoryAgent.DataPath.Hop ourHop) {
            ++total;
            if (success) { ++successes; }

            return 1 - ((double)successes)/total;
        }

        @Override
        public Function2Var<Integer, Integer, Double> getProbabilityOfSuccessFunction(int hopsRemaining, Inet4Address destination, int transactionID, Advert advert) {
            return new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer x, Integer y) {
                    return ((double)successes)/total;
                }
            };
        }

        @Override
        public String getProfileName() {
            return "Malicious Reliability Profile";
        }

        @Override
        public String getProfileDetails() {
            return "Drops packets at a rate of " + (1 - ((double)successes)/total) * 100 + "%";
        }
    }

    private class AngelicProfile implements ReliabilityProfileImpl {

        int total = 0;
        int successes = 0;

        @Override
        public double matchToDevice(int transactionID, boolean success, HistoryAgent.DataPath.Hop ourHop) {
            ++total;
            if (success) { ++successes; }

            return ((double)successes)/total;
        }

        @Override
        public Function2Var<Integer, Integer, Double> getProbabilityOfSuccessFunction(int hopsRemaining, Inet4Address destination, int transactionID, Advert advert) {
            return new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer x, Integer y) {
                    return ((double)successes)/total;
                }
            };
        }

        @Override
        public String getProfileName() {
            return "Angelic Reliability Profile";
        }

        @Override
        public String getProfileDetails() {
            return "Successfully delivers packets at a rate of " + ((double)successes)/total * 100 + "%";
        }
    }

    private class PracticalReliabilityProfile implements ReliabilityProfileImpl {

        int totalInRange = 0;
        int totalOutOfRange = 0;

        int successInRange = 0;
        int successOutOfRange = 0;

        @Override
        public double matchToDevice(int transactionID, boolean success, HistoryAgent.DataPath.Hop ourHop) {
            if (shouldBeSuccess(ourHop)) {
                ++totalInRange;
                successInRange += success ? 1 : 0;
            }
            else {
                ++totalOutOfRange;
                successOutOfRange += success ? 1 : 0;
            }

            double inRangeMatch = getInRangeMatch(transactionID, success, ourHop);
            double outOfRangeMatch = getOutOfRangeMatch(transactionID, success, ourHop);
            return (totalInRange*inRangeMatch + totalOutOfRange*outOfRangeMatch) / (totalInRange + totalOutOfRange);
        }

        protected boolean shouldBeSuccess(HistoryAgent.DataPath.Hop ourHop) {
            return ourHop.isCurrentlyInRange();
        }

        protected double getInRangeMatch(int transactionID, boolean success, HistoryAgent.DataPath.Hop ourHop) {
            return ((double)successInRange)/totalInRange;
        }

        protected double getOutOfRangeMatch(int transactionID, boolean success, HistoryAgent.DataPath.Hop ourHop) {
            return 1 - ((double)successOutOfRange)/totalOutOfRange;
        }

        @Override
        public Function2Var<Integer, Integer, Double> getProbabilityOfSuccessFunction(int hopsRemaining, Inet4Address destination, int transactionID, Advert advert) {
            DijkstraShortestPath<Device, Connection> shortestPath = device.getTopologyAgent().getShortestPath(destination, transactionID, null);
            synchronized (device.getTopologyAgent().topology) {
                if (shortestPath.getPath(device, device.getTopologyAgent().getDevice(destination)).size() > hopsRemaining) {
                    return new Function2Var<Integer, Integer, Double>() {
                        @Override
                        public Double evaluate(Integer budget, Integer fine) {
                            return ((double)successOutOfRange)/totalOutOfRange;
                        }
                    };
                }
                else {
                    return new Function2Var<Integer, Integer, Double>() {
                        @Override
                        public Double evaluate(Integer budget, Integer fine) {
                            return ((double)successInRange)/totalInRange;
                        }
                    };
                }
            }
        }

        @Override
        public String getProfileName() {
            return "Practical Reliability Profile";
        }

        @Override
        public String getProfileDetails() {
            return "Success rate while in range: " + ((double)successInRange)/totalInRange * 100 + "%\n" +
                    "Success rate while out of range: " + ((double)successOutOfRange)/totalOutOfRange * 100 + "%";
        }
    }

    /**
     * Like the PracticalReliabilityProfile, but considers sending the packet to the backbone.
     */
    private class BetterPracticalReliabilityProfile extends PracticalReliabilityProfile {

        @Override
        protected boolean shouldBeSuccess(HistoryAgent.DataPath.Hop ourHop) {
            return super.shouldBeSuccess(ourHop) || ourHop.backboneIsProfitable();
        }

        @Override
        public Function2Var<Integer, Integer, Double> getProbabilityOfSuccessFunction(int hopsRemaining, Inet4Address destination, final int transactionID, final Advert advert) {
            DijkstraShortestPath<Device, Connection> shortestPath = device.getTopologyAgent().getShortestPath(destination, transactionID, null);
            synchronized (device.getTopologyAgent().topology) {
                if (shortestPath.getPath(device, device.getTopologyAgent().getDevice(destination)).size() > hopsRemaining) {
                    return new Function2Var<Integer, Integer, Double>() {
                        @Override
                        public Double evaluate(Integer budget, Integer fine) {
                            if (device.getMostProbableBidAsConsumer(transactionID, budget, fine) + advert.getFine() > advert.getInitialBudget()) {
                                return ((double)successInRange)/totalInRange;
                            }
                            else {
                                return ((double)successOutOfRange)/totalOutOfRange;
                            }
                        }
                    };
                }
                else {
                    return new Function2Var<Integer, Integer, Double>() {
                        @Override
                        public Double evaluate(Integer budget, Integer fine) {
                            return ((double)successInRange)/totalInRange;
                        }
                    };
                }
            }
        }

        @Override
        public String getProfileName() {
            return "Better Practical Reliability Profile";
        }

        @Override
        public String getProfileDetails() {
            return "Success rate while in range: " + ((double)successInRange)/totalInRange * 100 + "%\n" +
                    "Success rate while out of range: " + ((double)successOutOfRange)/totalOutOfRange * 100 + "%";
        }
    }

    /**
     * Like the BetterPracticalReliabilityProfile, but also drops packets when the fine and budget are very low.
     */
    private class SophisticatedPracticalReliabilityProfile extends BetterPracticalReliabilityProfile {
// TODO
//        @Override
//        protected boolean shouldBeSuccess(HistoryAgent.DataPath.Hop ourHop) {
//            return super.shouldBeSuccess(ourHop) && ourHop.getBudgetPlusFine() != 0 isSufficientlyHigh(ourHop.getBudgetPlusFine());
//        }
//
//        private boolean isSufficientlyHigh(int budgetPlusFine) {
//            return budgetPlusFine > 20; // TODO;
//        }
    }

    private class BackboneProfile implements ReliabilityProfileImpl {

        @Override
        public double matchToDevice(int transactionID, boolean success, HistoryAgent.DataPath.Hop ourHop) {
            if (device.isBackbone()) {
                return 1;
            }
            else {
                return 0;
            }
        }

        @Override
        public Function2Var<Integer, Integer, Double> getProbabilityOfSuccessFunction(int hopsRemaining, Inet4Address destination, int transactionID, Advert advert) {
            return new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer x, Integer y) {
                    return 1.0;
                }
            };
        }

        @Override
        public String getProfileName() {
            return "Backbone Reliability Profile";
        }

        @Override
        public String getProfileDetails() {
            return "This node is a backbone, and therefore has a 100% delivery success rate.";
        }
    }
}
