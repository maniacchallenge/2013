package com.example.maniaclib;


import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class ActionFragment extends Fragment {

	private static ActionFragment me;

	private Button buttonSendData;

	/**
	 * This should be called instead of the constructor in order to have only
	 * one instance of ActionFragment
	 * 
	 * @return A new or existing instance of ActionFragment
	 */
	public static ActionFragment getInstance() {

		if (me == null)
			me = new ActionFragment();

		return me;

	}

	private class Refresh implements Runnable {

		@Override
		public void run() {
			if (activity.isApiRunning()) {

			}

			handler.postDelayed(this, 1000);
		}

	}

	private SophisticatedActivity activity;
	private Handler handler;
	private Refresh refresher;

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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View me = inflater.inflate(R.layout.fragment_actions, container, false);

		buttonSendData = (Button) me.findViewById(R.id.buttonSendData);

		buttonSendData.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fm = getFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();

				Fragment dia = fm.findFragmentByTag("send_data");
				if (dia != null)
					ft.remove(dia);

				ft.addToBackStack(null);
				ft.commit();

				DialogDataFragment df = new DialogDataFragment();
				df.show(getFragmentManager(), "send_data");
			}
		});

		return me;
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
