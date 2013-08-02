package de.uni_bremen.comnets.maniac.agents;

import android.util.Log;

import com.android.internal.util.Predicate;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.network_manager.TopologyInfo;
import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.BidWin;
import de.fu_berlin.maniac.packet_builder.Data;
import de.uni_bremen.comnets.maniac.collections.Predicates;
import de.uni_bremen.comnets.maniac.devices.BiddingOpponent;
import de.uni_bremen.comnets.maniac.devices.Consumer;
import de.uni_bremen.comnets.maniac.devices.Device;
import de.uni_bremen.comnets.maniac.log.ResultTracer;
import de.uni_bremen.comnets.maniac.util.Function;
import de.uni_bremen.comnets.maniac.util.Function2Var;
import de.uni_bremen.comnets.maniac.util.Function3Var;
import de.uni_bremen.comnets.maniac.util.Function4Var;
import de.uni_bremen.comnets.maniac.util.NumericalMethods;
import de.uni_bremen.comnets.maniac.util.Pair;
import de.uni_bremen.comnets.maniac.util.Quadruple;

/**
 * The AuctionAgent provides the optimal AuctionParameters
 * for a given scenario.
 *
 * Created by Isaac Supeene on 6/11/13.
 */
public class AuctionAgent extends Agent {

    /**
     * Updated in onRcvAdvert.  We spend the time between receiving the advert and
     * sending our bid to calculate the optimal parameters.
     *
     * Read from in onRcvData.
     *
     * Cleared in onRcvData if we use it in an auction, and in onRcvBidWin otherwise
     */
    public Map<Integer, AuctionParameters> optimalAuctionParameters = new HashMap<Integer, AuctionParameters>();
    /**
     * Updated in onRcvAdvert.  We spend the time between receiving the advert and
     * sending our bid to calculate the optimal bid.
     *
     * Read from in sendBid.
     *
     * Cleared in sendBid.
     */
    public Map<Integer, Integer> optimalBids = new HashMap<Integer, Integer>();

    private TopologyAgent topologyAgent;    public void setTopologyAgent(TopologyAgent agent) {
        topologyAgent = agent;
    }
    private HistoryAgent historyAgent;      public void setHistoryAgent(HistoryAgent agent) {
        historyAgent = agent;
    }

    private int droppingThreshold = 10;
    public void setDroppingThreshold(int threshold) {
        this.droppingThreshold = threshold;
    }

    private boolean tryUnknownAdverts = true;
    public void setTryUnknownAdverts(boolean tryUnknownAdverts) {
        this.tryUnknownAdverts = tryUnknownAdverts;
    }

