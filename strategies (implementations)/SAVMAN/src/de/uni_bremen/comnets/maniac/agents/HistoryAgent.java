package de.uni_bremen.comnets.maniac.agents;

import android.util.Log;

import com.android.internal.util.Predicate;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.fu_berlin.maniac.bank.BankManager;
import de.fu_berlin.maniac.general.Mothership;
import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.Bid;
import de.fu_berlin.maniac.packet_builder.BidWin;
import de.uni_bremen.comnets.maniac.Brain;
import de.uni_bremen.comnets.maniac.collections.InvertibleArrayList;
import de.uni_bremen.comnets.maniac.collections.InvertibleList;
import de.uni_bremen.comnets.maniac.collections.Predicates;
import de.uni_bremen.comnets.maniac.devices.Device;
import de.uni_bremen.comnets.maniac.graphs.Connection;
import de.uni_bremen.comnets.maniac.log.ResultTracer;
import de.uni_bremen.comnets.maniac.util.DelayableTimerTask;
import de.uni_bremen.comnets.maniac.util.Pair;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;

/**
 * Many Strategy methods take only Bids as parameters, which
 * doesn't give us too much information about what they're
 * bidding for - for example, which packet they are bidding for,
 * how much we bid for it, and what our fine will be if it isn't
 * delivered.  This class provides this information by correlating
 * new information about Bids and BidWins to trace the path of each
 * data packet through the MANET.
 *
 * Created by isaac on 6/12/13.
 */
public class HistoryAgent extends Agent {

    public static final int IN_PROGRESS = 0;
    public static final int SUCCESSFULLY_DELIVERED = 1;
    public static final int DROPPED_OR_EXPIRED = 2;
    public static final int UNKNOWN = 3;

    private final Object auctionMapLock = new Object();

    public interface DataPathInfoListener {
        public void onNewPathInfo(List<DataPath> dataPaths);
    }
    private List<DataPathInfoListener> pathInfoListeners = new ArrayList<DataPathInfoListener>();
    public void addPathInfoListener(DataPathInfoListener listener) {
        pathInfoListeners.add(listener);
    }
    public void removePathInfoListener(DataPathInfoListener listener) {
        pathInfoListeners.remove(listener);
    }

    private TopologyAgent topologyAgent;    public void setTopologyAgent(TopologyAgent agent) {
        topologyAgent = agent;
    }

    /**
     * Updated in onRcvData.  We upgrade a potential exploit stored in the
     * AuctionAgent to an actually attempted exploit.
     *
     * Read from in onRcvBid to update nodesBiddingOnExploit, and selectWinner
     * to check if we need to update node vulnerability.
     *
     * Cleared in selectWinner.
     */
    private Set<Integer> pendingExploits = new HashSet<Integer>();
    /**
     * Updated in onRcvBid
     *
     * Read from in selectWinner
     *
     * Cleared in selectWinner
     */
    private Map<Integer, Set<Device>> nodesBiddingOnExploit = new HashMap<Integer, Set<Device>>();
    /**
     * Updated in selectWinner.
     *
     * Read from in a delayed timer task specifying the termination of a packet's path.
     * This timer task is started in onRcvBidWin or onRcvAdvert, and its timer is
     * restarted every time we get a BidWin for that particular auction.
     * @see DataPath
     *
     * Cleared in the aforementioned timer task.
     */
    private Map<Integer, Device> nodesWinningOnExploit = new HashMap<Integer, Device>();
    /**
     * Updated in onRcvData.  We upgrade a dead packet which we might get from
     * an auction to one which we have actually received.
     *
     * Read from in onRcvBid to update nodesBiddingOnDeadPacket, and selectWinner
     * to update the nodes' probability of bidding on dead packets.
     *
     * Cleared in selectWinner
     */
    private Set<Integer> pendingDeadPackets = new HashSet<Integer>();
    /**
     * Updated in onRcvBid.
     *
     * Read from in selectWinner.
     *
     * Cleared in selectWinner.
     */
    private Map<Integer, Set<Device>> nodesBiddingOnDeadPacket = new HashMap<Integer, Set<Device>>();

    public class DataPath {
        public class Hop {
            public void tryModifyVulnerabilityToLowBudgetTrick(boolean success) { }
            public void updateDeviceBalance(boolean success) { }
            public void updateDeviceReliabilityProfile(int transactionID, boolean success) { }
            public Device getImportantDevice() { return null; }
            public boolean contains(Inet4Address address) { return false; }
            public boolean isCurrentlyInRange() { return false; }
            public boolean backboneIsProfitable() { return false; }
            public int getBudgetPlusFine() { return 0; }

            protected boolean inRange(Inet4Address source, Device exception, Advert originalAdvert) {
                synchronized (topologyAgent.topology) {
                    ResultTracer<Boolean> t = new ResultTracer<Boolean>(TAG(), source, exception, originalAdvert);
                    DijkstraShortestPath<Device, Connection> shortestPath = topologyAgent.getShortestPath(source, transactionID, exception);
                    return t.finish(shortestPath.getPath(topologyAgent.getDevice(source), topologyAgent.getDevice(originalAdvert.getFinalDestinationIP())).size() >= originalAdvert.getDeadline());
                }
            }

