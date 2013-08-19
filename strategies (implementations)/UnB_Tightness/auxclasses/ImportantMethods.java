package de.fu_berlin.maniac.strategies.auxclasses;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import android.util.Log;

public class ImportantMethods {
	
	public InetAddress[] devices = new InetAddress[45];
	int ctd = 0;
	
	public ImportantMethods() {
		for (int i = 0; i < devices.length; i++) {
			try {
				//IP inexistente
				devices[i] = (InetAddress) InetAddress.getByName("200.168.0.1");				
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static double calculateAverage(ArrayList<Integer> marks) {
	  Integer sum = 0;
	  for (Integer mark : marks) {
	      sum += mark;
	  }
	  return sum.doubleValue() / marks.size();
	}

	public static Integer calculateMinValue(ArrayList<Integer> marks) {
	  Integer min = Integer.MAX_VALUE;
	  for (Integer mark : marks) {
	      if (mark < min) 
    	  	min = mark;
	  }
	  return min;
	}

	public static Integer calculateMaxValue(ArrayList<Integer> marks) {
	  Integer max = Integer.MIN_VALUE;
	  for (Integer mark : marks) {
	      if (mark > max) 
    	  	max = mark;
	  }
	  return max;
 	}
	
	
	public void associateIntFromInet(InetAddress ip) {
		devices[ctd++] = ip;
	}
	
	public Integer fromInetToInt(InetAddress ip) {
		int int_ip = -1;   
		
		int i;
		for (i = 0; i < devices.length; i++) {
			if (devices[i].equals(ip)) {
				int_ip = i;
			}
		}
		
		if (int_ip == -1) {
			associateIntFromInet(ip);
			int_ip = ctd-1;
		}
		
		return int_ip;		
	}
	
	public InetAddress fromIntToInet(Integer int_ip) {
		InetAddress ip = null;
		for (int i = 0; i < devices.length; i++) {
			if (i == int_ip) {
				ip = devices[i];
			}
		}
		return ip;		
	}
}
