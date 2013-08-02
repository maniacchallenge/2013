package de.uni_bremen.comnets.maniac.agents;

import android.util.Log;

import org.apache.commons.collections15.Transformer;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.network_manager.NodePair;
import de.fu_berlin.maniac.network_manager.TopologyInfo;
import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.Bid;
import de.uni_bremen.comnets.maniac.devices.BiddingOpponent;
import de.uni_bremen.comnets.maniac.devices.Consumer;
import de.uni_bremen.comnets.maniac.devices.Device;
import de.uni_bremen.comnets.maniac.graphs.ClearableDirectedSparseGraph;
import de.uni_bremen.comnets.maniac.graphs.Connection;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;

/**
 * The TopologyAgent keeps the current network topology in a
 * graph data structure, and uses this to find information such
 * as the minimum number of hops to a destination node.
 *
 * This Agent also keeps track of the trustworthiness of each node,
 * to better analyze the probability of a successful delivery.
 *
 * Created by Isaac Supeene on 6/11/13.
 */
public class TopologyAgent extends Agent {


    public interface DeviceListChangedListener {
        public void onDeviceListChanged(List<Device> newList);
    }
    private List<DeviceListChangedListener> deviceListListeners = new ArrayList<DeviceListChangedListener>();

    // Public for synchronization (sorry xD).  The best thing to do would be to actually move all the code that's synchronized over this object into this class (Topology Agent).
    public final ClearableDirectedSparseGraph<Device, Connection> topology = new ClearableDirectedSparseGraph<Device, Connection>();
    private Map<Inet4Address, Device> devices = new ConcurrentHashMap<Inet4Address, Device>();

    private AuctionAgent auctionAgent;  public void setAuctionAgent(AuctionAgent agent) {
        auctionAgent = agent;
    }
    private HistoryAgent historyAgent;  public void setHistoryAgent(HistoryAgent agent) {
        historyAgent = agent;
    }

    private Inet4Address partnerAddress; public void setPartnerAddress(Inet4Address address) {
        if (partnerAddress != null) {
            getDevice(partnerAddress).onDeviceInfoChanged();
        }
        partnerAddress = address;
        getDevice(address).onDeviceInfoChanged();
    }

    public TopologyAgent() {
        postMessage(new UpdateTopologyMessage());
    }

    // Synchronized to avoid conflicting with removeDeviceChangedListener and getDevice, which iterates through the listeners
    public synchronized void addDeviceChangedListener(DeviceListChangedListener listener) {
        deviceListListeners.add(listener);
    }

    // Synchronized to avoid conflicting with addDeviceChangedListener and getDevice, which iterates through the listeners
    public synchronized void removeDeviceChangedListener(DeviceListChangedListener listener) {
        deviceListListeners.remove(listener);
    }

    private void notifyDeviceListeners() {
        for (DeviceListChangedListener l : deviceListListeners) {
            l.onDeviceListChanged(new ArrayList<Device>(devices.values()));
        }
    }

    // This is the only place we modify the device map - we synchronize the method so we can't have
    // two threads modifying it concurrently. TODO: Check if this negatively impacts performance - this method is called quite a bit.
    public synchronized Device getDevice(Inet4Address address) {
        Device result = devices.get(address);
        if (result == null) {
            result = new Device(address, historyAgent, this);
            devices.put(address, result);
            notifyDeviceListeners();
            topology.addVertex(result);
        }
        return result;
    }

    private Inet4Address ourIP;
    public Inet4Address getOurIP() {
        if (ourIP == null) {
            ourIP = (Inet4Address)TopologyInfo.getInterfaceIpv4("wlan0");
        }
        return ourIP;
    }

    public Device getOurDevice() {
        return getDevice(getOurIP());
    }

    public double getProbabilityOfSuccess(final Bid bid) {
        Device device = getDevice(bid.getSourceIP());
        AuctionParameters parameters = auctionAgent.getOptimalParameters(bid.getTransactionID());
        return device.getProbabilityOfSuccessAsConsumer(bid.getTransactionID(), parameters.getMaxbid(), parameters.getFine());
    }

    private boolean hasAlreadyWonPacket(Inet4Address address, int transactionID) {
        return historyAgent.getPacket(transactionID).contains(address);
    }

    public int getLeastNumberOfHops(final Inet4Address source, final Inet4Address destination, final int transactionID) {
        Device sourceDevice = getDevice(source);
        Device destinationDevice = getDevice(destination);
        synchronized (topology) {
            return getShortestPath(destination, transactionID, null).getPath(sourceDevice, destinationDevice).size();
        }
    }

