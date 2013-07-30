package com.example.maniaclib;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class OlsrdFragment extends Fragment {

	private static OlsrdFragment me;

	public static OlsrdFragment getInstance() {

		if (me == null)
			me = new OlsrdFragment();

		return me;

	}

	private class Refresh implements Runnable {
		@Override
		public void run() {
			// Load http://localhost:2006 or reload page if it was already
			// loaded.
			if (webOlsrd.getUrl() == null
					|| !webOlsrd.getUrl().equals("http://localhost:2006"))
				webOlsrd.loadUrl("http://localhost:2006");
			else
				webOlsrd.reload();

			// Refresh in 2s.
			handler.postDelayed(this, 2000);
		}
	}

	private WebView webOlsrd;
	private Refresh refresher;
	private Handler handler;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View me = inflater.inflate(R.layout.fragment_olsrd, container, false);

		webOlsrd = (WebView) me.findViewById(R.id.webOlsrd);

		return me;
	}

	@Override
	public void onStart() {
		super.onStart();

		refresher = new Refresh();
	}

	@Override
	public void onStop() {
		super.onStop();

		refresher = null;
	}

	@Override
	public void onResume() {
		super.onResume();

		handler = new Handler();
		handler.postDelayed(refresher, 100);
	}

	@Override
	public void onPause() {
		super.onPause();

		handler.removeCallbacks(refresher);
		handler = null;
	}

}
