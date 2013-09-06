package com.example.maniaclib;

import java.util.ArrayList;
import java.util.LinkedList;

import de.fu_berlin.maniac.packet_builder.Packet;

import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

public class LogFragment extends ListFragment {

	private static LogFragment me;
	private LinkedList<String> log;
	private static ArrayList<Packet> recv;
	private Packet buffer_packet;

	/**
	 * This should be called instead of the constructor in order to have only
	 * one instance of LogFragment
	 * 
	 * @return A new or existing instance of LogFragment
	 */
	public static LogFragment getInstance(ArrayList<Packet> rv) {
		if (me == null)
			me = new LogFragment();
			recv = rv;

		return me;
	}
	

	// Updates the log view every 200ms.
	private class Refresh implements Runnable {
		@Override
		public void run() {
			
			if(!recv.isEmpty()){
				buffer_packet = recv.get(0);
				recv.remove(0);
				log.add(buffer_packet.toString());
			}
			
			
			String[] list = new String[log.size()];
			
			list = activity.getLog().toArray(list);

			if (list.length != oldLength) {
				setListAdapter(new ArrayAdapter<String>(getActivity(),
						android.R.layout.simple_list_item_1, list));
				oldLength = list.length;
			}

			handler.postDelayed(this, 200);
		}
	}

	private SophisticatedActivity activity;
	private Refresh refresher;
	private Handler handler;

	private int oldLength = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Show an empty list.
		setListAdapter(new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1,
				new String[] { "No entries..." }));
		setHasOptionsMenu(true);

		activity = (SophisticatedActivity) getActivity();
		refresher = new Refresh();
		log = (LinkedList<String>) activity.getLog();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		refresher = null;
		activity = null;
	}

	@Override
	public void onResume() {
		super.onResume();

		handler = new Handler();
		handler.postDelayed(refresher, 10);
	}

	@Override
	public void onPause() {
		super.onPause();

		handler.removeCallbacks(refresher);
		handler = null;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.logger, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.itemMail) {
			// Enables the user to send an E-Mail containing the current log.
			StringBuilder text = new StringBuilder("ManiacLog:\n\n");

			for (String i : activity.getLog()) {
				text.append(i);
				text.append('\n');
			}

			text.append("\nEnd of the Log.\nHave fun!\n");

			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("plain/text");
			intent.putExtra(Intent.EXTRA_EMAIL,
					new String[] { "bar@fu-berlin.de" });
			intent.putExtra(Intent.EXTRA_SUBJECT, "ManiacLog");
			intent.putExtra(Intent.EXTRA_TEXT, text.toString());
			startActivity(Intent.createChooser(intent, "Send mail"));

			return true;
		}

		return false;
	}

}