    /**
     * The Exploit Lifecycle
     *
     * When we want to take advantage of another node with a trick, we need to keep
     * careful track of whether or not it worked, so we know whether or not to use this
     * trick in the future.
     *
     * 1. Low-budget Tricks / Exploits
     *
     * The Low-Budget Trick is when we have a packet with 2 hops left, and we are 1 away from
     * a node that can deliver the packet immediately.  In this case, we want to take advantage
     * of this node by setting our budget to 1, and hogging all the profit for ourselves.  The
     * target node will most likely be willing to deliver the packet for us, since it's trivial
     * to do so.
     *
     * When we first decide that we are going to attempt this trick, the transactionID of the
     * packet goes in the pendingExploits set.  If we win the auction for the packet, it is
     * sent to the HistoryAgent where it is added to HistoryAgent.pendingExploits, and it is
     * removed from AuctionAgent.pendingExploits.  If another node wins the bid, the entry
     * is simply removed.
     *
     * The history agent will be notified when nodes bid on the packet in question.  Once the
     * auction is finished, the history agent will adjust all adjacent nodes that didn't bid
     * on the exploit to mark them as not susceptible to the trick.  It will then add the
     * packet id and the winning node to its nodesWinningOnExploits map.
     *
     * Once we've determined that the winning node has successfully delivered the packet, we
     * update our info on him to reflect the fact that he is vulnerable to the low budget trick.
     * Otherwise, if we find that he has dropped the packet, we update his info to reflect
     * that he is not vulnerable.
     *
     *
     * Updated in onRcvAdvert while we are calculating the optimal parameters.
     *
     * Read from in onRcvData to populate the set in the HistoryAgent.
     *
     * Cleared in onRcvData if we won the auction, or in onRcvBidWin if we didn't.
     */
    public Set<Integer> pendingExploits = new HashSet<Integer>();
    /**
     * 2. Dead Packets
     *
     * If we receive a packet that cannot possibly be delivered to the destination, it's still
     * possible to auction it off and try to get other nodes to bid on it.  If it's in our
     * interest to distribute the fine from an auctioning node to a potential bidder,
     * for instance, if that bidder has a high balance and we know he is vulnerable to this
     * trick, then we will take the packet and hand it off to him.
     *
     * When we first decide that we are going to attempt this trick, the transactionID of the
     * packet goes in the pendingDeadPackets set.  If we win the auction for the packet, it is
     * sent to the HistoryAgent, where it is added to HistoryAgent.pendingDeadPackets, and it is
     * removed from AuctionAgent.pendingDeadPackets.  If another node wins the bid, the entry is
     * simply removed.
     *
     * The History Agent will be notified when nodes bid on the packet in question.  Once the
     * auction is finished, all node information for adjacent nodes will be updated to reflect
     * whether or not they are willing to bid on a dead packet.
     *
     *
     * Updated in onRcvAdvert while we are calculating the optimal parameters.
     *
     * Read from in onRcvData to populate the set in the HistoryAgent.
     *
     * Cleared in onRcvData if we won the auction, or in onRcvBidWin if we didn't.
     */
    public Set<Integer> pendingDeadPackets = new HashSet<Integer>();
    /**
     * 3. Dropped Packets
     *
     * Dropping packets is primarily in response to the low budget trick, but we might calculate
     * that it is desirable to drop a packet in other circumstances as well.  When we decide that
     * we are going to drop a packet without even holding an auction when we receive it, the
     * transactionID of the packet in question is stored in pendingDroppedPackets.  If we lose
     * the bid, or when we win the bid and successfully drop the packet, the id will be removed.
     *
     *
     * Updated in onRcvAdvert while we are calculating the optimal parameters.
     *
     * Read from in dropPacketBefore.
     *
     * Cleared in dropPacketBefore.
     */
    public Set<Integer> pendingDroppedPackets = new HashSet<Integer>();

    public AuctionParameters getOptimalParameters(Data data) {
        return getOptimalParameters(data.getTransactionID());
    }

    public AuctionParameters getOptimalParameters(int transactionID) {
        if (optimalAuctionParameters.containsKey(transactionID)) {
            AuctionParameters result = optimalAuctionParameters.get(transactionID);
            Log.d(TAG(), "Fetching AuctionParameters for transaction with ID " + transactionID + ".\n" +
                         "Budget = " + result.getMaxbid() + ", Fine = " + result.getFine());
            if (pendingExploits.contains(transactionID)) {
                Log.d(TAG(), "Using low-budget trick.");
                historyAgent.watchForExploit(transactionID);
                pendingExploits.remove(transactionID);
            }
            if (pendingDeadPackets.contains(transactionID)) {
                Log.d(TAG(), "Using dead packet trick.");
                historyAgent.watchForBidsOnDeadPacket(transactionID);
                pendingDeadPackets.remove(transactionID);
            }
            return result;
        }
        else {
            Log.w(TAG(), "Optimal AuctionParameters for transaction with id " + transactionID + " not found! Defaulting to the default.");
            return new AuctionParameters(-1, Integer.MAX_VALUE); // TODO: Default to something a little smarter than this (↑↑ and be sure to update logging)
        }
    }

    public Integer getOptimalBid(Advert advert) {
        return getOptimalBid(advert.getTransactionID());
    }

