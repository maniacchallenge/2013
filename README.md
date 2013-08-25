# API for the MANIAC Challenge 2013

This repository contains code for the 2013 Maniac Challenge (API and Strategies) held at the Free University of Berlin.

# What is the ManiacChallenge?
The MANIAC Challenge is a competition to better understand cooperation and interoperability in ad-hoc networks. Competing teams students/researchers come together to form a wireless ad-hoc network, while simultaneously connected to a backbone of access points. The organizers generate traffic coming from the backbone, destined to somewhere in the network. A hop-by-hop bidding contest decides the path of each data packet towards its destination.

# Rules of the 2013 MANIAC Challenge
The specific focus of the MANIAC Challenge 2013 is on developing and comparatively evaluating strategies to offload infrastructure access points via customer ad hoc forwarding using handhelds (e.g., smartphones, tablets). The incentive for customers is discounted monthly fees, and the incentive for operators is decreased infrastructure costs. The idea is to demonstrate scenarios/strategies that do not degrade user experience while offering significant mobile offloading on the infrastructure.

# Hardware
- For the ``Backbones``, we used between 8 and 23 nodes of the [DES-testbed](http://des-testbed.net/). All Backbone routers were connected to one central server, the ``Master``. Tha Master tols the Backbones to announce traffic and logged every packet the Backbones picked up.
- As ``Clients``, we used [Google's Nexus 7](http://www.google.com/nexus/7/) tablets running Android 4.2.1. 

# Software and Programming Languages

We used Google's [ProtocolBuffer](http://code.google.com/p/protobuf/) to serialize the data that is sent between Backbones and Clients.  
The actual routing was done by [OLSRd](http://www.olsr.org), the daemon for the [Optimized Link State Routing Protocol](http://en.wikipedia.org/wiki/Optimized_Link_State_Routing_Protocol).

## Clients
The API for the tablets was entirely written in Java (since there's Android running on them).
The Eclipse project contains a file called ``protoPackets.proto`` in its ``/src``folder which specifies our protobuf message format, as well as the .java files compiled from it.

### Setting up your Tablets

To get the ad-hoc mode running on the Nexus 7, you first have to install [CyanogenMod](http://www.cyanogenmod.org/), then build your [own Kernel](http://source.android.com/source/building-kernels.html) and finally install the [ManetManager](https://github.com/ProjectSPAN/android-manet-manager), written by the [SPAN-Team](https://groups.google.com/forum/#!forum/spandev), but you have to [root your device](https://www.google.de/search?output=search&sclient=psy-ab&q=root%20cyanogenmod%20nexus%207&=&=&oq=&gs_l=&pbx=1) to use it.
And finally, you have to write an [App](http://developer.android.com) which uses the API. We provided a very simple dummy app, so you can get an idea of how it can look like/work.

## Backbones
The task of all backbones is to start auctions and report all traffic they notice to the master. To accomplish this, they maintain >= 3 connections:

- a **TCP Connection** to the master
- a **UDP Socket** listening for traffic and forwarding everything to the Master for logging
- a **TCP ServerSocket** listening for Connections by Clients
- a **TCP Socket** for every Client connected to the Backbone. The Backbone will periodically send Check packets containing Banking info (i.e. their new balance and the amount they gained/list in the last 5 seconds, and the TransactionID of the transaction in which they lost it) to the Client, while the Client will send copies of the BidWins they're boradcasting to to their Backbone to make sure this is logged by the Master.

The code that ran on the backbones was written in [Python 2.7](http://python.org/download/releases/2.7.5/), and we used [Twisted](http://twistedmatrix.com/), an "event driven networking engine" which does a lot of magic for you.  
In the ``backbone``folder, you'll also find a file called ``protoPackets_pb2.py``, which is the python code complied from the .proto file mentioned in Clients. *Do not* touch this, as this is machine generated code. If you want to learn how to write and compile your own protobuf messages, read the [protobuf installation](https://github.com/maniacchallenge/2013/wiki/protobuf-installation) manual.  


A quick note about the routers from some people who learned it the hard way: *Check the version of your Wifi driver.* You do not want to mess with a 4 year old implementation of Ad Hoc mode.  

## Master
The Code for the Master-Server was written in [Go](http://golang.org/)

## LiveLog
We built a LiveLog so can see live what the state of the handhelds is. This was written in every language you can imagine when it comes to creating a webpage.

We hope this will help you to get started. 
For more detailed information about Message formats etc please consult the [Wiki](https://github.com/maniacchallenge/2013/wiki).
If you have any questions, do not hesitate to contact us.
***Happy Hacking!***
