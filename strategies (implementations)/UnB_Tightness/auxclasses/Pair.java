package de.fu_berlin.maniac.strategies.auxclasses;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;

import de.fu_berlin.maniac.packet_builder.BidWin;

public class Pair<K, V> {

    private final K element0;
    private final V element1;

    public static <K, V> Pair<K, V> createPair(K element0, V element1) {
        return new Pair<K, V>(element0, element1);
    }

    public Pair(K element0, V element1) {
        this.element0 = element0;
        this.element1 = element1;
    }

    public K getElement0() {
        return element0;
    }

    public V getElement1() {
        return element1;
    }
    
    public boolean equals0(K needle) {
    	if (element0.equals(needle)) {
    		return true;
    	}
    	return false;
    }
    
    public boolean equals1(V needle) {
    	if (element1.equals(needle)) {
    		return true;
    	}
    	return false;
    }
    
    public static <K, V> V getMap(ArrayList<Pair<K, V>> list, K value) {
    	for (Pair<K, V> bns : list ) {
			if (bns.equals0(value)) {
				return bns.getElement1();
			}
		}
		return null;
    }
    	
	public static <K, V> int idExists(ArrayList<Pair<K, V>> list, K id) {
		int i = 0;
		for (Pair<K, V> bns : list ) {
			if (bns.equals0(id)) {
				return i;
			}			
			i++;
		}
		return i;
	}
	
	public static <K, V> boolean hasThatId(ArrayList<Pair<K, V>> list, K id, V IP) {
    	for (Pair<K, V> bns : list ) {
			if (bns.equals0(id) && bns.equals1(IP)) {
				return true;
			}
		}
		return false;
    }
	
}