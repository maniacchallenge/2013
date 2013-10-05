/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.tuhh.maniac.simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author NetFalcon
 */
public class TxtInfoFaker {
    
    
        /*
     * Currently the simulator has one textfile with info
     * There should be a number of txt files in the txtinfo folder to pretend some kind of topology changes
     * 
     * 
     * WHO is requesting ?
     * 
     */
    public static List<String> getTxtInfo(String req, Inet4Address who) {
        List<String> retlist = new ArrayList<String>();
        BufferedReader in = null;
        
        
        // set simulation directory for currenty simulation
        File simDirectory = new File("txtinfos" + File.separator +  "scenario_0" +File.separator);

        // TODO: Support all requests! // Get more data
        System.out.println("SIM.Simulator.getTxtInfo() - req from: " + who + " req is: " + req);
        
        File info = null;
        
        if(req.equals("/lin")){
             System.out.println("SIM.Simulator.getTxtInfo() - req is equal /lin");
             info = new File(simDirectory, who + "_lin.txt");
	System.out.println(info.getAbsoluteFile());
            
        } else {
            System.out.println("SIM.Simulator.getTxtInfo() - req currently not supported");
        }
        
        

        // load data
     
        try {
            in = new BufferedReader(new FileReader(info));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        String line;
        try {
            while ((line = in.readLine()) != null) {
                if (!line.equals("")) {
                    System.out.println("SIM.Simulator.getTxtInfo() - Line is: " + line);
                    retlist.add(line);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }


        return retlist;
        
    }
    
}
