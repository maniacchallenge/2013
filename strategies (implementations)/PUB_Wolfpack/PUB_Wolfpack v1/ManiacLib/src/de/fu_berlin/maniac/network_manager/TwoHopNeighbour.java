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
 * A 2-hop neighbour is a node that is reachable in 2 or less hops from this
 * node.
 * 
 * @author maniacchallenge
 * 
 */
public class TwoHopNeighbour {
	InetAddress neighbour_ip;
	Boolean sym;
	InetAddress hop_ip;

	/**
	 * @param iadd
	 *            The Inetaddress of the 2-hop neighbour. A 2-Hop neighbour is
	 *            any node that is reachable via max. 2 hops. That means that
	 *            links also count as 2-hop neighbours!
	 * @param sym
	 *            Indicates if this link is symmetric, i.e. if the neighbour
	 *            also 'sees' you in the network
	 * @param hop
	 *            The Inetaddress of the node between this node and the two hop
	 *            neighbour
	 */
	public TwoHopNeighbour(InetAddress iadd, Boolean sym, InetAddress hop) {
		this.neighbour_ip = iadd;
		this.sym = sym;
		this.hop_ip = hop;
	}

	public InetAddress getNeighbourIp() {
		return neighbour_ip;
	}

	public Boolean getSym() {
		return sym;
	}

	public InetAddress getHopIp() {
		return hop_ip;
	}

}