            protected boolean profitableBackbone(Advert advert) {
                return advert.getCeil() + advert.getFine() > advert.getInitialBudget();
            }

            protected boolean inRange(Inet4Address source, Device exception, HopFirstHalf originalAdvert) {
                try {
                    synchronized (topologyAgent.topology) {
                        DijkstraShortestPath<Device, Connection> shortestPath = topologyAgent.getShortestPath(source, transactionID, exception);
                        return shortestPath.getPath(topologyAgent.getDevice(source), topologyAgent.getDevice(originalAdvert.finalDestination)).size() >= originalAdvert.hopsRemaining;
                    }
                }
                catch (NullPointerException ex) {
                    ex.printStackTrace();
                    return false;
                }
            }

            protected boolean profitableBackbone(HopFirstHalf advert) {
                return advert.budget + advert.fine > advert.initialBudget;
            }
        }

        public class KnownSource extends Hop {
            Inet4Address packetSource;
            int originalBudget;
            int originalFine;
            boolean currentlyInRange;

            public KnownSource(Advert originalAdvert) {
                this.packetSource = originalAdvert.getSourceIP();
                this.originalBudget = originalAdvert.getCeil();
                this.originalFine = originalAdvert.getFine();
                currentlyInRange = inRange(packetSource, null, originalAdvert);
            }

            @Override
            public String toString() {
                return "Original Advert from IP " + packetSource.toString().substring(1) +
                     "\nBudget: " + originalBudget + ";  Fine: " + originalFine;
            }

            @Override
            public Device getImportantDevice() {
                return topologyAgent.getDevice(packetSource);
            }

            @Override
            public boolean isCurrentlyInRange() {
                return currentlyInRange;
            }

            @Override
            public boolean backboneIsProfitable() {
                return false;
            }

            @Override
            public int getBudgetPlusFine() {
                return originalFine + originalBudget;
            }
        }

        public class UnknownSource extends Hop {
            @Override
            public String toString() {
                return "Unknown packet origin.";
            }
        }

        public class HopFirstHalf extends Hop {
            Inet4Address auctioneer;
            int budget;
            int fine;
            int hopsRemaining;
            Inet4Address finalDestination;
            int initialBudget;
            boolean currentlyInRange;
            boolean backboneProfitable;

            public HopFirstHalf(Advert advert) {
                auctioneer = advert.getSourceIP();
                budget = advert.getCeil();
                fine = advert.getFine();
                hopsRemaining = advert.getDeadline();
                finalDestination = advert.getFinalDestinationIP();
                initialBudget = advert.getInitialBudget();
                currentlyInRange = inRange(auctioneer, null, advert);
                backboneProfitable = profitableBackbone(advert);
            }

            @Override
            public String toString() {
                if (auctioneer == null) {
                    Log.e(TAG(), "auctioneer is NULL!!!!");
                }
                return "Auction held by node with IP " + auctioneer.toString().substring(1) +
                     "\nBudget: " + budget + ";  Fine: " + fine + ";  Hops Remaining: " + hopsRemaining;
            }

            @Override
            public void updateDeviceReliabilityProfile(int transactionID, boolean success) {
                topologyAgent.getDevice(auctioneer).updateReliabilityProfile(transactionID, success, this);
            }

            @Override
            public Device getImportantDevice() {
                return topologyAgent.getDevice(auctioneer);
            }

            @Override
            public boolean contains(Inet4Address address) {
                return auctioneer.equals(address);
            }

            @Override
            public boolean isCurrentlyInRange() {
                return currentlyInRange;
            }

            @Override
            public boolean backboneIsProfitable() {
                return backboneProfitable;
            }

            @Override
            public int getBudgetPlusFine() {
                return budget + fine;
            }
        }

        public class HopSecondHalf extends Hop {
            Inet4Address auctioneer;
            Inet4Address auctionWinner;
            int bid;
            int fine;

            public HopSecondHalf(BidWin bidwin) {
                auctioneer = bidwin.getSourceIP();
                auctionWinner = bidwin.getWinnerIP();
                bid = bidwin.getWinningBid();
                fine = bidwin.getFine();
            }

            @Override
            public String toString() {
                return "Auction held by node with IP " + auctioneer.toString().substring(1) +
                     "\nWon by node with IP " + auctionWinner.toString().substring(1) +
                     "\nBudget: Unknown;  Bid: " + bid + ";  Fine: " + fine;
            }

            @Override
            public void tryModifyVulnerabilityToLowBudgetTrick(boolean success) {
                if (nodesWinningOnExploit.get(transactionID) == topologyAgent.getDevice(auctionWinner)) {
                    topologyAgent.getDevice(auctionWinner).modifyVulnerabilityToLowBudgetTrick(success);
                }
            }

