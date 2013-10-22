package de.fu_berlin.maniac.packet_builder;

/**
 * Checks are sent to nodes over the TCP connection between a node and their
 * backbone router every time a node's bank account changes.
 * 
 * @author maniacchallenge
 * 
 */
public class Check extends Packet {

	private int amount;
	private int newBalance;

	protected Check(int transactionID, int amount, int newBalance) {
		this.transactionID = transactionID;
		this.amount = amount;
		this.newBalance = newBalance;
	}

	protected Check(String rawData) {
		System.out.println("new check: " + rawData);

		// make sure to only handle one check at a time
		String[] checks = rawData.split(" $");
		int numberOfChecks = checks.length;
		for (int i = 0; i < numberOfChecks; i++) {
			// parse check
			String[] data = checks[i].split(" ");
			// omit the 'c' at data[0]
			this.transactionID = Integer.parseInt(data[1]);
			this.amount = Integer.parseInt(data[2]);
			this.newBalance = Integer.parseInt(data[3]);
		}
	}

	/**
	 * @return the amount which has just been added to or subtracted from your
	 *         bank account.
	 */
	public int getAmount() {
		return this.amount;
	}

	/**
	 * @return the new balance of your bank account
	 */
	public int getNewBalance() {
		return this.newBalance;
	}

	// TODO sollten hier evtl noch Source- & DestinationIP rein? ZUr
	// uebersichtlichkeit?
	@Override
	public String toString() {
		return "CHECK \n transactionID: " + this.transactionID + "\n amount:"
				+ this.amount + "\n newbalance: " + this.newBalance;
	}

	/*
	 * public Check(byte[] array) { int transID = ((int)array[4]) << 24; transID
	 * += ((int)array[3]) << 16; transID += ((int)array[2]) << 8; transID +=
	 * (int)array[1]; this.transID = transID; int amount = ((int)array[8]) <<
	 * 24; amount += ((int)array[7]) << 16; amount += ((int)array[6]) << 8;
	 * amount += (int)array[5]; this.difference = amount; int balance =
	 * ((int)array[12]) << 24; balance += ((int)array[11]) << 16; balance +=
	 * ((int)array[10]) << 8; balance += (int)array[9]; this.newBalance =
	 * balance; }
	 * 
	 * public byte[] parseToByteArray() { byte[] bytearray = new byte[13];
	 * bytearray[0] = 'C'; bytearray[1] = (byte) ((this.transactionID << 24) >>
	 * 24); bytearray[2] = (byte) ((this.transactionID << 16) >> 24);
	 * bytearray[3] = (byte) ((this.transactionID << 8) >> 24); bytearray[4] =
	 * (byte) (this.transactionID >> 24); bytearray[5] = (byte) ((this.amount <<
	 * 24) >> 24); bytearray[6] = (byte) ((this.amount << 16) >> 24);
	 * bytearray[7] = (byte) ((this.amount << 8) >> 24); bytearray[8] = (byte)
	 * (this.amount >> 24); bytearray[9] = (byte) ((newBalance << 24) >> 24);
	 * bytearray[10] = (byte) ((newBalance << 16) >> 24); bytearray[11] = (byte)
	 * ((newBalance << 8) >> 24); bytearray[12] = (byte) (newBalance >> 24);
	 * return bytearray; }
	 */

}
