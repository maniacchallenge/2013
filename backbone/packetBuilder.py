'''
 This file is part of the API for the Maniac Challenge 2013.

 The Maniac API is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 The Maniac API is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this software.  If not, see <http://www.gnu.org/licenses/lgpl>.
'''

#!/usr/bin/env python
# -*- coding: utf-8 -*-
from google.protobuf.internal import encoder
import random
import protoPackets_pb2
import traceback
import sys
import json
import struct
import ownIp


''' 
This module creates packets that are compliant with the MANIAC challenge 2013
specifications.
'''


# defines the basic structure of a packet
class Packet:
    packetMessage = protoPackets_pb2.PacketMessage()

    PACKET_PORT = 8765

    transactionID = sourceIP = destinationIP = ""

    def buildPayload(self):
        raise NotImplementedError("Packet does not implement this."
            " Please use Bid, BidWin, Advert, Check or Data")

    def toJSON(self):
        raise NotImplementedError("don't forget to code this!")

    # returns the port the packets we be send to.
    def getPort(self):
        return self.PACKET_PORT

    # returns the destination this packet will be sent to 
    # or the one it has been sent to.
    def getDestinationIP(self):
        return self.destinationIP

    # returns the source of this packet (or null if it hasn't been sent).
    def getSource(self):
        return self.sourceIP

    # returns the transaction ID of this packet.
    def getTransactionID(self):
        return self.transactionID

    # determine the packet type, then parse accordingly.
    # returns Advert/Bid/BiwdWin/Chcek/Data packet if type is kown, None otherwise.
    def parse(self, payload, sourceIP):
        try:
            self.packetMessage.ParseFromString(payload)
            packet_type = self.packetMessage.type

            if (packet_type == 0):
                p = Advert()
            elif (packet_type == 1):
                p = Bid()
            elif (packet_type == 2):
                p = BidWin()
            elif (packet_type == 3):
                p = Check()
            elif (packet_type == 4):
                p = Data()
            elif (packet_type == 5):
                p = GeneralPurposePacket()
            else:
                return None

            p.parse(self.packetMessage)
            p.sourceIP = sourceIP

            return p
        
        except: 
            print payload, payload[1]
            #traceback.print_exc(file=sys.stdout)
            return None

# here come the specialized Packets...

class Advert(Packet):

    ceil = deadline = fine = initialBudget = 0

    def buildPayload(self, transactionID, 
        finalDestinationIP, ceil, deadline, fine, initialBudget):
        
        self.transactionID = transactionID
        self.finalDestinationIP = finalDestinationIP
        self.ceil = ceil
        self.deadline = deadline
        self.fine = fine
        self.initialBudget = initialBudget

        return self.serialize()

    '''
        Serialize Packet into sendable & decodable protobuf message
    '''
    def serialize(self):

        # create outer packet message
        packetMessage = protoPackets_pb2.PacketMessage()
        packetMessage.type = protoPackets_pb2.PacketMessage.ADVERT
        packetMessage.transactionID = self.transactionID
        
        # add advert-specific content
        packetMessage.advertMessage.finalDestinationIP = self.finalDestinationIP
        packetMessage.advertMessage.ceil = self.ceil
        packetMessage.advertMessage.deadline = self.deadline
        packetMessage.advertMessage.fine = self.fine
        packetMessage.advertMessage.initialBudget = self.initialBudget

        return packetMessage.SerializeToString()

    '''
        parses the content of a protoPackets_pb2.PacketMessage into this Advert
    '''
    def parse(self, packetMessage):
        
        self.transactionID = packetMessage.transactionID
        
        self.finalDestinationIP = packetMessage.advertMessage.finalDestinationIP
        self.ceil = packetMessage.advertMessage.ceil
        self.deadline = packetMessage.advertMessage.deadline
        self.fine = packetMessage.advertMessage.fine 
        self.initialBudget = packetMessage.advertMessage.initialBudget
        
        return self

    def toJSON(self):
        self.jsonStr = """{
            "type": "A",
            "transactionID": %i,
            "sourceIP": "%s",
            "destinationIP": "%s",
            "finalDestinationIP": "%s",
            "ceil": %i,
            "deadline": %i,
            "fine": %i,
            "initialBudget": %i
        }"""

        return self.jsonStr % (self.transactionID, self.sourceIP, 
            self.destinationIP, self.finalDestinationIP, self.ceil, 
            self.deadline, self.fine, self.initialBudget)


    def buildRandomPacket(self):

        transactionID = random.randint(0,2147483647)
        destinationIP = random.randint(0,2147483647)
        ceil = random.randint(0,4294)
        fine = random.randint(0,4294)

        return self.buildPayload(transactionID, destinationIP, ceil, fine)

    def __str__(self):
        return self.toJSON()


