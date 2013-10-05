package de.fu_berlin.maniac.general;

public class AuctionParameters {

	final int maxbid;

	final int fine;

	/**
	 * AuctionParameters are used to specify the maximum accepted bid and the
	 * fine the winner node has to pay to this node in case the data packet
	 * delivery ends up failing.
	 * 
	 * NOTE: The fine has to be equal or smaller than the fine from the auction
	 * this data packet was won from
	 * 
	 * @param maxb
	 *            Will be set to 1 if it is less than 1.
	 * @param fine
	 *            Cannot be bigger than maxBid, will be set to maxBid in case a
	 *            bigger value is passed. Cannot be bigger than the fine in the
	 *            auction that the data packet has been won from was. Will be
	 *            set to 0 if a negative value is passed.
	 */
	public AuctionParameters(int maxb, int fine) {

		/**
		 * Fine has to be smaller than Maxbid. Maxbid has to be more than 0 and
		 * fine can min. be 0 I think
		 * 
		 * */
		if (maxb < 1) {
			this.maxbid = 1;
		} else {
			this.maxbid = maxb;
		}
		if (fine > maxbid) {
			this.fine = this.maxbid;
		}
		// TODO Darf fine = 0 sein?
		else if (fine < 0) {
			this.fine = 0;

		} else {
			this.fine = fine;
		}

	}

	public int getMaxbid() {
		return maxbid;
	}

	public int getFine() {
		return fine;
	}
}