            @Override
            public void updateDeviceBalance(boolean success) {
                topologyAgent.getDevice(auctioneer).addBalance(success ? -bid : 0);
                topologyAgent.getDevice(auctionWinner).addBalance(success ? bid : -fine);
            }

            @Override
            public void updateDeviceReliabilityProfile(int transactionID, boolean success) {
                topologyAgent.getDevice(auctioneer).updateReliabilityProfile(transactionID, success, this);
                topologyAgent.getDevice(auctionWinner).updateReliabilityProfile(transactionID, success, this);
            }

            @Override
            public Device getImportantDevice() {
                return topologyAgent.getDevice(auctionWinner);
            }

            @Override
            public boolean contains(Inet4Address address) {
                return auctioneer.equals(address) || auctionWinner.equals(address);
            }

            @Override
            public int getBudgetPlusFine() {
                return bid + fine;
            }
        }

        public class HopFull extends Hop {
            Inet4Address auctioneer;
            Inet4Address auctionWinner;
            int budget;
            int bid;
            int fine;
            int hopsRemaining;
            boolean currentlyInRange;
            boolean backboneProfitable;

            public HopFull(HopFirstHalf advert, BidWin bidwin) {
                auctioneer = advert.auctioneer;
                auctionWinner = bidwin.getWinnerIP();
                budget = advert.budget;
                bid = bidwin.getWinningBid();
                fine = advert.fine;
                hopsRemaining = advert.hopsRemaining;
                currentlyInRange = inRange(auctioneer, topologyAgent.getDevice(auctionWinner), advert);
                backboneProfitable = profitableBackbone(advert);
            }

            @Override
            public String toString() {
                return "Auction held by node with IP " + auctioneer.toString().substring(1) +
                     "\nWon by node with IP " + auctionWinner.toString().substring(1) +
                     "\nBudget: " + budget + ";  Bid: " + bid + ";  Fine: " + fine +
                     "\nHops Remaining: " + hopsRemaining;
            }

            @Override
            public void tryModifyVulnerabilityToLowBudgetTrick(boolean success) {
                if (nodesWinningOnExploit.get(transactionID) == topologyAgent.getDevice(auctionWinner)) {
                    topologyAgent.getDevice(auctionWinner).modifyVulnerabilityToLowBudgetTrick(success);
                }
            }

            @Override
            public void updateDeviceBalance(boolean success) {
                topologyAgent.getDevice(auctioneer).addBalance(success ? -bid : 0);
                topologyAgent.getDevice(auctionWinner).addBalance(success ? bid : -fine);
            }

            @Override
            public void updateDeviceReliabilityProfile(int transactionID, boolean success) {
                topologyAgent.getDevice(auctioneer).updateReliabilityProfile(transactionID, success, this);
                topologyAgent.getDevice(auctionWinner).updateReliabilityProfile(transactionID, success, this);
            }

            @Override
            public Device getImportantDevice() {
                return topologyAgent.getDevice(auctionWinner);
            }

            @Override
            public boolean contains(Inet4Address address) {
                return auctioneer.equals(address) || auctionWinner.equals(address);
            }

            @Override
            public boolean isCurrentlyInRange() {
                return currentlyInRange;
            }

            @Override
            public boolean backboneIsProfitable() {
                return backboneProfitable;
            }

            @Override
            public int getBudgetPlusFine() {
                return budget + fine;
            }
        }

        public class HopUnknown extends Hop {
            @Override
            public String toString() {
                return "Unknown intermediate path.";
            }
        }

        public class Drop extends Hop {
            @Override
            public String toString() {
                return "Packet was dropped by this node.";
            }

            @Override
            public Device getImportantDevice() {
                return topologyAgent.getOurDevice();
            }
        }

        public class UseBackbone extends Hop {
            @Override
            public String toString() {
                return "This packet was forwarded to the backbone by your device.";
            }

            @Override
            public Device getImportantDevice() {
                return topologyAgent.getOurDevice();
            }
        }

        public DataPath(int transactionID) {
            this.transactionID = transactionID;
            this.status = IN_PROGRESS;
            hops.add(new UnknownSource());
            hops.add(new HopUnknown());
        }

        public DataPath(Advert advert) {
            this.transactionID = advert.getTransactionID();
            this.destination = advert.getFinalDestinationIP();
            this.remainingHops = advert.getDeadline();
            this.status = IN_PROGRESS;
            if (topologyAgent.getDevice(advert.getSourceIP()).isBackbone()) {
                this.maxHops = advert.getDeadline();
                this.source = advert.getSourceIP();
            }

            if (topologyAgent.getDevice(advert.getSourceIP()).isBackbone()) {
                hops.add(new KnownSource(advert));
            }
            else {
                hops.add(new UnknownSource());
                hops.add(new HopUnknown());
            }
            hops.add(new HopFirstHalf(advert));
            defaultPathTerminatedTask.start();
        }

