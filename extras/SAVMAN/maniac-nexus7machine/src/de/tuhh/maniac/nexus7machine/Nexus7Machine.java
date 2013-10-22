/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.tuhh.maniac.nexus7machine;

import de.fu_berlin.maniac.general.ManiacStrategyInterface;
import de.fu_berlin.maniac.general.Mothership;
import de.fu_berlin.maniac.network_manager.TopologyInfo;
import de.fu_berlin.maniac.strategies.DefaultStrategy;
import java.net.SocketException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author NetFalcon
 */
public class Nexus7Machine {

          // please specify the interface
          private static String intf = "eth1";
          private static String strategy = "de.fu_berlin.maniac.strategies.DefaultStrategy";

          // REMEMBER set Strategy further below
          // TODO sepcify backbones here not in generic.txt
          public static void main(String[] args) {
	         if (args.length > 0 && !args[0].isEmpty()) {
		        intf = args[0];
	         }
	          if (args.length > 1 && !args[1].isEmpty()) {
		        strategy = args[1];
	         }

	         try {
		        // set the interface
		        TopologyInfo.setInterface(intf);
		        
		        // create new mothership
		        Mothership nexus7 = new Mothership();

		        // set startegy for this nexus7
		        Class strat;
		        try {
			       strat = Class.forName(strategy);
			       Object strato = strat.newInstance();
			        nexus7.setStrategy((ManiacStrategyInterface) strato);
		        } catch (ClassNotFoundException ex) {
			       System.err.println("Error Strategy: " + strategy + " not found.");
		        } catch (InstantiationException ex) {
			       Logger.getLogger(Nexus7Machine.class.getName()).log(Level.SEVERE, null, ex);
		        } catch (IllegalAccessException ex) {
			       Logger.getLogger(Nexus7Machine.class.getName()).log(Level.SEVERE, null, ex);
		        } 
		       

		        // start the Mothership
		        nexus7.start();
		        System.out.println("[" + new Date() + "] Started Nexus7Machine with interface " + intf + " and IP: " + TopologyInfo.getOwnIP() + " and strategy: " + strategy);
	         } catch (SocketException ex) {
		        Logger.getLogger(Nexus7Machine.class.getName()).log(Level.SEVERE, null, ex);
	         }


          }
}
