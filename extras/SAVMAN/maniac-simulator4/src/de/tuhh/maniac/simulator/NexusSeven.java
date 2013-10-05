/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.tuhh.maniac.simulator;

import de.fu_berlin.maniac.general.Mothership;
import de.fu_berlin.maniac.strategies.DefaultStrategy;
import java.net.Inet4Address;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a representation of a Nexus7
 *
 */
public class NexusSeven {

    // ip of this NexusSeven
    private Inet4Address IP;
    Mothership first = null;

    public NexusSeven(Inet4Address IP, DefaultStrategy crrStrategy) {
        // this is some ugly coding, encapsulate it ;)
        this.IP = IP;
        System.out.println( "[" + IP.toString() + "] Nexus7 started");

        try {
            first = new Mothership(IP);

            // set no strategy
            first.setStrategy(crrStrategy);
        } catch (SocketException ex) {
            Logger.getLogger(NexusSeven.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void start(){
            first.start();
  }

}
