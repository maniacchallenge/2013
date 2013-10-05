package com.example.maniaclib;

import java.net.SocketException;

import android.os.AsyncTask;

import de.fu_berlin.maniac.network_manager.Sender;
import de.fu_berlin.maniac.packet_builder.*;

public class Sendjob extends AsyncTask<Packet, Integer, Long> {
	Sender sender;

	public Sendjob() throws SocketException {
		sender = new Sender();
	}

	protected Long doInBackground(Packet... packet) {
		sender.send(packet[0]);
		return null;
	}

	protected void onProgressUpdate(Integer... progress) {
	}

	protected void onPostExecute(Long result) {
	}
}