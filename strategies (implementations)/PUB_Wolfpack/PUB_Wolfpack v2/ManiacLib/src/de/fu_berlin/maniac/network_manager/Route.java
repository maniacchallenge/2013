/**
 * This file is part of the API for the Maniac Challenge 2013.
 *
 * The Maniac API is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Maniac API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

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
