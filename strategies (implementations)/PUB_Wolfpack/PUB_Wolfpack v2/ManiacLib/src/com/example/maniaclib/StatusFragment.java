package com.example.maniaclib;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.fu_berlin.maniac.network_manager.Link;
import de.fu_berlin.maniac.network_manager.NetworkManager;
import de.fu_berlin.maniac.network_manager.TopologyInfo;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

public class StatusFragment extends Fragment {

	private static StatusFragment me;

	/**
	 * This should be called instead of the constructor in order to have only
	 * one instance of StatusFragment
	 * 
	 * @return A new or existing instance of StatusFragment
	 */
	public static StatusFragment getInstance() {

		if (me == null)
			me = new StatusFragment();

		return me;

	}
	
	// Updates the view every second. (This may take some time and is responsible for the FrameSkips in the App)
	private class Refresh implements Runnable {

		@Override
		public void run() {
			try {
				
			// Is the API running?
			if (activity.isApiRunning()) {
				textApiStatus.setText("aktiv");
				textApiStatus.setTextColor(Color.GREEN);
			} else {
				textApiStatus.setText("inaktiv");
				textApiStatus.setTextColor(Color.RED);
			}

			// What's the backbone IP?
			InetAddress ip = NetworkManager.getInstance().getMyOwnBackbone();
			if (ip != null) {
				textBackboneStatus.setText(ip.getHostAddress());
				textBackboneStatus.setTextColor(Color.GREEN);
			} else {
				textBackboneStatus.setText("keine");
				textBackboneStatus.setTextColor(Color.RED);
			}

			// Are there any neighbors?
			try {
				int neighs = TopologyInfo.getLinks().size();
				if (neighs == 0) {
					textNeighborsVisible.setText("keine");
					textNeighborsVisible.setTextColor(Color.RED);
				} else {
					textNeighborsVisible.setText(Integer.toString(neighs));
					textNeighborsVisible.setTextColor(Color.GREEN);
				}
			} catch (Exception e) {
				textNeighborsVisible.setText("Fehler");
				textNeighborsVisible.setTextColor(Color.RED);
			}

			// Update Links
			try {
				ArrayList<Link> neighs = TopologyInfo.getLinks();
				textLinks.setText("");
				for (Link l : neighs) {
					textLinks.append(l.toString() + "\n");
				}
			} catch (Exception e) {
				textNeighborsVisible.setText("Fehler");
				textNeighborsVisible.setTextColor(Color.RED);
			}
			
			// Update time stamp...
			textStatusUpdated.setText(SimpleDateFormat.getTimeInstance()
					.format(new Date()));
			
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Schedule for rerun in 1s.
			handler.postDelayed(this, 1000);
		}

	}

	// The several UI elements
	private TextView textApiStatus;
	private TextView textBackboneStatus;
	private TextView textNeighborsVisible;
	private TextView textStatusUpdated;
	private TextView textLinks;

	// References needed to update the shown data.
	private SophisticatedActivity activity;
	private Handler handler;
	private Refresh refresher;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View me = inflater.inflate(R.layout.fragment_status, container, false);

		textApiStatus = (TextView) me.findViewById(R.id.textApiStatus);
		textBackboneStatus = (TextView) me
				.findViewById(R.id.textBackboneStatus);
		textNeighborsVisible = (TextView) me
				.findViewById(R.id.textNeighborsVisible);
		textStatusUpdated = (TextView) me.findViewById(R.id.textStatusUpdate);
		textLinks = (TextView) me.findViewById(R.id.textLinks);
		
		// Callback for the manual-mode-switch
		Switch switchManual = (Switch) me.findViewById(R.id.switchManual);
		switchManual.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				activity.setManualMode(isChecked);
			}
		});

		return me;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		activity = (SophisticatedActivity) getActivity();
		refresher = new Refresh();
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

}