        public DataPath(BidWin bidWin) {
            this.transactionID = bidWin.getTransactionID();
            this.status = IN_PROGRESS;

            hops.add(new UnknownSource());
            if (!topologyAgent.getDevice(bidWin.getSourceIP()).isBackbone()) {
                hops.add(new HopUnknown());
            }
            hops.add(new HopSecondHalf(bidWin));
            defaultPathTerminatedTask.start();
        }

        private int transactionID; public int getTransactionID() {
            return transactionID;
        }
        private int status;
        private Inet4Address source; public Inet4Address getSource() {
            return source;
        }
        private Inet4Address destination; public Inet4Address getDestination() {
            return destination;
        }
        private Integer maxHops; public Integer getMaxHops() {
            return maxHops;
        }
        private Integer remainingHops; public Integer getRemainingHops() {
            return remainingHops;
        }
        private InvertibleList<Hop> hops = new InvertibleArrayList<Hop>(); public InvertibleList<Hop> getHops() {
            return hops;
        }
        private boolean includesUs = false;

        private DelayableTimerTask defaultPathTerminatedTask = new DelayableTimerTask(10 * Brain.getAuctionTimeout(), new Runnable() {
            @Override
            public void run() {
                currentPackets.remove(transactionID);
            }
        });
        private DelayableTimerTask pathTerminatedTask;
        private void startPathTerminatedTask() {
            // Remaining hops should never be null at this point, but we will add a default, just in case.
            pathTerminatedTask = new DelayableTimerTask(remainingHops == null ? 10000 : remainingHops * Brain.getAuctionTimeout(), new Runnable() {
                @Override
                public void run() {
                    currentPackets.remove(transactionID);
                    if (hops.size() == 0) {
                        // Under normal game conditions, this shouldn't really happen, but we don't want to crash if it does.
                        return;
                    }

                    if (includesUs) {
                        Integer diff = null;
                        for (int i = 10; i > 0; --i) { // Try 10 times to get the bank info for this transaction.
                            try {
                                diff = BankManager.getAmount(transactionID);
                                if (diff != null) {
                                    break;
                                }
                            }
                            catch (NullPointerException ex) { }
                            try {
                                Thread.sleep(1000); // Try to get the bank info every second.
                            }
                            catch (InterruptedException ex) { }
                        }
                        if (diff != null) {
                            boolean success = diff > 0;

                            hops.get(-1).tryModifyVulnerabilityToLowBudgetTrick(success);
                            for (Hop hop : hops) {
                                hop.updateDeviceBalance(success);
                                hop.updateDeviceReliabilityProfile(transactionID, success);
                            }

                            status = success ? SUCCESSFULLY_DELIVERED : DROPPED_OR_EXPIRED;
                        }
                        else {
                            status = UNKNOWN;
                        }
                        nodesWinningOnExploit.remove(transactionID);
                    }
                    else {
                        status = UNKNOWN;
                    }
                    // If we didn't participate in the packet delivery, we have no sure way of knowing whether it succeeded or failed!
                }
            });
            pathTerminatedTask.start();
            defaultPathTerminatedTask.cancel();
        }

        void updatePath(Advert advert) {
            if (destination != null) {
                destination = advert.getFinalDestinationIP();
            }
            remainingHops = advert.getDeadline();

            Hop lastHop = hops.get(-1);
            if (lastHop instanceof HopSecondHalf) {
                HopSecondHalf lastHopSecondHalf = (HopSecondHalf)lastHop;
                if (!lastHopSecondHalf.auctionWinner.equals(advert.getSourceIP())) {
                    hops.add(new HopUnknown());
                }
            }
            else if (lastHop instanceof HopFull) {
                HopFull lastHopFull = (HopFull)lastHop;
                if (!lastHopFull.auctionWinner.equals(advert.getSourceIP())) {
                    hops.add(new HopUnknown());
                }
            }
            else {
                hops.add(new HopUnknown());
            }
            hops.add(new HopFirstHalf(advert));

            if (topologyAgent.getOurIP().equals(advert.getSourceIP())) {
                includesUs = true; // Hopefully this is already true from the BidWin update.
            }
            onPathInfoChanged();
        }

        void updatePath(BidWin bidwin) {
            Hop lastHop = hops.get(-1);
            if (lastHop instanceof HopFirstHalf) {
                hops.set(-1, new HopFull((HopFirstHalf)lastHop, bidwin));
            }
            else if (lastHop instanceof HopSecondHalf) {
                HopSecondHalf secondHalf = (HopSecondHalf)lastHop;
                if (!secondHalf.auctionWinner.equals(bidwin.getSourceIP())) {
                    hops.add(new HopUnknown());
                }
                hops.add(new HopSecondHalf(bidwin));
            }
            else if (lastHop instanceof KnownSource) {
                KnownSource knownSource = (KnownSource)lastHop;
                if (!knownSource.packetSource.equals(bidwin.getSourceIP())) {
                    hops.add(new HopUnknown());
                }
                hops.add(new HopSecondHalf(bidwin));
            }
            else {
                hops.add(new HopUnknown());
                hops.add(new HopSecondHalf(bidwin));
            }

            if (topologyAgent.getOurIP().equals(bidwin.getWinnerIP())) {
                includesUs = true;
                startPathTerminatedTask();
            }
            onPathInfoChanged();
        }

