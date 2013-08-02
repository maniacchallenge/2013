package de.uni_bremen.comnets.maniac.devices;

import android.util.Log;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fu_berlin.maniac.bank.BankManager;
import de.fu_berlin.maniac.network_manager.NetworkManager;
import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.Bid;
import de.uni_bremen.comnets.maniac.agents.HistoryAgent;
import de.uni_bremen.comnets.maniac.agents.TopologyAgent;
import de.uni_bremen.comnets.maniac.util.Function2Var;

/**
 * Created by Isaac Supeene on 6/13/13.
 */
public class Device implements BiddingOpponent, Consumer {
    private static final String TAG = "Maniac Device";

    private List<DeviceInfoChangedListener> deviceInfoListeners = new ArrayList<DeviceInfoChangedListener>(); // Alert the listeners every time something important happens.

    public class AuctionInfo {
        private int transactionID;                      // onRcvBid or onRcvBidWin
        private Advert originalAdvert;                  // onRcvBid or onRcvBidWin, if we have the advert already stored.
        private Integer hopsFromDestination;            // onRcvBid or onRcvBidWin, if we have the advert already stored.
        private Integer bidAmount;                      // onRcvBid or onRcvBidWin
        private Boolean successfulBid;                  // onRcvBidWin
        private Boolean droppedPackage;                 // data path terminated timer task or onRcvAdvert or onRcvBidWin
        private Integer newFine;                        // onRcvAdvert or onRcvBidWin
        private Integer newBudget;                      // onRcvAdvert
        private List<Bid> bids = new ArrayList<Bid>();  // onRcvBid and onRcvAdvert
        private Device nextHop;                         // onRcvBidWin
        private Integer nextHopBid;                     // onRcvBidWin

        public boolean complete() {
            return originalAdvert != null &&
                   hopsFromDestination != null &&
                   bidAmount != null &&
                   successfulBid != null &&
                   droppedPackage != null &&
                   newFine != null &&
                   newBudget != null &&
                   nextHop != null &&
                   nextHopBid != null;
        }

        public AuctionInfo(int transactionID, int bidAmount) {
            this.transactionID = transactionID;
            this.bidAmount = bidAmount;
        }

        public AuctionInfo(int transactionID, int bidAmount, Advert originalAdvert, int hopsFromDestination) {
            this(transactionID, bidAmount);
            this.originalAdvert = originalAdvert;
            this.hopsFromDestination = hopsFromDestination;
        }

        public AuctionInfo(int transactionID, int bidAmount, boolean successfulBid) {
            this(transactionID, bidAmount);
            this.successfulBid = successfulBid;
        }

        public AuctionInfo(int transactionID, int bidAmount, boolean successfulBid, Advert originalAdvert, int hopsFromDestination) {
            this(transactionID, bidAmount, successfulBid);
            this.originalAdvert = originalAdvert;
            this.hopsFromDestination = hopsFromDestination;
        }

        public int getTransactionID() {
            return transactionID;
        }

        public Advert getOriginalAdvert() {
            return originalAdvert;
        }

        public int getHopsFromDestination() {
            return hopsFromDestination;
        }

        public int getBidAmount() {
            return bidAmount;
        }

        public Boolean isSuccessfulBid() {
            return successfulBid;
        }

        public void setSuccessfulBid(boolean successfulBid) {
            this.successfulBid = successfulBid;
            if (successfulBid) {
                wonAuctionHistory.put(transactionID, this);
            }
        }

        public Boolean isDroppedPackage() {
            return droppedPackage;
        }

        public void setDroppedPackage(boolean droppedPackage) {
            this.droppedPackage = droppedPackage;
        }

        public Integer getNewFine() {
            return newFine;
        }

        public void setNewFine(Integer newFine) {
            this.newFine = newFine;
        }

        public Integer getNewBudget() {
            return newBudget;
        }

        public void setNewBudget(Integer newBudget) {
            this.newBudget = newBudget;
        }

        public List<Bid> getBids() {
            return bids;
        }

        public void addBid(Bid bid) {
            bids.add(bid);
        }

        public Device getNextHop() {
            return nextHop;
        }

        public void setNextHop(Device nextHop) {
            this.nextHop = nextHop;
        }

        public Integer getNextHopBid() {
            return nextHopBid;
        }

