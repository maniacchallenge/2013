package de.fu_berlin.maniac.network_manager;

import java.net.InetAddress;
/**
 * Node pairs represent information about the whole mesh network. Single pairs show known relations between nodes in the network.
 * For more information please refer to the OLSRd documentation, since this is just our representation of the network info the olsr daemon
 * spits out with the txtinfo plugin
 * @author ponken
 *
 */
public class NodePair {
	InetAddress destinationIP;

	InetAddress lastHopIP;
	Float cost;
	
	public NodePair(InetAddress dest, InetAddress lastHop,Float cost){
		this.destinationIP = dest;
		this.lastHopIP = lastHop;
		this.cost = cost;
	}
	public InetAddress getDestinationIP() {
		return destinationIP;
	}
	
	public InetAddress getLastHopIP() {
		return lastHopIP;
	}
	
	public Float getCost() {
		return cost;
	}
	
}