    public Integer getOptimalBid(int transactionID) {
        if (optimalBids.containsKey(transactionID)) {
            Integer result = optimalBids.get(transactionID);
            Log.d(TAG(), "Fetching optimal bid for transaction with ID " + transactionID + ".  Bid = " + result);
            return result;
        }
        else {
            // If we haven't heard about it, we probably don't want it.
            // TODO: consider a smarter default (and be sure to update logging if the default changes) ↓↓
            if (tryUnknownAdverts) {
                Log.w(TAG(), "Optimal bid for transaction with ID " + transactionID + " not found! Defaulting to half.");
                Advert advert = historyAgent.getCurrentAdvert(transactionID);
                if (advert != null) {
                    return advert.getCeil() / 2;
                }
                else {
                    Log.w(TAG(), "Actually, 20.");
                    return 20;
                }
            }
            else {
                Log.w(TAG(), "Optimal bid for transaction with ID " + transactionID + " not found! Defaulting to -1.");
                return -1;
            }
        }
    }

    public boolean dropPacketBefore(int transactionID) {
        if (pendingDroppedPackets.contains(transactionID)) {
            Log.d(TAG(), "Returning true for dropPacketBefore for transaction with ID " + transactionID);
            pendingDroppedPackets.remove(transactionID);
            return true;
        }
        else {
            return false;
        }
    }

    public void onBidWinReceived(BidWin bidwin) {
        postMessage(new BidWinReceivedMessage(bidwin));
    }

    /**
     * Begins computation of the optimal bid and AuctionParameters for the specified Advert.
     * @param advert The Advert for which to compute parameters.
     * @param adjacentDevices A list of adjacent devices provided by the TopologyAgent.  These
     *                        devices are prepared to quickly respond to queries about their
     *                        most probable bid, probability of successfully delivering the packet,
     *                        etc.  This list will never include any backbones.
     */
    public void computeParameters(Advert advert, List<Consumer> adjacentDevices, List<BiddingOpponent> biddingOpponents) {
        postMessage(new ComputeParametersMessage(advert, adjacentDevices, biddingOpponents));
    }

    /* ******** *
     * Messages *
     * ******** */

    /**
     * Changes the focus of this Agent's computations to
     * provide a more accurate optimal set of AuctionParameters
     * for an advertised data packet, provided we win the auction
     * for that packet.
     */
    private class ComputeParametersMessage extends Message {
        Advert advert;
        List<Consumer> adjacentDevices;
        List<BiddingOpponent> biddingOpponents;

        public ComputeParametersMessage(Advert advert, List<Consumer> adjacentDevices, List<BiddingOpponent> biddingOpponents) {
            this.advert = advert;
            this.adjacentDevices = adjacentDevices;
            this.biddingOpponents = biddingOpponents;
        }

        @Override
        protected void processImpl() {
            Pair<Integer, AuctionParameters> optimalParameters = selectCase(advert).getOptimalParameters(advert, adjacentDevices, biddingOpponents);
            optimalAuctionParameters.put(advert.getTransactionID(), new AuctionParameters(optimalParameters.getSecond() == null ? advert.getCeil() / 2 : optimalParameters.getSecond().getMaxbid(), advert.getFine()));
            optimalBids.put(advert.getTransactionID(), optimalParameters.getFirst());
        }

        private Case selectCase(final Advert advert) {
            if (advert.getDeadline() <= 2) { // There will only be one hop (or less) left when it reaches us.
                return new DeadPacket();
            }
            else if (advert.getDeadline() == 3 &&
                     Predicates.any(topologyAgent.getNeighbors(), new Predicate<Device>() {
                @Override
                public boolean apply(Device device) {
                             return topologyAgent.nodesAreAdjacent(device.getAddress(), advert.getFinalDestinationIP()) &&
                                    device.isVulnerableToLowBudgetTrick();
                }
            })) {
                return new ExploitAdjacentNode();
            }
            else if (tryToDropPacket(advert)) {
                return new DropPacket();
            }
            else {
                return new NormalCase();
            }
        }

