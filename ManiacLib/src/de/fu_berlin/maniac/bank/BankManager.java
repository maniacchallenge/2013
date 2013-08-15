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

package de.fu_berlin.maniac.bank;

import java.util.*;

import android.annotation.SuppressLint;
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

	// The current balance of your account
	private static Integer balance;
	// difference between latest transaction and (latest -1 ) transaction 
	private static Integer lastDifference;
	
	/**
	 * All transactions, available via the transaction-ID
	 */
	private static HashMap<Integer, Integer> transactions;
	
	/**
	 * just the last updates
	 */
	private static HashMap<Integer, Integer> balanceUpdates;
	
	/**
	 * Just the constructor
	 */
	@SuppressLint("UseSparseArrays")
	public BankManager() {
		balance = 0;
		transactions = new HashMap<Integer, Integer>();
		lastDifference = 0;
	}

	/**
	 * 
	 * @return Returns the current balance
	 */
	public static Integer getBalance() {
		return balance;
	}

	/**
	 * @return Returns the change of the balance since the last update. 
	 * Attention: an update may contain the values of _several transactions_!
	 * getDiff will return the sum of all new amounts.
	 * To get all updates, use getUpdates().
	 * To get the amount of a specific transaction, use getAmount(int transactionID).
	 */
	public static Integer getDiff() {
		return lastDifference;
	}
	
	/**
	 * get all <transactionID, amount> pairs that have been added
	 * with the latest update packet
	 * @return
	 */
	public static HashMap<Integer, Integer> getUpdates(){
		return balanceUpdates;
	}
	
	/**
	 * @param transID
	 *            the transaction-ID of the transaction you want to know the
	 *            change of
	 * @return the Amount you've won/lost in the transaction transactionID.
	 */
	public static Integer getAmount(int transactionID){
		return transactions.get(transactionID);
	}

	/**
	 * Not to be called by the user, but rather the NetworkManager when a
	 * Check-Packet arrives
	 * 
	 * @param check
	 *            Check-packet with the new balance information
	 */

	public void update(Check check) {
			int oldDifference = lastDifference;
			
			//this.lastTransID = check.getTransactionID();
			Integer newBalance = check.getNewBalance();
			lastDifference = balance - newBalance;
			if(lastDifference == 0){
				lastDifference = oldDifference;
			}else{
				System.out.println("OMG NEW BALANCE: "+newBalance);
			}
			balance = newBalance;
			balanceUpdates = check.getBalanceUpdates();
			// ATTENTION: this will update all duplicates.
			BankManager.transactions.putAll(balanceUpdates);
			
	}

}