        public void setNextHopBid(int nextHopBid) {
            this.nextHopBid = nextHopBid;
        }
    }
    private Map<Integer, AuctionInfo> auctionHistory = new HashMap<Integer, AuctionInfo>();
    public Map<Integer, AuctionInfo> getAuctionHistory() {
        return auctionHistory;
    }
    private Map<Integer, AuctionInfo> wonAuctionHistory = new HashMap<Integer, AuctionInfo>(); // TODO: keep this up-to-date!
    public AuctionInfo getWonAuctionInfo(Integer transactionID) {
        return wonAuctionHistory.get(transactionID);
    }
    public void onDeviceInfoChanged() {
        for (DeviceInfoChangedListener listener : deviceInfoListeners) {
            listener.onDeviceInfoChanged();
        }
    }

    private Inet4Address address;
    private HistoryAgent historyAgent;      public HistoryAgent getHistoryAgent() {
        return historyAgent;
    }
    private TopologyAgent topologyAgent;    public TopologyAgent getTopologyAgent() {
        return topologyAgent;
    }
    private int balance = 0;
    private Boolean backbone;

    private double vulnerabilityToLowBudgetTrick = 0.75; // TODO: 0.75 is pretty arbitrary...
    private double vulnerabilityToDeadPacketTrick = 0.5; // TODO: 0.75 is pretty arbitrary...

    // We distinguish between as consumer and as opponent, to distinguish between values for the
    // auction that we are holding, and the auction that we and this node are both bidding on.

    private Map<Integer, Function2Var<Integer, Integer, Double>> probabilitiesOfNoBidAsConsumer = new HashMap<Integer, Function2Var<Integer, Integer, Double>>();
    private Map<Integer, Function2Var<Integer, Integer, Double>> probabilitiesOfSuccessAsConsumer = new HashMap<Integer, Function2Var<Integer, Integer, Double>>();
    private Map<Integer, Function2Var<Integer, Integer, Integer>> probableBidsAsConsumer = new HashMap<Integer, Function2Var<Integer, Integer, Integer>>();

    private Map<Integer, NormalDistribution> probableBidsAsOpponent = new HashMap<Integer, NormalDistribution>();
    // TODO: Maybe we also want probability of success as opponent

    private BiddingProfile biddingProfile = new BiddingProfile(this); // An estimation of how this node chooses its bids.
    private ReliabilityProfile reliabilityProfile = new ReliabilityProfile(this); // An estimation of how this node chooses to drop packets.
    private AuctionProfile auctionProfile = new AuctionProfile(this);

    public BiddingProfile getBiddingProfile() {
        return biddingProfile;
    }

    public ReliabilityProfile getReliabilityProfile() {
        return reliabilityProfile;
    }

    public AuctionProfile getAuctionProfile() {
        return auctionProfile;
    }

    public Device(Inet4Address address, HistoryAgent historyAgent, TopologyAgent topologyAgent) {
        if (address == null) {
            Log.wtf(TAG, "Someone tried to create a device with a null address!!!");
            throw new IllegalArgumentException("No null device addresses allowed!!!");
        }
        this.address = address;
        this.historyAgent = historyAgent;
        this.topologyAgent = topologyAgent;
    }

    public void addDeviceInfoListener(DeviceInfoChangedListener listener) {
        deviceInfoListeners.add(listener);
    }

    public void removeDeviceInfoListener(DeviceInfoChangedListener listener) {
        deviceInfoListeners.remove(listener);
    }

    public Inet4Address getAddress() {
        return address;
    }

    public String getName() {
        return "Device " + address.toString().substring(1); // TODO: more memorable name?
    }

    public double getVulnerabilityToLowBudgetTrick() {
        return vulnerabilityToLowBudgetTrick;
    }

    public void modifyVulnerabilityToLowBudgetTrick(boolean success) {
        // Just cut in half the probability of the node making a different decision.
        // This also happens to have the property that we will not try to pull that
        // trick on the same node more than once (given the arbitrary vulnerability
        // threshold of .5). TODO: See if there's a better way of doing this?
        if (success) {
            // Node is vulnerable!
            vulnerabilityToLowBudgetTrick = 1 - (1 - vulnerabilityToLowBudgetTrick)/2;
        }
        else {
            vulnerabilityToLowBudgetTrick /= 2;
        }
    }

    public boolean isVulnerableToLowBudgetTrick() {
        return getVulnerabilityToLowBudgetTrick() > 0.5; // TODO: 0.5 is pretty arbitrary... it's just less than the default, 0.75
    }

    public double getVulnerabilityToDeadPacketTrick() {
        return vulnerabilityToDeadPacketTrick;
    }

    // TODO: Modify the vulnerability to the trick based on observations from other nodes' auctions, and not just our own.
    public void modifyVulnerabilityToDeadPacketTrick(boolean success) {
        // see modifyVulnerabilityToLowBudgetTrick
        if (success) {
            vulnerabilityToDeadPacketTrick = 1 - (1 - vulnerabilityToDeadPacketTrick)/2;
        }
        else {
            vulnerabilityToDeadPacketTrick /= 2;
        }
        onDeviceInfoChanged();
    }