        private boolean tryToDropPacket(Advert advert) {
            return advert.getCeil() < droppingThreshold && !advert.getSourceIP().equals(topologyAgent.getPartnerIP());
        }
    }

    private class BidWinReceivedMessage extends Message {
        BidWin bidwin;

        public BidWinReceivedMessage(BidWin bidwin) {
            this.bidwin = bidwin;
        }

        @Override
        protected void processImpl() {
            if (!bidwin.getWinnerIP().equals(topologyAgent.getOurIP())) {
                // If we didn't win the bid, we can stop keeping track of potential exploits we could have done with it.
                pendingExploits.remove(bidwin.getTransactionID());
                pendingDeadPackets.remove(bidwin.getTransactionID());
                pendingDroppedPackets.remove(bidwin.getTransactionID());
            }
        }
    }

    private abstract class Case {
        /**
         *
         * @param advert
         * @return
         */
        Pair<Integer, AuctionParameters> getOptimalParameters(Advert advert, List<Consumer> adjacentDevices, List<BiddingOpponent> biddingOpponents) {
            ResultTracer<Pair<Integer, AuctionParameters>> t = new ResultTracer<Pair<Integer, AuctionParameters>>(TAG(), getClass().getSimpleName() + ".getOptimalParameters", advert, adjacentDevices, biddingOpponents);
            return t.finish(getOptimalParametersImpl(advert, adjacentDevices, biddingOpponents));
        }

        abstract Pair<Integer, AuctionParameters> getOptimalParametersImpl(Advert advert, List<Consumer> adjacentDevices, List<BiddingOpponent> biddingOpponents);
    }

