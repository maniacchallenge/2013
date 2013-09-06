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