        public boolean contains(final Inet4Address address) {
            return Predicates.any(hops, new Predicate<Hop>() {
                @Override
                public boolean apply(Hop hop) {
                    return hop.contains(address);
                }
            });
        }

        void drop() {
            hops.add(new Drop());
        }

        void toBackbone() {
            hops.add(new UseBackbone());
        }

        public int getStatus() {
            return status;
        }
    }

    private void onPathInfoChanged() {
        for (DataPathInfoListener listener : pathInfoListeners) {
            listener.onNewPathInfo(new ArrayList<DataPath>(allPackets.values()));
        }
    }

    /**
     * Updated in onRcvAdvert and onRcvBidWin.
     *
     * Read from in the timer task signaling the end of a DataPath.
     * This timer task is started in onRcvBidWin or onRcvAdvert, and its timer is
     * restarted every time we get a BidWin for that particular auction.
     * @see DataPath
     *
     * Cleared in the aforementioned timer task
     */
    private Map<Integer, DataPath> currentPackets = new HashMap<Integer, DataPath>();
    /**
     * Updated in onRcvAdvert and onRcvData - when we learn about an existing auction, or when we start one ourselves
     *
     * Read from in onRcvBid to update nodes' bidding profiles
     *
     * Cleared via a timer task after AUCTION_TIMEOUT
     * @see Mothership
     */
    private Map<Integer, Pair<Advert, DelayableTimerTask>> currentAuctions = new HashMap<Integer, Pair<Advert, DelayableTimerTask>>(); // Auctions less than 3 seconds old
    /**
     * Updated in the timer task which moves auctions from currentAuctions into recentAuctions.
     *
     * Read from in onRcvBidWin to match a bidwin with an advert
     *
     * Cleared via a timer task started from the timer task that populates it.
     */
    private Map<Integer, Advert> recentAuctions = new HashMap<Integer, Advert>(); // Auctions greater than 3 but less than 6 seconds old.
    /**
     * Updated in onRcvBidWin.
     *
     * Read from in selectWinner to see what our bid/fine was for a packet.
     *
     * Never cleared - permanent record of successful bids.
     */
    private Map<Integer, Pair<Advert, BidWin>> ourWonAuctions = new HashMap<Integer, Pair<Advert, BidWin>>(); // Auctions that we have won
    /**
     * Updated whenever currentPackets is updated.
     *
     * Provides historical information to the app.
     *
     * Never cleared - permanent record of data paths.
     */
    private Map<Integer, DataPath> allPackets = new HashMap<Integer, DataPath>();
    /**
     * Updated in onSendAdvert
     *
     * Read from in selectWinner to get details about the auction and create a fake BidWin.
     *
     * Never cleared - permanent record of our auctions (could be cleared after selectWinner?).
     */
    private Map<Integer, Advert> ourAuctions = new HashMap<Integer, Advert>(); // Auctions we have held / are holding
    public DataPath getPacket(Integer transactionID) {
        if (!allPackets.containsKey(transactionID)) {
            allPackets.put(transactionID, new DataPath(transactionID));
        }
        return allPackets.get(transactionID);
    }

//    private Mothership mothership;  public void setMothership(Mothership mothership) {
//        this.mothership = mothership;
//    }

    /**
     * Returns the advert that this bid is for.
     * @param transactionID
     * @return The advert in response to which the specified bid was provided.
     */
    public Advert getCurrentAdvert(int transactionID) {
        if (currentAuctions.containsKey(transactionID)) {
            return currentAuctions.get(transactionID).getFirst();
        }
        else if (recentAuctions.containsKey(transactionID)) {
            Log.d(TAG(), "A call to getCurrentAdvert(" + transactionID + ") had to default to an advert from recentAuctions.");
            return recentAuctions.get(transactionID);
        }
        else {
            Log.w(TAG(), "A call to getCurrentAdvert(" + transactionID + ")  had to default to null.");
            return null;
        }
    }

    public Advert getOurAdvert(int transactionID) {
        if (ourAuctions.containsKey(transactionID)) {
            return ourAuctions.get(transactionID);
        }
        else {
            Log.w(TAG(), "A call to getOurAdvert had to default to null.");
            return null;
        }
    }

    public Advert getPreviousAdvert(int transactionID) {
        if (ourWonAuctions.containsKey(transactionID)) {
            return ourWonAuctions.get(transactionID).getFirst();
        }
        else {
            Log.w(TAG(), "A call to getPreviousAdvert(" + transactionID + ") had to default to null.");
            return null;
        }
    }