    public DijkstraShortestPath<Device, Connection> getShortestPath(final Inet4Address destination, final int transactionID, final Device exception) {
        return new DijkstraShortestPath<Device, Connection>(topology, new Transformer<Connection, Integer>() {
            @Override
            public Integer transform(Connection connection) {
                if ((connection.getFirst().isBackbone() && !connection.getFirst().getAddress().equals(destination))     ||
                        (connection.getSecond().isBackbone() && !connection.getSecond().getAddress().equals(destination))   ||
                        (hasAlreadyWonPacket(connection.getSecond().getAddress(), transactionID) && !connection.getSecond().equals(exception)))
                {
                    return 1000;
                }
                else if (connection.getCost() > 2) {
                    return (int)Math.ceil(connection.getCost() / 2);
                }
                else {
                    return 1;
                }
            }
        });
    }

    /**
     * Gets a list of all neighbors of the specified node, including backbones.
     */
    public List<Device> getNeighbors(Inet4Address address) {
        return getNeighbors(getDevice(address));
    }

    public List<Device> getNeighbors(Device device) {
        synchronized (topology) {
            return new ArrayList<Device>(topology.getNeighbors(device));
        }
    }

    public List<Device> getNeighbors() {
        return getNeighbors(getOurIP());
    }

    public void prepareToComputeParameters(Advert advert) {
        postMessage(new PrepareForAdvert(advert));
    }

    public boolean nodesAreAdjacent(Inet4Address node1, Inet4Address node2) {
        synchronized (topology) {
            return topology.isNeighbor(getDevice(node1), getDevice(node2));
        }
    }

    public Inet4Address getPartnerIP() {
        return partnerAddress;
    }

    private void updateGraph() {
        synchronized (topology) {
            ArrayList<NodePair> newLinks;
            try {
                newLinks = TopologyInfo.getTopology();
            }
            catch (UnknownHostException ex) {
                Log.e(TAG(), "Encountered UnknownHostException when attempting to update the topology.");
                ex.printStackTrace();
                // TODO: Possibly notify the user.
                return;
            }

            topology.clear();
            for (NodePair pair : newLinks) {
                Device first = getDevice((Inet4Address)pair.getLastHopIP());
                Device second = getDevice((Inet4Address)pair.getDestinationIP());
                topology.addVertex(first);
                topology.addVertex(second);
                if (pair.getCost() < 20) {
                    topology.addEdge(new Connection(first, second, pair.getCost()), first, second);
                }
            }

            // Add free nodes that we know about, but don't have any connections for.
            for (Device d : devices.values()) {
                if (!topology.containsVertex(d)) {
                    topology.addVertex(d);
                }
            }

            notifyDeviceListeners();
        }
    }


    /* ******** *
     * Messages *
     * ******** */


    private class UpdateTopologyMessage extends Message {
        @Override
        protected void processImpl() {
            updateGraph();
        }
    }
    /**
     * Prepares two lists of adjacent devices, and provides them with all the information
     * required by the AuctionAgent to compute the optimal parameters.  Then, passes
     * these lists and the advert to the AuctionAgent and tells it to begin its computations.
     *
     * One list is a list of "Consumers", i.e. those nodes who will be bidding on our auction
     * if we win, and the other is a list of "BiddingOpponents", i.e. those nodes who will
     * be bidding on the same auction as we are.
     */
    private class PrepareForAdvert extends Message {
        Advert advert;

        public PrepareForAdvert(Advert advert) {
            this.advert = advert;
        }

        @Override
        protected void processImpl() {
            updateGraph();

            List<Device> adjacentDevices = getNeighbors();
            for (Iterator<Device> it = adjacentDevices.iterator(); it.hasNext();) {
                Device d = it.next();
                if (d.isBackbone()) {
                    it.remove();
                }
            }
            // TODO: Remove devices who are known to have already bid on this packet.

            List<Device> biddingOpponents = getNeighbors(advert.getSourceIP());
            for (Iterator<Device> it = biddingOpponents.iterator(); it.hasNext();) {
                Device d = it.next();
                if (d.isBackbone() || d == getOurDevice() || d.getAddress().equals(getPartnerIP())) {
                    it.remove();
                }
            }
            // TODO: Remove devices who are known to have already bid on this packet.

            // We are always using the number of hops that will remain when the node RECEIVES the data!
            for (Device d : adjacentDevices) {
                d.prepareAsConsumer(advert.getTransactionID(), advert.getDeadline() - 2, advert.getFinalDestinationIP(), advert);
            }

            for (Device d : biddingOpponents) {
                d.prepareAsBiddingOpponent(advert.getTransactionID(), advert.getDeadline() - 1, advert.getFinalDestinationIP(), advert.getCeil(), advert.getFine());
            }

            auctionAgent.computeParameters(advert, new ArrayList<Consumer>(adjacentDevices), new ArrayList<BiddingOpponent>(biddingOpponents));
        }
    }
}