    /**
     * Let G = the total probabilistic gain - i.e. what we can expect to gain from a transaction on average.
     * Our goal is to maximize G.
     *
     * G = Gbid * Pbid
     *
     * where Gbid is the amount we can expect to gain if we win the bid, and Pbid is our probability of winning the bid.
     *
     * Pbid is a function of Bu, the upstream bid.
     *
     * We split the case of winning the bid into two subcases: either we are able to forward the packet, or we are not.
     *
     * Gbid = Gfor + Gnobid * Pnobid
     *
     * where Gfor is the forwarding probabilistic gain - i.e. what we can expect on average to gain from forwarding the packet,
     * taking into account the probability of successfully forwarding the packet - and Gnobid and Pnobid are the gain and
     * probability, respectively, of failing to receive any bids at all for a packet.
     *
     * We take Gfor to be max(Gfori), where Gfori is the probabilistic gain expected from forwarding to an arbitrary node i.
     * NOTE: This is an approximation, since even if the best possibility doesn't meet expectations, there will typically be
     * other possibilities.  However, I don't believe this will make a huge difference in most cases.
     *
     * Gfori = (Bu - Bdi) * Psucci + (Fd - Fu) * Pfaili
     *
     * where Bdi is our estimation of node i's most probable bid for the packet, Psucci is the probability of success
     * when forwarding the packet to node i, Fd is the downstream fine we set for our auction, Fu is the upstream fine
     * for the current auction, and Pfaili is the probability of failure when forwarding to node i.
     *
     * The rest of the undefined variables are computed from the context of the network and data we have collected about
     * the various devices in the network.
     */
    private class NormalCase extends Case {
        @Override
        public Pair<Integer, AuctionParameters> getOptimalParametersImpl(final Advert advert, final List<Consumer> adjacentDevices, final List<BiddingOpponent> biddingOpponents) {
            // Compute Bdi
            final Function3Var<Consumer, Integer, Integer, Integer> expectedBid = new Function3Var<Consumer, Integer, Integer, Integer>() {
                @Override
                public Integer evaluate(Consumer device, Integer budget, Integer fine) {
                    return device.getMostProbableBidAsConsumer(advert.getTransactionID(), budget, fine);
                }
            };

            // Compute Psucci
            final Function3Var<Consumer, Integer, Integer, Double> deviceProbabilityOfSuccess = new Function3Var<Consumer, Integer, Integer, Double>() {
                @Override
                public Double evaluate(Consumer device, Integer budget, Integer fine) {
                    return device.getProbabilityOfSuccessAsConsumer(advert.getTransactionID(), budget, fine);
                }
            };

            // Compute Pfaili
            final Function3Var<Consumer, Integer, Integer, Double> deviceProbabilityOfFailure = new Function3Var<Consumer, Integer, Integer, Double>() {
                @Override
                public Double evaluate(Consumer device, Integer budget, Integer fine) {
                    return device.getProbabilityOfFailureAsConsumer(advert.getTransactionID(), budget, fine);
                }
            };

            // Compute Pnobidi
            final Function3Var<Consumer, Integer, Integer, Double> deviceProbabilityOfNoBid = new Function3Var<Consumer, Integer, Integer, Double>() {
                @Override
                public Double evaluate(Consumer device, Integer budget, Integer fine) {
                    return device.getProbabilityOfNoBidAsConsumer(advert.getTransactionID(), budget, fine);
                }
            };

            // The gain we are likely to get if we forward to a particular node.
            // Gfori = (Bu - Bdi) * Psucci + (Fd - Fu) * Pfaili
            final Function4Var<Consumer, Integer, Integer, Integer, Double> deviceSpecificProbabilisticGain = new Function4Var<Consumer, Integer, Integer, Integer, Double>() {
                @Override
                public Double evaluate(Consumer device, Integer ourBid, Integer budget, Integer fine) {
                    return (ourBid - expectedBid.evaluate(device, budget, fine)) * deviceProbabilityOfSuccess.evaluate(device, budget, fine) +
                           (fine - advert.getFine()) * deviceProbabilityOfFailure.evaluate(device, budget, fine);
                }
            };

            // The maximum we can expect to get, by forwarding to the best node.
            // Gfor = max(Gfori)
            final Function3Var<Integer, Integer, Integer, Double> forwardingProbabilisticGain = new Function3Var<Integer, Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer bid, Integer budget, Integer fine) {
                    List<Double> results = new ArrayList<Double>();
                    for (Consumer d : adjacentDevices) {
                        results.add(deviceSpecificProbabilisticGain.evaluate(d, bid, budget, fine));
                    }
                    if (results.size() > 0) {
                        return Collections.max(results);
                    }
                    else {
                        return 0.0;
                    }
                }
            };

            // Compute Gnobid
            final Function<Integer, Double> gainOnNoBid = new Function<Integer, Double>() {
                @Override
                public Double evaluate(Integer bid) { // TODO: Is this bid needed for this computation?
                    return new Double(Math.min(/*advert.getInitialBudget*/1000000, -advert.getFine())); // TODO: Consider reputation, and the possibility of buying a ticket to the backbone.
                }
            };

            // Compute Pnobid
            final Function2Var<Integer, Integer, Double> probabilityOfNoBid = new Function2Var<Integer, Integer, Double>() {
                @Override
                public Double evaluate(Integer budget, Integer fine) {
                    double result = 1;
                    for (Consumer d : adjacentDevices) {
                        result *= deviceProbabilityOfNoBid.evaluate(d, budget, fine);
                    }
                    return result;
                }
            };

            // Compute Pbid
            final Function<Integer, Double> probabilityOfSuccessfulBid = new Function<Integer, Double>() {
                @Override
                public Double evaluate(Integer bid) {
                    return getProbabilityOfSuccessfulBid(bid, advert, biddingOpponents);
                }
            };