    /**
     * Returns the amount that we bid to acquire the data packet
     * with the provided id
     * @param transactionID The id of the packet we bid for
     * @return The amount we bid for the packet with the specified id if we won a bid for this packet, otherwise 0.
     */
    public int getOurBid(int transactionID) {
        if (ourWonAuctions.containsKey(transactionID)) {
            return ourWonAuctions.get(transactionID).getSecond().getWinningBid();
        }
        else {
            Log.w(TAG(), "Trying to get our bid from an auction which we apparently haven't won (ID: " + transactionID + ")");
            return 0;
        }
    }

    public void onBidReceived(Bid bid) {
        postMessage(new BidReceivedMessage(bid));
    }

    public void onBidWinReceived(BidWin bidwin) {
        postMessage(new BidWinReceivedMessage(bidwin));
    }

    public void onAdvertReceived(Advert advert) {
        postMessage(new AdvertReceivedMessage(advert));
    }

    public void onAdvertSent(Advert advert) {
        postMessage(new AdvertSentMessage(advert));
    }

    public void onBidSent(int transactionID, int bid) {
        postMessage(new BidSentMessage(transactionID, bid));
    }

    public void onBackboneUsed(int transactionID) {
        postMessage(new UseBackboneMessage(transactionID));
    }

    public void onDropPacket(int transactionID) {
        postMessage(new PackageDroppedMessage(transactionID));
    }

    public void onAuctionFinished(int transactionID, Inet4Address winner) {
        postMessage(new AuctionFinishedMessage(transactionID, winner));
    }

    public void watchForExploit(int transactionID) {
        pendingExploits.add(transactionID);
        nodesBiddingOnExploit.put(transactionID, new HashSet<Device>());
    }

    public void watchForBidsOnDeadPacket(int transactionID) {
        pendingDeadPackets.add(transactionID);
        nodesBiddingOnDeadPacket.put(transactionID, new HashSet<Device>());
    }


    /* ******** *
     * Messages *
     * ******** */

    /**
     * Called when a BidWin is received anywhere in the MANET.  Updates this Agent's
     * state to track the data packet that was won in the provided BidWin.
     */
    private class BidWinReceivedMessage extends Message {
        private BidWin bidWin;

        public BidWinReceivedMessage(BidWin bidWin) {
            this.bidWin = bidWin;
        }

        @Override
        protected void processImpl() {
            if (currentPackets.containsKey(bidWin.getTransactionID())) {
                currentPackets.get(bidWin.getTransactionID()).updatePath(bidWin);
            }
            else {
                DataPath path = new DataPath(bidWin);
                currentPackets.put(bidWin.getTransactionID(), path);
                allPackets.put(bidWin.getTransactionID(), path);
                onPathInfoChanged();
            }

            // If we were the winner, and we have the advert in our recent auction map, add the auction and bidwin to our victory map.
            if (bidWin.getWinnerIP().equals(topologyAgent.getOurIP()) && recentAuctions.containsKey(bidWin.getTransactionID())) {
                if (ourWonAuctions.containsKey(bidWin.getTransactionID())) {
                    Log.w(TAG(), "We just won an auction for a packet we've already won an auction for! (transactionID = " + bidWin.getTransactionID() + ")");
                }

                if (recentAuctions.containsKey(bidWin.getTransactionID())) { // TODO: Figure out why this fails sometimes
                    ourWonAuctions.put(bidWin.getTransactionID(), Pair.make(recentAuctions.get(bidWin.getTransactionID()), bidWin));
                }
                else {
                    Log.w(TAG(), "We just won an auction we didn't even know about! (transactionID = " + bidWin.getTransactionID() + ")");
                    // TODO: Store all the bids we make and validate them against the auctions we win.
                }
            }
            else {
                Advert advert = getCurrentAdvert(bidWin.getTransactionID());
                if (advert != null) {
                    topologyAgent.getDevice(bidWin.getWinnerIP()).updateBiddingProfile(advert, bidWin.getWinningBid());
                }
            }

            Device device = topologyAgent.getDevice(bidWin.getWinnerIP());
            Device.AuctionInfo info = Predicates.findAny(device.getAuctionHistory().values(), new Predicate<Device.AuctionInfo>() {
                @Override
                public boolean apply(Device.AuctionInfo auctionInfo) {
                    // The same node could bid on the same package twice - we compare bid amounts to make sure its really the same bid.
                    return auctionInfo.getTransactionID() == bidWin.getTransactionID() && auctionInfo.getBidAmount() == bidWin.getWinningBid();
                }
            });
            if (info == null) {
                if (recentAuctions.containsKey(bidWin.getTransactionID())) {
                    Advert advert = recentAuctions.get(bidWin.getTransactionID());
                    info = device.new AuctionInfo(bidWin.getTransactionID(), bidWin.getWinningBid(), true, advert, topologyAgent.getLeastNumberOfHops(bidWin.getSourceIP(), advert.getFinalDestinationIP(), bidWin.getTransactionID()));
                }
                else {
                    info = device.new AuctionInfo(bidWin.getTransactionID(), bidWin.getWinningBid(), true);
                }
                device.getAuctionHistory().put(info.getTransactionID(), info);
            }
            else {
                info.setSuccessfulBid(true);
            }
            device.onDeviceInfoChanged();

            Device auctioningDevice = topologyAgent.getDevice(bidWin.getSourceIP());
            Device.AuctionInfo auctioningInfo = Predicates.findAny(auctioningDevice.getAuctionHistory().values(), new Predicate<Device.AuctionInfo>() {
                @Override
                public boolean apply(Device.AuctionInfo auctionInfo) {
                    return auctionInfo.getTransactionID() == bidWin.getTransactionID() && (auctionInfo.isSuccessfulBid() != null) && auctionInfo.isSuccessfulBid();
                }
            });
            if (auctioningInfo != null) {
                auctioningInfo.setNextHop(device);
                auctioningInfo.setNextHopBid(bidWin.getWinningBid());
                auctioningInfo.setDroppedPackage(false);
                if (auctioningInfo.complete()) {
                    auctioningDevice.updateAuctionProfile(auctioningInfo);
                }
                auctioningDevice.onDeviceInfoChanged();
            }
        }
    }

