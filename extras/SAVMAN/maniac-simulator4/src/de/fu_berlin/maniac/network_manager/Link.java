package de.fu_berlin.maniac.network_manager;

import java.net.InetAddress;

/**
 * A link represents a direct connection between two nodes.
 * 
 */
public class Link {
	InetAddress ip;
	Float cost;

	/**
	 * 
	 * @param ip
	 *            The InetAddress of the node this link refers to
	 * @param cost
	 *            The 'costs' of the links represents the link quality. 1.00 is
	 *            the best value, higher is worse.
	 */
	public Link(InetAddress ip, Float cost) {
		this.ip = ip;
		this.cost = cost;

	}

	public InetAddress getIp() {
		return ip;
	}

	public Float getCost() {
		return cost;
	}

}