class Bid(Packet):

    bid = 0

    def buildPayload(self, transactionID, bid):
        self.bid = bid
        self.transactionID = transactionID

        return self.serialize()

    '''
        Serialize Packet into sendable & decodable protobuf message
    '''
    def serialize(self):
        # create outer packet message
        packetMessage = protoPackets_pb2.PacketMessage()
        packetMessage.type = protoPackets_pb2.PacketMessage.BID
        packetMessage.transactionID = self.transactionID 

        # add Bid-specific content
        packetMessage.bidMessage.bid = self.bid

        return packetMessage.SerializeToString()        

    def parse(self, packetMessage):
        
        self.transactionID = packetMessage.transactionID
        self.bid = packetMessage.bidMessage.bid

        return self

    def toJSON(self):
        self.jsonStr = """{
            "type": "B",
            "transactionID": %i,
            "sourceIP": "%s",
            "destinationIP": "%s",
            "bid": %i
        }"""

        return self.jsonStr % (self.transactionID, self.sourceIP, 
            self.destinationIP, self.bid)

    def __str__(self):
        return self.toJSON()

class BidWin(Packet):

    winnerIP = ""
    winingBid = fine = 0

    def buildPayload(self, transactionID, 
        winnerIP, winningBid, fine):

        self.transactionID = transactionID
        self.winnerIP = winnerIP
        self.winningBid = winningBid
        self.fine = fine

        return self.serialize()

    '''
        Serialize Packet into sendable & decodable protobuf message
    '''
    def serialize(self):
        # create outer packet message
        packetMessage = protoPackets_pb2.PacketMessage()
        packetMessage.type = protoPackets_pb2.PacketMessage.BIDWIN
        packetMessage.transactionID = self.transactionID 

        # add Bid-specific content
        packetMessage.bidWinMessage.winnerIP = self.winnerIP
        packetMessage.bidWinMessage.winningBid = self.winningBid
        packetMessage.bidWinMessage.fine = self.fine

        return packetMessage.SerializeToString()        

    def parse(self, packetMessage):
        
        self.transactionID = packetMessage.transactionID

        self.winnerIP = packetMessage.bidWinMessage.winnerIP
        self.winningBid = packetMessage.bidWinMessage.winningBid
        self.fine = packetMessage.bidWinMessage.fine

        return self

    def toJSON(self):
        self.jsonStr = """{
            "type": "W",
            "transactionID": %i,
            "sourceIP": "%s",
            "destinationIP": "%s",
            "winnerIP": "%s",
            "winningBid": %i,
            "fine": %i
        }"""

        return self.jsonStr % (self.transactionID, self.sourceIP, 
            self.destinationIP, self.winnerIP, self.winningBid, self.fine)

    def __str__(self):
        return self.toJSON()

