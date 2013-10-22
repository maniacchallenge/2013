package de.fu_berlin.maniac.packet_builder;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The Data that is being forwarded through the mesh network.
 *
 * @author maniacchallenge
 *
 */
public class Data extends Packet {

          private final int HEADER_SZ = 21;
          // protected final char type = 'D';
          private Inet4Address finalDestination; // not the next hop but rather the
          // end node

          public Inet4Address getFinalDestination() {
	         return finalDestination;
          }
          private int hopCount; // hops left
          private int dataLength;
          private byte[] data;
          private int fine;
          private int initialBudget;         // initial budget for the transmission
          // (also the fine for retransmission via BBN)

          // create Data-Packet from raw data & automatically decr. hop count.
          public Data(byte[] rawdata) {
	         parseFromByteArray(rawdata);
          }

          // this exists only for testing purposes. TODO deleteme
          public Data(int transactionID, int hopCount, int fine, byte[] data,
	       Inet4Address finalDestination, int initialBudget) {
	         this.transactionID = transactionID;
	         this.hopCount = hopCount;
	         this.data = data;
	         this.finalDestination = finalDestination;
	         this.type = 'D';
	         this.fine = fine;
	         this.initialBudget = initialBudget;
          }

          /**
           * @return the number of hops this Data packet has traveled so far
           */
          public int getHopCount() {
	         return this.hopCount;
          }

          /**
           * @return the fine for the failed delivery of this Data packet
           */
          public int getFine() {
	         return this.fine;
          }

          /**
           *
           * @return the initial budget for the transmission. This also the fine for retransmission via a Backbone
           * router instead of a Node.
           */
          public int getInitialBudget() {
	         return this.initialBudget;
          }

          // Should this really be public?! TODO
          protected void setFine(int fine) {
	         this.fine = fine;
          }

          @Override
          protected DatagramPacket getDatagramPacket() {
	         byte[] payload = parseToByteArray();
	         return new DatagramPacket(payload, payload.length, this.destinationIP,
		      PACKET_PORT);
          }

          private void parseFromByteArray(byte[] payload) {
	         this.transactionID = loadFromByteArray(payload, 1); // Transaction ID

	         // Final destination address
	         byte[] address = new byte[4];
	         for (int i = 0; i < 4; i++) {
		        address[i] = payload[5 + i];
		        //System.err.println("===> " + address[i]);
	         }
	         try {
		        this.finalDestination = (Inet4Address) InetAddress
			     .getByAddress(address);
	         } catch (UnknownHostException e) {
		        System.err.println("Error parsing Data from Byte array");
		        //this.destinationIP = null;
		        e.printStackTrace();
	         }

	         this.hopCount = loadFromByteArray(payload, 9) - 1; // get & decrease hop
	         // count

	         this.data = new byte[loadFromByteArray(payload, 13)]; // get data length
	         this.fine = loadFromByteArray(payload, 17);

	         // load the data
	         int len = Math.min(data.length, payload.length - 13);

	         for (int i = 0; i < len; i++) {
		        this.data[i] = payload[21 + i];
	         }
	         this.type = (char) payload[0];
          }

          private byte[] parseToByteArray() {
	         int payloadLength = HEADER_SZ + dataLength;
	         byte[] payload = new byte[payloadLength];

	         // Packet type
	         payload[0] = (byte) type;

	         saveToByteArray(this.transactionID, payload, 1); // Insert transaction
	         // ID
	         // shouldn't this throw an exception or something? (lotte) TODO
	         if (this.finalDestination == null) {
		        System.err.println("error: final destination not defined!");
	         } else {
		        byte[] address = this.finalDestination.getAddress();
		        //System.err.println("===> assgining finalDestination as:" + this.finalDestination.getAddress());
		        for (int i = 0; i < 4; i++) {
			       payload[5 + i] = address[i];   
			       //System.err.println("===> " + address[i]);
		        }
		  
	         }

	         saveToByteArray(this.hopCount, payload, 9); // set hop count
	         saveToByteArray(dataLength, payload, 13); // set data length
	         saveToByteArray(this.fine, payload, 17);

	         // set data
	         for (int i = 0; i < dataLength; i++) {
		        payload[21 + i] = data[i];
	         }

	         return payload;
          }

          @Override
          public String toString() {
	         return "DATA \n transactionID: " + this.transactionID + "\n SourceIP:"
		      + this.sourceIP + "" + "\n destinationIP:" + this.destinationIP
		      + "\n finalDestination: " + this.finalDestination
		      + "\n hopCount: " + this.hopCount + "\n data: " + this.data;
          }
}
