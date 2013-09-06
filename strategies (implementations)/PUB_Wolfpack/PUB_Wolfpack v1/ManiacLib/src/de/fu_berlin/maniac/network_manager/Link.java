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
