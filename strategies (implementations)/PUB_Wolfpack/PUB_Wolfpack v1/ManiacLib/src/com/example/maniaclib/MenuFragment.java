package com.example.maniaclib;

import java.util.ArrayList;

import de.fu_berlin.maniac.packet_builder.Packet;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MenuFragment extends ListFragment {

	private static final String[] menu = new String[] { "State", "Actions",
			"Log", "OLSRd" };

	private int current = -1;
	private SophisticatedActivity activity;

	private static MenuFragment me;
	ArrayList<Packet> received_packets;

	public static MenuFragment getInstance() {

		if (me == null)
			me = new MenuFragment();

		return me;

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setListAdapter(new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1, menu));
		activity = (SophisticatedActivity) getActivity();
		received_packets = activity.maniac.getReceived_packets();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		FragmentManager fm = activity.getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();

		// Change the shown fragment depending on the clicked menu item.
		switch (position) {
		case 0: // State
			if (current != position)
				ft.replace(R.id.fragment_content, StatusFragment.getInstance());
			break;

		case 1: // Actions
			if (current != position)
				ft.replace(R.id.fragment_content, ActionFragment.getInstance());
			break;

		case 2: // Log
			if (current != position)
				ft.replace(R.id.fragment_content, LogFragment.getInstance(received_packets));
			break;

		case 3: // OLSRd
			if (current != position)
				ft.replace(R.id.fragment_content, OlsrdFragment.getInstance());

		}

		// If the chosen item has changed, commit it.
		if (current != position) {
			current = position;
			ft.commit();
		}

	}

}