            if (topologyAgent.nodesAreAdjacent((Inet4Address) TopologyInfo.getInterfaceIpv4("wlan0"), advert.getFinalDestinationIP())) {
                // If we are adjacent to the backbone, we can deliver it immediately.
                final Function<Integer, Double> gainOnSuccessfulBid = new Function<Integer, Double>() {
                    @Override
                    public Double evaluate(Integer bid) {
                        return new Double(bid);
                    }
                };

                // G = Gbid * Pbid
                Function<Integer, Double> totalProbabilisticGain = new Function<Integer, Double>() {
                    @Override
                    public Double evaluate(Integer bid) {
                        return probabilityOfSuccessfulBid.evaluate(bid) * gainOnSuccessfulBid.evaluate(bid);
                    }
                };

                Integer optimalBid = NumericalMethods.fibonacciMax(totalProbabilisticGain, 1, advert.getCeil()).getFirst();
                return Pair.make(optimalBid, new AuctionParameters(advert.getFine(), advert.getFine())); // TODO: Evaluate default auction parameters
            }
            else {
                // Gbid = Gfor + Gnobid * Pnobid
                final Function3Var<Integer, Integer, Integer, Double> gainOnSuccessfulBid = new Function3Var<Integer, Integer, Integer, Double>() {
                    @Override
                    public Double evaluate(Integer bid, Integer budget, Integer fine) {
                        return forwardingProbabilisticGain.evaluate(bid, budget, fine) + gainOnNoBid.evaluate(bid) * probabilityOfNoBid.evaluate(budget, fine);
                    }
                };

                // G = Gbid * Pbid
                final Function3Var<Integer, Integer, Integer, Double> totalProbabilisticGain = new Function3Var<Integer, Integer, Integer, Double>() {
                    @Override
                    public Double evaluate(Integer bid, Integer budget, Integer fine) {
                        return probabilityOfSuccessfulBid.evaluate(bid) * gainOnSuccessfulBid.evaluate(bid, budget, fine);
                    }
                };

                Quadruple<Integer, Integer, Integer, Double> max;
                try {
                    // Note that we ignore the constraint that the fine must be lower than the budget - the framework will
                    // automatically lower the fine to be equal to the budget if we exceed it.
                    // TODO: See if we really do need to worry about this constraint.
                    // Note also that we are putting the previous fine's max budget as our max budget.  This is basically
                    // just to restrict the range of values we need to search through.
                    // TODO: See if we need to consider setting a budget above the previous auction's ceiling.
                    max = NumericalMethods.fibonacciMax3Var(totalProbabilisticGain,
                                                            1, advert.getCeil(),
                                                            1, advert.getCeil(),
                                                            1, advert.getFine());
                    if (max.getThird() > max.getSecond()) {
                        Log.w(TAG(), "Optimal AuctionParameters calculated had a higher fine than bid!");
                    }
                }
                catch (StackOverflowError ex) {
                    ex.printStackTrace();
                    Log.e(TAG(), "fibonacciMax3Var (standard case) threw StackOverflowError for the following advert: " + advert);
                    return Pair.make(advert.getCeil(), new AuctionParameters(advert.getCeil(), advert.getFine())); // TODO: Better default?
                }

                return Pair.make(max.getFirst(), new AuctionParameters(max.getSecond(), max.getThird()));
            }
        }
    }

    /**
     * The case when we receive a packet with 1 or fewer hops remaining.
     * If we can connect to the backbone and deliver the packet, we will
     * not be holding an auction anyway, so we can simply ignore that case,
     * and assume for the purposes of the auction that we do not have backbone
     * connectivity.
     *
     * The idea behind this special case, is that nobody can deliver the packet anymore,
     * so we want to minimize our losses.  Thus, we keep the fine the same, in
     * an attempt to slough off the entire burden on someone else, and then make
     * the budget as high as possible, to entice other nodes to bid on the
     * dead packet.
     */
    private class DeadPacket extends Case {
        @Override
        public Pair<Integer, AuctionParameters> getOptimalParametersImpl(final Advert advert, final List<Consumer> adjacentDevices, final List<BiddingOpponent> biddingOpponents) {
            // We try for the packet if there is a node next to us that's vulnerable to this trick, and who has
            // more credit that the node holding this auction (so we defer the penalty to the node that's doing better).
            pendingDeadPackets.add(advert.getTransactionID());

            if (Predicates.any(topologyAgent.getNeighbors(), new Predicate<Device>() {
                @Override
                public boolean apply(Device device) {
                               return device.isVulnerableToDeadPacketTrick() &&
                                     (device.getBalance() > topologyAgent.getDevice(advert.getSourceIP()).getBalance() ||
                                             topologyAgent.getDevice(advert.getSourceIP()).isBackbone());
                               }
                }))
            {
                return Pair.make(0, new AuctionParameters(Integer.MAX_VALUE, advert.getFine()));
            }
            else {
                return Pair.make(advert.getCeil(), new AuctionParameters(Integer.MAX_VALUE, advert.getFine()));
            }
        }
    }

    /**
     * If our packet has two hops remaining, and we are adjacent to someone who
     * can deliver the packet, and who has not caught on to this trick, set
     * both the fine and the budget to 1.  Nodes who know they can deliver the
     * packet immediately will likely bid on it no matter what, so we will get
     * a lot of free credit from this.
     */
    private class ExploitAdjacentNode extends Case {
        @Override
        public Pair<Integer, AuctionParameters> getOptimalParametersImpl(final Advert advert, final List<Consumer> adjacentDevices, final List<BiddingOpponent> biddingOpponents) {

            pendingExploits.add(advert.getTransactionID());

            final Function<Integer, Double> probabilityOfSuccessfulBid = new Function<Integer, Double>() {
                @Override
                public Double evaluate(Integer bid) {
                   return getProbabilityOfSuccessfulBid(bid, advert, biddingOpponents);
                }
            };

            // We pay the next node 1 credit to forward the packet, and hog the rest.
            final Function<Integer, Double> gainOnSuccessfulBid = new Function<Integer, Double>() {
                @Override
                public Double evaluate(Integer bid) {
                    Consumer victim = Collections.max(adjacentDevices, new Comparator<Consumer>() {
                        @Override
                        public int compare(Consumer device, Consumer device2) {
                            return device.getVulnerabilityToLowBudgetTrick() > device2.getVulnerabilityToLowBudgetTrick() ? 1 : -1;
                        }
                    });
                    return new Double((bid - 1) * victim.getVulnerabilityToLowBudgetTrick());
                }
            };

            Function<Integer, Double> totalProbabilisticGain = new Function<Integer, Double>() {
                @Override
                public Double evaluate(Integer bid) {
                    return probabilityOfSuccessfulBid.evaluate(bid) * gainOnSuccessfulBid.evaluate(bid);
                }
            };

            Integer optimalBid;
            try {
                optimalBid = NumericalMethods.fibonacciMax(totalProbabilisticGain, 1, advert.getCeil()).getFirst();
            }
            catch (StackOverflowError ex) {
                ex.printStackTrace();
                Log.e(TAG(), "fibonacciMax (exploit adjacent node case) threw StackOverflowError for the following advert: " + advert);
                return Pair.make(advert.getCeil(), new AuctionParameters(1, 1)); // TODO better bid default.
            }

            return Pair.make(optimalBid, new AuctionParameters(1, 1));
        }
    }

    private class DropPacket extends Case {
        @Override
        public Pair<Integer, AuctionParameters> getOptimalParametersImpl(Advert advert, List<Consumer> adjacentDevices, List<BiddingOpponent> biddingOpponents) {
            pendingDroppedPackets.add(advert.getTransactionID());

            // We want the packet so we can drop it, and we don't care about the auction parameters, because there's not going to be one. :)
            return Pair.make(0, null);
        }
    }

    private Double getProbabilityOfSuccessfulBid(Integer bid, Advert advert, List<BiddingOpponent> biddingOpponents) {
        return topologyAgent.getDevice(advert.getSourceIP()).getProbabilityOfChoosingOurBid(bid, advert, biddingOpponents);
    }
}
