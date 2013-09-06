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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

import net.commotionwireless.olsrinfo.TxtInfo;

/**
 * This class just passes on the topology informationen given by the olsrd
 * txt-plugin. If you want to look at that for clarification go to MANET Manager
 * -> routing info on your nexus. WARNING: This info might not always be
 * correct, as in the olsrd txtinfo output sometimes does not pick up on things.
 * 
 * @author maniacchallenge
 * 
 */
public class TopologyInfo {

	static TxtInfo txtinfo = new TxtInfo();
	static InetAddress ownIP;
	static String intf;
	static Inet4Address broadCastAddr;

	private TopologyInfo() {

	}

	/**
	 * Returns the InetAddress of the interface specified.
	 * 
	 * @param intf
	 *            In the MANIAC Challenge this will almost always be "wlan0".
	 * @return Returns the InetAddress associated with the interface intf
	 */
	public static InetAddress getInterfaceIpv4(String iface) {

		if(ownIP == null || intf != iface){
			
			Enumeration<NetworkInterface> interfaces;
			
			try {
				interfaces = NetworkInterface.getNetworkInterfaces();
			} catch (SocketException e) {
				e.printStackTrace();
				return null;
			}
			
			NetworkInterface current;
			
			while (interfaces.hasMoreElements()) {
				current = interfaces.nextElement();
				
				
				if (current.getName().equalsIgnoreCase(iface)) {
					
					for (InterfaceAddress interfaceAddress :
						current.getInterfaceAddresses()) {
						TopologyInfo.broadCastAddr = (Inet4Address)interfaceAddress.getBroadcast();
						if (TopologyInfo.broadCastAddr == null)
							continue;
					}
					
					
					Enumeration<InetAddress> addresses = current.getInetAddresses();
					while (addresses.hasMoreElements()) {
						
						InetAddress current_addr = addresses.nextElement();
						if (current_addr instanceof Inet4Address){
							ownIP = current_addr;
							intf = iface;
						}
					}
				}
			}
		}
		
		return ownIP;
		
	}

	/**
	 * 
	 * @param ip
	 *            Any IP address from the network can be passed to this function
	 * @return Returns the Broadcast InetAddress for the network the given
	 *         InetAddress is in. For ip = 192.168.1.12 this would be
	 *         192.168.1.255 for example.
	 */
	public static InetAddress getBroadCastAddress(InetAddress ip) {

		if (ip == null)
			ip = getInterfaceIpv4("wlan0");

		
		return TopologyInfo.broadCastAddr;
		/*
		try {
			byte[] byte_addr = ip.getAddress();
			byte_addr[3] = -1;
			return InetAddress.getByAddress(byte_addr);
		} catch (UnknownHostException e) {
			System.err.println("could not find broadcast address:");
			return null;
		} catch (NullPointerException E) {
			System.err
					.println("could not find broadcast address. Remember to start the MANET manager.");
			return null;
		}
		*/

	}

	/**
	 * A link is a node that is directly reachable from this node.
	 * 
	 * @return A list of all directly reachable nodes
	 * @throws UnknownHostException
	 */
	public static ArrayList<Link> getLinks() throws UnknownHostException {

		ArrayList<Link> links = new ArrayList<Link>();

		String[][] neighbours_string = txtinfo.links();

		Float cost;

		String[] neighbour;

		// TODO Test this!
		// Note to Lotte: We need POSITIVE_INFINITY here, because findBestBbn()
		// would choose a link with infinite cost as "best" otherwise, as it has
		// the lowest cost of all ;)
		for (int i = 0; i < neighbours_string.length; i++) {
			cost = Float.POSITIVE_INFINITY;
			neighbour = neighbours_string[i];
			if (!neighbour[5].equals("INFINITE")) {
				cost = Float.valueOf(neighbour[5]);
				links.add(new Link(InetAddress.getByName(neighbour[1]), cost));
			}
		}

		return links;
	}

	/**
	 * 
	 * @return Returns a list of all nodes reachable with 2 Hops.
	 * @throws UnknownHostException
	 */
	public static ArrayList<TwoHopNeighbour> getTwoHopNeighbours()
			throws UnknownHostException {
		ArrayList<TwoHopNeighbour> twohopneighbours = new ArrayList<TwoHopNeighbour>();
		String[][] neighbours_string = txtinfo.twohop();
		InetAddress ip;
		InetAddress hop;
		Boolean sym;

		for (int i = 1; i < neighbours_string.length; i++) {
			ip = InetAddress.getByName(neighbours_string[i][0]);
			if (neighbours_string[i][1].equals("YES")) {
				sym = true;
			} else {
				sym = false;
			}
			hop = InetAddress.getByName(neighbours_string[i][6]);

			twohopneighbours.add(new TwoHopNeighbour(ip, sym, hop));
		}

		return twohopneighbours;

	}

	/**
	 * 
	 * @return Returns a list of all routes found. See Route for more info.
	 * @throws UnknownHostException
	 */
	public static ArrayList<Route> getRoutes() throws UnknownHostException {
		ArrayList<Route> routes = new ArrayList<Route>();
		String[][] routes_string = txtinfo.routes();
		InetAddress dest;
		InetAddress gateway;
		Float etx;
		String dest_ip;

		for (int i = 0; i < routes_string.length; i++) {
			dest_ip = routes_string[i][0];
			dest = InetAddress.getByName(dest_ip.substring(0,
					dest_ip.length() - 3));
			gateway = InetAddress.getByName(routes_string[i][1]);
			
			etx = Float.POSITIVE_INFINITY;
			
			if (!routes_string[i][3].equals("INFINITE")) {
				etx = Float.valueOf(routes_string[i][3]);
			}
			
			routes.add(new Route(dest, gateway, etx));
		}
		return routes;
	}
	
	/**
	 * 
	 * @return Returns a list of all NodePairs found. See NodePair for more Info.
	 * @throws UnknownHostException
	 */
	public static ArrayList<NodePair> getTopology() throws UnknownHostException{
		ArrayList<NodePair> top = new ArrayList<NodePair>();
		String[][] pairs = txtinfo.topology();
		InetAddress dest;
		InetAddress lastHop;
		Float cost;
		
		for (int i = 0; i<pairs.length;i++){
			dest = InetAddress.getByName(pairs[i][0]);
			lastHop = InetAddress.getByName(pairs[i][1]);
			cost = Float.POSITIVE_INFINITY;
			if(!pairs[i][4].equals("INFINITE")){
				cost = Float.valueOf(pairs[i][4]);
			}
			
			top.add(new NodePair(dest, lastHop, cost));
			
		}
		
		return top;
		
	}

}