    /**
     * Called when a Bid is received, but only from a node adjacent to us.
     * Updates this Agent's state to store the bid.
     */
    private class BidReceivedMessage extends Message {
        private Bid bid;

        public BidReceivedMessage(Bid bid) {
            this.bid = bid;
        }

        @Override
        protected void processImpl() {
            Device device = topologyAgent.getDevice(bid.getSourceIP());
            if (pendingDeadPackets.contains(bid.getTransactionID())) {
                nodesBiddingOnDeadPacket.get(bid.getTransactionID()).add(device);
            }
            if (pendingExploits.contains(bid.getTransactionID())) {
                nodesBiddingOnExploit.get(bid.getTransactionID()).add(device);
            }
            if (currentAuctions.containsKey(bid.getTransactionID())) {
                Advert advert = currentAuctions.get(bid.getTransactionID()).getFirst();
                device.updateBiddingProfile(advert, bid.getBid());
                device.getAuctionHistory().put(bid.getTransactionID(), device.new AuctionInfo(bid.getTransactionID(), bid.getBid(),
                        advert, topologyAgent.getLeastNumberOfHops(bid.getSourceIP(), advert.getFinalDestinationIP(), bid.getTransactionID())));
                Device.AuctionInfo auctionInfo = topologyAgent.getDevice(advert.getSourceIP()).getAuctionHistory().get(bid.getTransactionID());
                if (auctionInfo != null) {
                    auctionInfo.addBid(bid);
                }
            }
            else {
                device.getAuctionHistory().put(bid.getTransactionID(), device.new AuctionInfo(bid.getTransactionID(), bid.getBid()));
            }
            device.onDeviceInfoChanged();
        }
    }


    private class AdvertReceivedMessage extends Message {
        private Advert advert;

        public AdvertReceivedMessage(Advert advert) {
            this.advert = advert;
        }

        @Override
        protected void processImpl() {
            if (currentPackets.containsKey(advert.getTransactionID())) {
                DataPath path = currentPackets.get(advert.getTransactionID());
                path.updatePath(advert);
            }
            else {
                DataPath path = new DataPath(advert);
                currentPackets.put(advert.getTransactionID(), path);
                allPackets.put(advert.getTransactionID(), path);
                onPathInfoChanged();
            }

            addToCurrentAuctions(advert);

            Device device = topologyAgent.getDevice(advert.getSourceIP());
            Device.AuctionInfo info = Predicates.findAny(device.getAuctionHistory().values(), new Predicate<Device.AuctionInfo>() {
                @Override
                public boolean apply(Device.AuctionInfo auctionInfo) {
                    return advert.getTransactionID() == auctionInfo.getTransactionID() && auctionInfo.isSuccessfulBid() != null && auctionInfo.isSuccessfulBid();
                }
            });
            if (info != null) {
                info.setNewBudget(advert.getCeil());
                info.setNewFine(advert.getFine());
                info.setDroppedPackage(false);
                device.onDeviceInfoChanged();
            }
        }
    }

