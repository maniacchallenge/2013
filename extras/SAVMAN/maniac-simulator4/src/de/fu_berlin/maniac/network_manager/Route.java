package de.fu_berlin.maniac.network_manager;

import java.net.InetAddress;

/**
 * A route object indicates that there is some route from this node to another
 * node.
 * 
 * @author maniacchallenge
 * 
 */
public class Route {

	InetAddress destination;
	InetAddress gateway;
	Float link_quality;

	/**
	 * @param dest
	 *            The InetAddress of the node the route is to.
	 * @param gw
	 *            The InetAddress of the gateway node which leads to the
	 *            destinationen route.
	 * @param etx
	 *            The link Quality. 1.00 is the best value, higher is worse.
	 */
	public Route(InetAddress dest, InetAddress gw, Float etx) {

		this.destination = dest;
		this.gateway = gw;
		this.link_quality = etx;

	}

	public InetAddress getDestination() {
		return destination;
	}

	public InetAddress getGateway() {
		return gateway;
	}

	public Float getLinkQuality() {
		return link_quality;
	}

}
