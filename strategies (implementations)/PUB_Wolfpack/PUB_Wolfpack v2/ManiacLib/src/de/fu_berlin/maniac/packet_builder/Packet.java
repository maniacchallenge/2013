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

package de.fu_berlin.maniac.packet_builder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;

/**
 * The superclass for all Packet types.
 * 
 * @author maniacchallenge
 * 
 */

public abstract class Packet {
	protected char type;
	protected int transactionID;

	// immediate source of the incoming packets
	protected Inet4Address sourceIP;
	protected Inet4Address destinationIP;
	protected Inet4Address broadcastAddr;
	protected final int PACKET_PORT = 8765;

	/**
	 * @return which kind of packet the object is. A : Advert, B : Bid, W :
	 *         BidWinm D : Data or C : Check (i.e. Bank Data)
	 */
	public char getType() {
		return type;
	}

	/**
	 * @return the transactionID of the transaction the packet belongs to.
	 */
	public int getTransactionID() {
		return transactionID;
	}

	// this ID is set by the PacketBuilder
	protected void setTransactionID(int id) {
		this.transactionID = id;
	}

	/**
	 * @return the last hop the packet has traveled (is this correct?) TODO
	 */
	public Inet4Address getSourceIP() {
		return sourceIP;
	}

	// this is set by the PacketBuilder
	protected void setSourceIP(Inet4Address sourceIP) {
		this.sourceIP = sourceIP;
	}

	/**
	 * @return the next hop of the packet (is this correct?) TODO
	 */
	public Inet4Address getDestinationIP() {
		return destinationIP;
	}

	// this is set by the PacketBuilder
	protected void setDestinationIP(Inet4Address destinationIP) {
		this.destinationIP = destinationIP;
	}

	protected void setBroadcastAddr(Inet4Address broadcastAddr) {
		this.broadcastAddr = broadcastAddr;
	}

	// wrap Packet's data into a DatagramPacket
	protected DatagramPacket getDatagramPacket() throws IOException {
		return null;
	}

	/**
	 * Get Data ready to be sent via TCP
	 * @return the Packet, packed into a byte[]
	 */
	protected byte[] getStreamableData() {
		return null;
	}

}
