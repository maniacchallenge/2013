package com.example.maniaclib;

import java.net.Inet4Address;
import java.net.InetAddress;

import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This Fragment is shown when someone wants to send a new Advert.
 */
public class DialogDataFragment extends DialogFragment {

	// Several UI elements.
	private AutoCompleteTextView editDest; // Destination IP.
	private EditText editFinalDest; // Final destination IP.
	private EditText editId; // Transmission ID.
	private EditText editData; // Data to be sent.
	private EditText editFine; // Fine when the delivery fails.
	private EditText editMaxbid; // Maximum bid for this Advert.

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View me = inflater
				.inflate(R.layout.fragment_dia_data, container, false);

		editDest = (AutoCompleteTextView) me.findViewById(R.id.editDest);
		editFinalDest = (EditText) me.findViewById(R.id.editFinalDest);
		editId = (EditText) me.findViewById(R.id.editId);
		editData = (EditText) me.findViewById(R.id.editData);
		editFine = (EditText) me.findViewById(R.id.editFine);
		editMaxbid = (EditText) me.findViewById(R.id.editMaxbid);

		Button okay = (Button) me.findViewById(R.id.buttonOkay);
		Button cancel = (Button) me.findViewById(R.id.buttonCancel);

		okay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SophisticatedActivity activity = (SophisticatedActivity) getActivity();

				if (!activity.isApiRunning()) { // The API should be running...
					Toast.makeText(activity,
							"Error: ManiacLib is not running.",
							Toast.LENGTH_LONG).show();
					return;
				}

				// Since this is a network activity we need to separate it from
				// the main task.
				AsyncTask<String, Object, Inet4Address[]> task = new AsyncTask<String, Object, Inet4Address[]>() {
					@Override
					protected Inet4Address[] doInBackground(String... params) {
						Inet4Address[] result = new Inet4Address[2];

						try {
							result[0] = (Inet4Address) InetAddress
									.getByName(params[0]);
							result[1] = (Inet4Address) InetAddress
									.getByName(params[1]);
						} catch (Exception e) {
						}

						return result;
					}
				};

				task.execute(editDest.getText().toString(), editFinalDest
						.getText().toString());
				Inet4Address[] dests;
				try {
					dests = task.get();
				} catch (Exception e) {
					Toast.makeText(activity,
							"Error while checking addesses: " + e.getMessage(),
							Toast.LENGTH_LONG).show();
					return;
				}

				if (dests[0] == null) {
					Toast.makeText(activity,
							"The destination address is invalid!",
							Toast.LENGTH_LONG).show();
					return;
				}
				if (dests[1] == null) {
					Toast.makeText(activity,
							"The final destination address is invalid!",
							Toast.LENGTH_LONG).show();
					return;
				}

				int id = Integer.valueOf(editId.getText().toString());
				int fine = Integer.valueOf(editFine.getText().toString());
				int maxbid = Integer.valueOf(editMaxbid.getText().toString());

				byte[] data = editData.getText().toString().getBytes();

				// Start the auction (will take place in a thread within the
				// ManiacLib, so no need for an AsyncTask here).
				activity.startAuction(dests[0], dests[1], id, fine, maxbid,
						data);

				Toast.makeText(activity, "Data will now be sent!",
						Toast.LENGTH_LONG).show();
				dismiss(); // Closes the current dialog fragment.
			}
		});

		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		return me;
	}

}
