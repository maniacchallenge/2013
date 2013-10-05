package de.fu_berlin.maniac.bank;

import java.util.*;

//import android.annotation.SuppressLint;
import de.fu_berlin.maniac.packet_builder.*;

/**
 * The BankManager manages your fines and wins during the challenge, and
 * provides the information to you in a readable form.
 * The functions to get your current balance is static, so just call BankManager.getBalance()
 * and you'll be fine.
 * 
 * @author maniacchallenge
 * 
 */
public class BankManager {

	/**
	 * The current balance of your account
	 */
	static int balance;

	/**
	 * All transactions, available via the transaction-ID
	 */
	static HashMap<Integer, Integer> transactions;

	/**
	 * Just the constructor
	 */
	//@SuppressLint("UseSparseArrays")
	public BankManager() {
		balance = 0;
		transactions = new HashMap<Integer, Integer>();
	}

	/**
	 * 
	 * @return Returns the current balance
	 */
	public static int getBalance() {
		return balance;
	}

	/**
	 * 
	 * @param transID
	 *            the transaction-ID of the transaction you want to know the
	 *            change of
	 * @return Returns the change of the balance after the given transaction
	 */
	public static int getDiff(int transID) {
		return transactions.get(transID);
	}

	/**
	 * Not to be called by the user, but rather the NetworkManager when a
	 * Check-Packet arrives
	 * 
	 * @param check
	 *            Check-packet with the new balance information
	 */
	public void update(Check check) {
		balance = check.getNewBalance();
		transactions.put(check.getTransactionID(), check.getAmount());
	}

}
