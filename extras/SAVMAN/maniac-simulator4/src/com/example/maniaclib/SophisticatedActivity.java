package com.example.maniaclib;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.fu_berlin.maniac.auction_manager.Auction;
import de.fu_berlin.maniac.exception.ManiacException;
import de.fu_berlin.maniac.general.AuctionParameters;
import de.fu_berlin.maniac.general.ManiacLogger;
import de.fu_berlin.maniac.general.ManiacStrategyInterface;
import de.fu_berlin.maniac.general.Mothership;
import de.fu_berlin.maniac.network_manager.NetworkManager;
import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.Bid;
import de.fu_berlin.maniac.packet_builder.BidWin;
import de.fu_berlin.maniac.packet_builder.Data;
import de.fu_berlin.maniac.packet_builder.Packet;
import de.fu_berlin.maniac.packet_builder.PacketBuilder;
import de.fu_berlin.maniac.strategies.DefaultStrategy;

import android.os.Bundle;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.widget.Toast;

public class SophisticatedActivity extends Activity {
	// Variables for the StatusFragment
	private AtomicBoolean apiRunning = new AtomicBoolean(false);
	private AtomicBoolean olsrdRunning = new AtomicBoolean(false);
	private AtomicBoolean manualMode = new AtomicBoolean(false);

	// Variables for the LogFragment
	private LinkedList<String> log = new LinkedList<String>();

	// API Variables
	public Mothership maniac;
	
	

	
	/**
	 * If you want to write your own app for customized output for example, you need to make sure to create an instance of Mothership and pass it
	 * the activity itself, see below. After that make sure to call .setStrategy with your strategy. Then call .start() to start the Mothership thread */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_sophisticated);

		try {
			maniac = new Mothership(this);
			
		} catch (Exception e) {
			e.printStackTrace();
			log("Error while initializing ManiacLib!");
			Toast.makeText(
					getApplicationContext(),
					"Error while initializing ManiacLib"
							+ (e.getMessage() == null ? "..." : ": "
									+ e.getMessage()), Toast.LENGTH_LONG)
					.show();
			return;
		}

		// Set the strategy and run the start the API
		if (maniac != null) {
			maniac.setStrategy(new DefaultStrategy());
			maniac.start();
			apiRunning.set(true);
		}
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (maniac != null) {
			maniac.setStrategy(null);
			maniac = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		ft.replace(R.id.fragment_menu, MenuFragment.getInstance());
		ft.replace(R.id.fragment_content, StatusFragment.getInstance());

		ft.commit();
	}

	/**
	 * @return true if the API was successfully started, false if not.
	 */
	public boolean isApiRunning() {
		return apiRunning.get();
	}

	/**
	 * @return true if the OLSD daemon is running, false if not.
	 */
	public boolean isOlsrdRunning() {
		return olsrdRunning.get();
	}

	/**
	 * @return returns the IP of the current backbone node the API is connected
	 *         to.
	 */
	public InetAddress getBackboneIp() {
		return NetworkManager.getInstance().getMyOwnBackbone();
	}

	/**
	 * Sets whether the App will answer Adverts and select Winners accordingly
	 * to its strategy or present it to the user.
	 * 
	 * @param manual
	 *            true for enabling the manual mode, false for disabling it.
	 */
	public void setManualMode(boolean manual) {
		manualMode.set(manual);
	}

	


	private void log(String message) {
		synchronized (log) {
			log.add(new Date().toString() + " - " + message);
		}
	}

	public LinkedList<String> getLog() {
		synchronized (log) {
			//String[] list = new String[log.size()];
			//return log.toArray(list);
			return log;
		}
	}

	public void startAuction(Inet4Address dest, Inet4Address finalDest, int id,
			int fine, int maxbid, byte[] payload) {
		// Start a new Auction here.
		// ONLY FOR DEBUGGING AND TESTING PURPOSES. DON'T START YOUR OWN AUCTION
		// DURING THE CHALLENGE!

		Sendjob sj1;
		Sendjob sj2;

		// initial budget for the transmission
		// (also the fine for retransmission via BBN)
		int initialBudget = fine;
		
		Data data = PacketBuilder.getInstance().buildData(id, dest,
				fine, 10, payload, finalDest, initialBudget);
		Advert adv = (Advert) PacketBuilder.getInstance().buildAdvert(data,
				maxbid, fine);
		/*
		maniac.getAuctionManager().getAuctions()
				.put(adv.getTransactionID(), new ArrayList<Bid>());

		maniac.getThreadPoolExecutor().schedule(
				new Auction(data, maniac.getSender(), maniac.getStrategy(),
						maniac.getAuctionManager(), maniac.getMyOwnBackbone()),
				maniac.getAuctionTimeout(), TimeUnit.MILLISECONDS);
*/
		try {
			sj1 = new Sendjob();
			sj2 = new Sendjob();
		} catch (Exception e) {
			log("error while sending: " + e.getMessage());
			return;
		}
		sj1.execute(adv);
		sj2.execute(data);

	}

	



}
