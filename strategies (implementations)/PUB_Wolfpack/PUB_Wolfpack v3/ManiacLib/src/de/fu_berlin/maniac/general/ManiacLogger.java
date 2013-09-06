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

package de.fu_berlin.maniac.general;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.annotation.SuppressLint;
import android.content.*;
import android.text.format.DateFormat;
import de.fu_berlin.maniac.packet_builder.*;

/**
 * 
 * This is the logger-class you're supposed to use
 * 
 * Makes local logs stored on the device like this, depending on the packet
 * type:
 * 
 * #ADVERT A [TRANS_ID] [SOURCE_IP] [FINALDEST_IP] [CEILING] [TIMEOUT]
 * [DEADLINE]
 * 
 * #BID B [TRANS_ID] [SOURCE_IP] [DEST_IP] [BID]
 * 
 * #BIDWIN W [TRANS_ID] [SOURCE_IP] [WINNER_IP]
 * 
 * #DATA D [TRANS_ID] [SOURCE_IP] [FINALDEST_IP]
 * 
 * 
 */
@SuppressLint("SdCardPath")
public final class ManiacLogger {

	private final static String filename = "gol.txt";

	private Context context;

	private FileOutputStream fos;

	private OutputStreamWriter osw;

	/**
	 * Log-File is opened in constructor for security reasons
	 * 
	 * @param con
	 */
	public ManiacLogger(Context con) {
		context = con;
		try {
			fos = context.openFileOutput(filename, Context.MODE_MULTI_PROCESS);
			try {
				fos.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			osw = new OutputStreamWriter(fos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Log-File is only closed in destructor for security reasons
	 */
	protected void finalize() throws Throwable {
		fos.close();
		super.finalize();
	}

	/**
	 * Use this function to log a packet, this works for all packet Types except
	 * checks
	 * 
	 * @param packet
	 *            the packet you want to log
	 * @throws IOException
	 */
	public void sendToLogger(Packet packet) throws IOException {
		java.text.DateFormat df = DateFormat.getTimeFormat(context);
		String time = df.toString();
		switch (packet.getType()) {
		case 'A':
			logAdvert((Advert) packet, time);
			break;
		case 'B':
			logBid((Bid) packet, time);
			break;
		case 'W':
			logBidWin((BidWin) packet, time);
			break;
		case 'D':
			logData((Data) packet, time);
			break;
		}
	}

	private void logAdvert(Advert advert, String time) {
		if(advert!=null && advert.getDestinationIP()!=null) {
			String output = time + " " + advert.getTransactionID() + " " + "A "
					+ advert.getSourceIP().getHostAddress() + " "
					+ advert.getDestinationIP().getHostAddress() + " ";
			output += advert.getCeil() + " ";
			output += advert.getDeadline();
			writeToFile(output);			
		} else {
			String output = "Advert not parsed correctly.";
					writeToFile(output);

		}
	}

	private void logBid(Bid bid, String time) {
		if(bid!=null && bid.getDestinationIP()!=null){
			
			String output = time + " " + bid.getTransactionID() + " " + "B "
					+ bid.getSourceIP().getHostAddress() + " "
					+ bid.getDestinationIP().getHostAddress() + " ";
			output += bid.getBid();
			writeToFile(output);
		} else {
			String output = "Bid not parsed correctly";
			writeToFile(output);
		}
	}

	private void logBidWin(BidWin bidwin, String time) {
		String output = time + " " + bidwin.getTransactionID() + " " + "W "
				+ bidwin.getSourceIP().getHostAddress() + " "
				+ bidwin.getWinnerIP().getHostAddress();
		writeToFile(output);
	}

	private void logData(Data data, String time) {
		if(data.getSourceIP()!=null) {
			String output = time + " " + data.getTransactionID() + " " + "D "
					+ data.getSourceIP().getHostAddress() + " "
					+ data.getFinalDestinationIP().getHostAddress() + " ";
			writeToFile(output);
		} else {
			String output = time + " " + data.getTransactionID() + " " + "D "
			+ data.getFinalDestinationIP().getHostAddress() + " ";
			writeToFile(output);
		}

	}

	private void writeToFile(String info) {
		try {
			osw.write(info);
			osw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}
	
}
