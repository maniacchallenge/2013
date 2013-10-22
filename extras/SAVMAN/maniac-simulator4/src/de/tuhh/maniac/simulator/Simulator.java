/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.tuhh.maniac.simulator;

import de.fu_berlin.maniac.network_manager.TopologyInfo;
import de.fu_berlin.maniac.packet_builder.Advert;
import de.fu_berlin.maniac.packet_builder.Data;
import de.fu_berlin.maniac.packet_builder.Packet;
import de.fu_berlin.maniac.packet_builder.PacketBuilder;
import de.fu_berlin.maniac.strategies.DefaultStrategy;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author NetFalcon
 */
public class Simulator {

            public static void main(String[] args) {
                        System.out.println("SIMULATOR startet");



                        // get one Nexus7 and set its parameters
                        NexusSeven first;
                        NexusSeven second;
                        NexusSeven three;
                        NexusSeven four;
                        NexusSeven five;
                        try {
                                    first = new NexusSeven((Inet4Address) Inet4Address.getByName("127.0.0.10"), new DefaultStrategy());
                                    first.start();
                                    four = new NexusSeven((Inet4Address) Inet4Address.getByName("127.0.0.11"), new DefaultStrategy());
                                    four.start();
                                    five = new NexusSeven((Inet4Address) Inet4Address.getByName("127.0.0.12"), new DefaultStrategy());
                                    five.start();
                                    second = new NexusSeven((Inet4Address) Inet4Address.getByName("127.0.0.20"), new DefaultStrategy());
                                    second.start();
                                    three = new NexusSeven((Inet4Address) Inet4Address.getByName("127.0.0.30"), new DefaultStrategy());
                                    three.start();

                        } catch (UnknownHostException ex) {
                                    Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
                        }


                        Backbone bone = null;
                        Backbone btwo = null;
                        Backbone bthree = null;



                        try {
                                    bone = new Backbone((Inet4Address) Inet4Address.getByName("127.0.0.5"));
                                    bone.start();
                                    btwo = new Backbone((Inet4Address) Inet4Address.getByName("127.0.0.6"));
                                    btwo.start();
                                    bthree = new Backbone((Inet4Address) Inet4Address.getByName("127.0.0.7"));
                                    bthree.start();
                        } catch (UnknownHostException ex) {
                                    Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (SocketException ex) {
                                    Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        // Sending data into the network
                        try {
                                    // send an Advert from backbone one
                                    bone.sendAdvert((Inet4Address) Inet4Address.getByName("127.0.0.7"));
                        } catch (UnknownHostException ex) {
                                    Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
                        }

            }
}
