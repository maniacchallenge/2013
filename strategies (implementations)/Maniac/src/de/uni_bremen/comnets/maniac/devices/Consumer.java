package de.uni_bremen.comnets.maniac.devices;

/**
 * Created by Isaac Supeene on 7/3/13.
 */
public interface Consumer extends Bidder {
    public int getMostProbableBidAsConsumer(int transactionID, int budget, int fine);
    public double getProbabilityOfSuccessAsConsumer(int transactionID, int budget, int fine);
    public double getProbabilityOfFailureAsConsumer(int transactionID, int budget, int fine);
    public double getProbabilityOfNoBidAsConsumer(int transactionID, int budget, int fine);
}