class Check(Packet):

    # note: check doesn't need a toJSON since a check will 
    # never be sent to the master
    
    newBalance = 0          # overall new balance
    balanceUpdates = []     # list of all transactions: [(transactionID, amount)]

    '''
    Since the python side of protobuf doesn't automatically support building
    Messages with delimiters we'll have to do it ourselves.
    (And since Checks are always sent via TCP, we need delimiters)
    '''
    def buildPayload(self, transactionID, newBalance, balanceUpdates):

        self.transactionID = transactionID
        self.newBalance = newBalance
        self.balanceUpdates = balanceUpdates

        return self.serializeDelimited()

    '''
        Serialize Packet into sendable & decodable protobuf message
    '''
    def serializeDelimited(self):

        # create outer packet message
        packetMessage = protoPackets_pb2.PacketMessage()
        packetMessage.type = protoPackets_pb2.PacketMessage.CHECK
        packetMessage.transactionID = self.transactionID

        # add check-specific content
        packetMessage.checkMessage.newBalance = self.newBalance
        
        for (transactionID, amount) in self.balanceUpdates:
            x = packetMessage.checkMessage.balanceUpdates.add()
            x.transactionID = transactionID
            x.amount = amount 

        print self

        serializedMessage = packetMessage.SerializeToString()
        delimiter = encoder._VarintBytes(len(serializedMessage))

        return delimiter + serializedMessage       

    '''
        parses the content of a protoPackets_pb2.PacketMessage into this Check
    '''
    def parse(self, packetMessage):
        
        self.transactionID = packetMessage.transactionID
        self.newBalance = packetMessage.checkMessage.newBalance

        balanceUpdateMsgs = packetMessage.checkMessage.balanceUpdates
        amount = transactionID = 0
        for balanceUpdate in balanceUpdateMsgs:
            transactionID = balanceUpdate.transactionID
            amount = balanceUpdate.amount
            self.balanceUpdates.append((transactionID, amount))

        return self

    def buildRandomPayload(self):

        transactionID = random.randint(0,2147483647)
        newBalance = random.randint(-5000,5000)
        balanceUpdates = []

        numberOfUpdates = random.randint(1,10)
        
        for i in range (0, numberOfUpdates):
            transID = random.randint(0,2147483647)
            amount = random.randint(-500,500)
            balanceUpdates.append((transID, amount))

        return self.buildDelimitedPayload(transactionID, newBalance, balanceUpdates)

    def __str__(self):
        s = "Check: { transactionID: '%s', newBalance: '%s', balanceUpdates: %s}"
        return s % (self.transactionID, self.newBalance, self.balanceUpdates)

class Data(Packet):
    
    finalDestinationIP = payload = ""
    deadline = fine = initialBudget = 0

    def buildPayload(self, transactionID, 
        finalDestinationIP, deadline, fine, initialBudget, payload):

        self.transactionID = transactionID
        self.finalDestinationIP = finalDestinationIP
        self.deadline = deadline
        self.fine = fine
        self.initialBudget = initialBudget
        self.payload = payload

        return self.serialize()

    '''
        Serialize Packet into sendable & decodable protobuf message
    '''
    def serialize(self):

        # create outer packet message
        packetMessage = protoPackets_pb2.PacketMessage()
        packetMessage.type = protoPackets_pb2.PacketMessage.DATA
        packetMessage.transactionID = self.transactionID
        
        # add Data-specific content
        packetMessage.dataMessage.finalDestinationIP = self.finalDestinationIP
        packetMessage.dataMessage.deadline = self.deadline
        packetMessage.dataMessage.fine = self.fine
        packetMessage.dataMessage.initialBudget = self.initialBudget
        packetMessage.dataMessage.payload = self.payload

        return packetMessage.SerializeToString()        

    def parse(self, packetMessage):

        self.transactionID = packetMessage.transactionID
        self.finalDestinationIP = packetMessage.dataMessage.finalDestinationIP
        self.deadline = packetMessage.dataMessage.deadline
        self.fine = packetMessage.dataMessage.fine
        self.initialBudget = packetMessage.dataMessage.initialBudget
        self.payload = packetMessage.dataMessage.payload

        return self

    def toJSON(self):
        self.jsonStr = """{
            "type": "D",
            "transactionID": %i,
            "sourceIP": "%s",
            "destinationIP": "%s",
            "finalDestinationIP": "%s",
            "deadline": %i,
            "fine": %i,
            "initialBudget": %i,
            "payload": "%s",
            "me": "%s"
        }"""

        print "xoxoxo", ownIp.getOwnIp()

        return self.jsonStr % (self.transactionID, self.sourceIP, 
            self.destinationIP, self.finalDestinationIP, self.deadline, self.fine, self.initialBudget, 
            self.payload, ownIp.getOwnIp())

    def __str__(self):
        return self.toJSON()

