# dependencies
- [twisted](http://twistedmatrix.com/trac/wiki/Downloads)
- [protobuf](https://pypi.python.org/pypi/protobuf/2.5.0)

# Usage
to start, simply type ``python backbone.py``

# What it does
The task of all backbones is to start auctions and report all traffic they notice to the master. To accomplish this, they maintain >= 3 connections:

- a **TCP Connection** to the master
- a **UDP Socket** listening for traffic and forwarding everything to the Master for logging
- a **TCP ServerSocket** listening for Connections by Clients
- a **TCP Socket** for every Client connected to the Backbone. The Backbone will periodically send Check packets containing Banking info (i.e. their new balance and the amount they gained/list in the last 5 seconds, and the TransactionID of the transaction in which they lost it) to the Client, while the Client will send copies of the BidWins they're boradcasting to to their Backbone to make sure this is logged by the Master.

The code that ran on the backbones was written in [Python 2.7](http://python.org/download/releases/2.7.5/), and we used [Twisted](http://twistedmatrix.com/), an "event driven networking engine" which does a lot of magic for you.  
In the ``backbone``folder, you'll also find a file called ``protoPackets_pb2.py``, which is the python code complied from the .proto file mentioned in Clients. *Do not* touch this, as this is machine generated code. If you want to learn how to write and compile your own protobuf messages, read (TODO).  

A quick note about the routers from some people who learned it the hard way: *Check the version of your Wifi driver.* You do not want to mess with a 4 year old implementation of Ad Hoc mode.  