    private void addToCurrentAuctions(final Advert advert) {
        synchronized (auctionMapLock) {
            if (currentAuctions.containsKey(advert.getTransactionID())) {
                Pair<Advert, DelayableTimerTask> currentAuctionPair = currentAuctions.get(advert.getTransactionID());
                DelayableTimerTask removeStaleAuctionTask = currentAuctionPair.getSecond();
                removeStaleAuctionTask.resetTimer();
                currentAuctions.put(advert.getTransactionID(), Pair.make(advert, removeStaleAuctionTask));
                recentAuctions.put(advert.getTransactionID(), currentAuctionPair.getFirst());
                DelayableTimerTask removeReallyStaleAuctions = new DelayableTimerTask(Mothership.getAuctionTimeout(), new Runnable() {
                    @Override
                    public void run() {
                        synchronized (auctionMapLock) {
                            recentAuctions.remove(advert.getTransactionID());
                        }
                    }
                });
                removeReallyStaleAuctions.start();
            }
            else {
                // It's especially important that we remove an auction after AUCTION_TIMEOUT, so that we don't use late bids
                // to update a node's bidding profile.  Such bids are probably late on purpose, and therefore mean nothing.
                DelayableTimerTask removeStaleAuctionTask = new DelayableTimerTask(Mothership.getAuctionTimeout(), new Runnable() {
                    @Override
                    public void run() {
                        synchronized (auctionMapLock) {
                            currentAuctions.remove(advert.getTransactionID());
                            recentAuctions.put(advert.getTransactionID(), advert);
                            DelayableTimerTask removeReallyStaleAuctions = new DelayableTimerTask(Mothership.getAuctionTimeout(), new Runnable() {
                                @Override
                                public void run() {
                                    synchronized (auctionMapLock) {
                                        recentAuctions.remove(advert.getTransactionID());
                                    }
                                }
                            });
                            removeReallyStaleAuctions.start();
                        }
                    }
                });
                removeStaleAuctionTask.start();
                currentAuctions.put(advert.getTransactionID(), Pair.make(advert, removeStaleAuctionTask));
            }
        }
    }

    private class AdvertSentMessage extends Message {
        private Advert advert;

        public AdvertSentMessage(Advert advert) {
            this.advert = advert;
        }

        @Override
        protected void processImpl() {
            if (currentPackets.containsKey(advert.getTransactionID())) {
                DataPath path = currentPackets.get(advert.getTransactionID());
                path.updatePath(advert);
            }
            else { // This probably should never happen, but best to be safe.
                DataPath path = new DataPath(advert);
                currentPackets.put(advert.getTransactionID(), path);
                allPackets.put(advert.getTransactionID(), path);
                onPathInfoChanged();
            }

            addToCurrentAuctions(advert);
            ourAuctions.put(advert.getTransactionID(), advert);
        }
    }

    private class BidSentMessage extends Message {
        private int transactionID;
        private int bid;

        public BidSentMessage(int transactionID, int bid) {
            this.transactionID = transactionID;
            this.bid = bid;
        }

        @Override
        protected void processImpl() {
            topologyAgent.getOurDevice().getAuctionHistory().put(transactionID, topologyAgent.getOurDevice().new AuctionInfo(transactionID, bid));
            Advert currentAdvert = getCurrentAdvert(transactionID);
            if (currentAdvert != null) {

            }
        }
    }

    private class AuctionFinishedMessage extends Message {
        private int transactionID;
        private Inet4Address winner;

        public AuctionFinishedMessage(int transactionID, Inet4Address winner) {
            this.transactionID = transactionID;
            this.winner = winner;
        }

        @Override
        protected void processImpl() {
            if (pendingDeadPackets.contains(transactionID)) {
                Set<Device> vulnerableNeighbors = nodesBiddingOnDeadPacket.get(transactionID);
                for (Device d : topologyAgent.getNeighbors()) {
                    d.modifyVulnerabilityToDeadPacketTrick(vulnerableNeighbors.contains(d));
                }
                pendingDeadPackets.remove(transactionID);
                nodesBiddingOnDeadPacket.remove(transactionID);
            }

            if (winner != null && !topologyAgent.getDevice(winner).isBackbone()) { // we did not drop the packet, or give it to the backbone.
                if (pendingExploits.contains(transactionID)) {
                    Set<Device> potentiallyVulnerableNeighbors = nodesBiddingOnExploit.get(transactionID);
                    Inet4Address destination = currentPackets.get(transactionID).getDestination();
                    for (Device d : topologyAgent.getNeighbors()) {
                        // We suspect that these nodes are less vulnerable to the trick, since they didn't bid, despite being adjacent to the destination
                        // but we can't yet say anything about the ones that did bid, because they might just want to drop the packet.
                        if (!potentiallyVulnerableNeighbors.contains(d) &&
                            topologyAgent.nodesAreAdjacent(d.getAddress(), destination))
                        {
                            d.modifyVulnerabilityToLowBudgetTrick(false);
                        }
                    }
                    nodesWinningOnExploit.put(transactionID, topologyAgent.getDevice(winner));
                }
            }

            pendingExploits.remove(transactionID);
            nodesBiddingOnExploit.remove(transactionID);
        }
    }

    private class PackageDroppedMessage extends Message {
        private int transactionID;

        public PackageDroppedMessage(int transactionID) {
            this.transactionID = transactionID;
        }

        @Override
        protected void processImpl() {
            if (currentPackets.containsKey(transactionID)) {
                currentPackets.get(transactionID).drop();
            }
        }
    }

    private class UseBackboneMessage extends Message {
        private int transactionID;

        public UseBackboneMessage(int transactionID) {
            this.transactionID = transactionID;
        }

        @Override
        protected void processImpl() {
            if (currentPackets.containsKey(transactionID)) {
                currentPackets.get(transactionID).toBackbone();
            }
        }
    }
}