    public boolean isVulnerableToDeadPacketTrick() {
        return getVulnerabilityToDeadPacketTrick() > 0.6; // TODO: 0.6 is pretty arbitrary... it's just more than the default, 0.5
    }

    public double getProbabilityOfHigherBidAsOpponent(Advert advert, int bid) {
        if (probableBidsAsOpponent.containsKey(advert.getTransactionID())) {
            return 1 - probableBidsAsOpponent.get(advert.getTransactionID()).cumulativeProbability(bid);
        }
        else {
            return ((double)bid)/advert.getCeil(); // Just assume a random linear distribution.
        }
    }

    public int getMostProbableBidAsConsumer(int transactionID, int budget, int fine) {
        if (probableBidsAsConsumer.containsKey(transactionID)) {
            return (int)probableBidsAsConsumer.get(transactionID).evaluate(budget, fine);
        }
        else {
            Log.w(TAG, "Most probable bid as consumer for transaction with ID " + transactionID + " not found. Returning budget/2.");
            return budget/2; // TODO: consider a smarter default;
        }
    }

    public double getProbabilityOfSuccessAsConsumer(int transactionID, int budget, int fine) {
        return probabilitiesOfSuccessAsConsumer.get(transactionID).evaluate(budget, fine);
    }

    public double getProbabilityOfFailureAsConsumer(int transactionID, int budget, int fine) {
        return 1 - getProbabilityOfSuccessAsConsumer(transactionID, budget, fine) - getProbabilityOfNoBidAsConsumer(transactionID, budget, fine);
    }

    public double getProbabilityOfNoBidAsConsumer(int transactionID, int budget, int fine) {
        return probabilitiesOfNoBidAsConsumer.get(transactionID).evaluate(budget, fine);
    }

    public double getProbabilityOfChoosingOurBid(int bid, Advert advert, List<BiddingOpponent> biddingOpponents) {
        return auctionProfile.getProbabilityOfChoosingOurBid(bid, advert, biddingOpponents);
    }

    public boolean isBackbone() {
        if (backbone == null) {
            Log.d(TAG, "Checking if device " + this + " is a backbone");
            List<InetAddress> backbones = NetworkManager.getInstance().getBackbones();
            Log.d(TAG, "List of backbones:");
            if (backbones.size() == 0) {
                Log.d(TAG, "Empty");
            }
            else {
                for (InetAddress address : backbones) {
                    Log.d(TAG, address.toString().substring(1));
                }
            }
            backbone = NetworkManager.getInstance().getBackbones().contains(address);
        }
        return backbone;
    }

    public void prepareAsBiddingOpponent(int transactionID, int hopsRemaining, Inet4Address destination, int budget, int fine) {
        probableBidsAsOpponent.put(transactionID, biddingProfile.getBiddingDistribution(hopsRemaining, destination, budget, fine, transactionID));
    }

    public void prepareAsConsumer(int transactionID, int hopsRemaining, Inet4Address destination, Advert advert) {
        probabilitiesOfNoBidAsConsumer.put(transactionID, biddingProfile.getProbabilityOfNoBidFunction(hopsRemaining, destination, transactionID));
        probableBidsAsConsumer.put(transactionID, biddingProfile.getMostProbableBidFunction(hopsRemaining, destination, transactionID));
        probabilitiesOfSuccessAsConsumer.put(transactionID, reliabilityProfile.getProbabilityOfSuccessFunction(hopsRemaining, destination, transactionID, advert));
    }

    public int getBalance() {
        if (this == topologyAgent.getOurDevice()) {
            return BankManager.getBalance();
        }
        else {
            return balance;
        }
    }

    public void addBalance(int change) {
        balance += change;
        onDeviceInfoChanged();
    }

    public void updateReliabilityProfile(int transactionID, boolean success, HistoryAgent.DataPath.Hop ourHop) {
        reliabilityProfile.update(transactionID, success, ourHop);
        onDeviceInfoChanged();
    }

    public void updateBiddingProfile(Advert advert, int bid) {
        biddingProfile.update(advert, bid);
        onDeviceInfoChanged();
    }

    public void updateAuctionProfile(AuctionInfo auctionInfo) {
        auctionProfile.update(auctionInfo);
        onDeviceInfoChanged();
    }

    public interface DeviceInfoChangedListener {
        public void onDeviceInfoChanged();
    }

    @Override
    public String toString() {
        return address.toString().substring(1);
    }

    // Auto-generated methods.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Device device = (Device) o;

        if (address != null ? !address.equals(device.address) : device.address != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }
}
