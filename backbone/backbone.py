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

# -*- coding: utf-8 -*-
from twisted.internet import reactor, defer, threads
from twisted.internet.protocol import Protocol, Factory, DatagramProtocol, ClientFactory
from twisted.internet.endpoints import TCP4ClientEndpoint, connectProtocol
from twisted.python import log
from packetBuilder import Advert, Data, Check, Bid, BidWin, Packet
import json
import sys
from jsondecode import *
from socket import SOL_SOCKET, SO_BROADCAST

# Hashmap, which holds the protocols for every connected Nexus-Device
# key: IP-Address of Device -- value: protocol for TCP-Connection with Device
connectedNexi = dict()

udpProtocol = None

# Hashmap that holds lists of bids for auctions currently being ran by this node
auctiontable = dict()

globalCheckDict = dict()


host = "localhost"
nexusTcpPort = 51113 #5555
udpPort = 8765
masterTcpPort = 6789
masterIP =  "127.0.0.1" #"160.45.111.42" # #"192.168.1.147"

#connectedToMaster = False
# Endpoint zum Master jetzt global
#point = TCP4ClientEndpoint(reactor, masterIP, masterTcpPort)
#masterDeferred = defer.Deferred()
#MUSS NOCH GESETZT WERDEN
BROADCAST_ADDRESS = "172.16.255.255"

AUCTION_TIMEOUT = 3


    
# The protocol for the communication between this backbone and the master
class MasterProtocol(Protocol):

    def __init__(self):
        global udpProtocol
        udpProtocol.setMasterConnection(self)

    def sendData(self, data):
        self.transport.write(data)
    
    """
    case Check bundle received:

    Store latest Check info for *all* Nexi in the whole Network in the 
    globalCheckDict. 
    packageDict has the following structure (values are examples):
    {
    '123.123.123.2': {
        'device': '123.123.123.2',
        'balanceUpdates': [{'transactionID': 2, 'amount': -5},
                           {'transactionID': 3, 'amount': 3}],
        'balance': -2}, 
    '127.0.0.1': {
        'device': '127.0.0.1',
        'balanceUpdates': [{'transactionID': 1, 'amount': 25},
                           {u'transactionID': 2, u'amount': -10}],
        'balance': 15}
    }

    Then, send Check info to all Nexi connected with this backbone.

    case X received:
        start auction
    """
    def dataReceived(self, data):
        global udpProtocol
        log.msg("received data from master: ", data)
        try:
            packageDict = json.loads(data)
            if packageDict["type"] == "C":

                freshChecks = packageDict["devices"]
                # overwrite old checks
                globalCheckDict.update(freshChecks)
                # send new checks to my connected Nexi
                for deviceIP, deviceInfo in freshChecks.iteritems():
                    globalCheckDict[deviceIP] = deviceInfo
                self.sendChecks(freshChecks)

            elif packageDict["type"] == "X":

                #Add auction to auctiontable, with empty list (for bids)
                transactionID = int(packageDict["transactionID"])
                auctiontable[transactionID] = []

                #Build packets
                adv, data = decodeXCommand(packageDict)

                #Build protobuf string to send
                adv.buildPayload(int(packageDict['transactionID']), packageDict['finalDestinationIP'], int(packageDict['ceil']), 
                    int(packageDict['deadline']), int(packageDict['fine']), int(packageDict['ceil']))
                adv.sourceIP = self.transport.getHost().host
                log.msg("advertising ",self.transport.getHost(), adv)
                
                #Send advert
                adv_serial = adv.serialize()


                udpProtocol.sendData(adv_serial,BROADCAST_ADDRESS,udpPort)

                #send to master
                advJSON = adv.toJSON().encode('ascii','replace')
                try:
                    self.sendData(advJSON)
                except Exception as inst:
                    log.msg(inst)

                #start Auction
                auction = self.startAuction(packageDict, data)

                #finishAuction will be called with data after AUCTION_TIMEOUT seconds, see startAuction()
                auction.addCallback(finishAuction)
        except:
            log.err("received incomplete data from master: ", data)
    
    def connectionMade(self):
        log.msg("Successfully connected to Master\n")
        #self.transport.setTcpKeepAlive(True)
    
    def connectionLost(self, reason):
        log.msg("Lost connection to Master. Reason:", reason, "\n")
        # hier jetzt Funktion aufrufen, die Endpoint zum Master neu initialisiert und connected
        #connectedToMaster = False
        #masterDeferred = connectMaster(point)

    # TODO: check if nexus found?
    def sendChecks(self, deviceInfos):
        self.deviceInfos = deviceInfos

        # fetch all checks for "our" nexi
        for address, nexus in connectedNexi.iteritems():
            try:
                # defer check assembly to another thread
                checkDict = self.deviceInfos[address]
                checkDeferred = threads.deferToThread(buildCheck, checkDict)
                checkDeferred.addCallback(nexus.sendData)
                log.msg("sent check")
            except:
                log.err("No Checks available for this Nexus (building empty Check): ", address)



    def startAuction(self, packageDict, data):
        log.msg("startAuction called\n")

        #Setup the callback
        auction = defer.Deferred()
        reactor.callLater(AUCTION_TIMEOUT,auction.callback,(self, data))
        return auction


# This factory generates an instance of MasterProtocol, as soon as the connection is established
class MasterFactory(ClientFactory):
    protocol = MasterProtocol

        
    def startedConnecting(self, connector):
        log.msg("Started connecting to Master\n")

    def clientConnectionFailed(self, connector, reason):
        log.msg("Connection to Master failed - will try again\n")
        connector.connect()

    def clientConnectionLost(self, connector, reason):
        log.msg("Connection to Master lost - will try to reconnect\n")
        connector.connect()
    
    def buildProtocol(self, address):
        p = self.protocol()
        p.factory = self
        return p

# For every TCP-connection to a Nexus-Device, an instance of this protocol will be created
class NexusTCPProtocol(Protocol):

    def __init__(self, nexusAddress, masterProtocol):
        self.nexusAddress = nexusAddress
        self.masterConnection = masterProtocol
        self.packet = Packet()
        self.emptyDict = {
            'device': '%s',
            'balanceUpdates': [],
            'balance': 0}
        
    def sendData(self, data):
        self.transport.write(data)

    def forwardToMaster(self, data):
        self.masterConnection.sendData(data)

    def dataReceived(self, data):
        parsedData = self.packet.parse(data, self.nexusAddress)
        # since toJSON() sometimes returns a unicode string after sending
        # (no idea why...) we'll make sure it's encoded properly
        packetJson = parsedData.toJSON().encode('ascii','replace')
        log.msg("Received data from Nexus:", parsedData, "\n")
        self.forwardToMaster(packetJson)

    def connectionMade(self):
        log.msg("Connected to Nexus ", self.nexusAddress)
        # just in case the nexus missed it, re-send latest check
        try:
            checkDict = globalCheckDict[self.nexusAddress]
        except:
            log.err("No Checks available for this Nexus: ", self.nexusAddress)
            checkDict = self.emptyDict

        checkDeferred = threads.deferToThread(buildCheck, checkDict)
        checkDeferred.addCallback(self.sendData)

        
    def connectionLost(self, reason):
        log.msg("Connection to Nexus lost: ", self.nexusAddress, reason)
        self.transport.loseConnection()

# This factory generates the NexusTCPProtocols and adds them to the connectedNexi-Hashmap
class NexusTCPFactory(Factory):
    protocol = NexusTCPProtocol

    def __init__(self, masterProtocol):
        self.masterConnection = masterProtocol
        log.msg(self.masterConnection)
        log.msg("init Nexus TCP factory")

    def buildProtocol(self, address):
        log.msg("New Conection: ",address)
        p = self.protocol(address.host, self.masterConnection)
        p.factory = self # ist das nciht doppelt gemoppelt?
        connectedNexi[address.host] = p
        return p

'''
Listens for UDP messages and sends them to the master.
Sends Advert, BidWin and Data Packets.
'''
class NexusUDPProtocol(DatagramProtocol):
    def __init__(self):
       # self.masterConnection = masterProtocol
        self.packet = Packet()
       # udpProtocol = self
        
    def startProtocol(self):
        self.transport.socket.setsockopt(SOL_SOCKET, SO_BROADCAST,True)
        return DatagramProtocol.startProtocol(self)

    def setMasterConnection(self,mProtocol):
        self.masterProtocol = mProtocol

    def sendData(self, data, host, port):
        self.transport.write(data, (host, port))

    def forwardToMaster(self, data):
        if not type(self.masterProtocol) == type(None):
            if not type(data) == type(None):
                self.masterProtocol.sendData(data)

    def datagramReceived(self, data, (host, port)):
        parsedData = self.packet.parse(data, host)
        # since toJSON() sometimes returns a unicode string after sending
        # (no idea why...) we'll make sure it's encoded properly
        packetJson = parsedData.toJSON().encode('ascii','replace')

        log.msg("received %r from %s:%d" % (packetJson, host, port))
        
        # forward everything you hear to the master for logging
        self.forwardToMaster(packetJson)
        log.msg("forwarding to master: ", packetJson)

        packetType = parsedData.__class__.__name__
        if (packetType == "Bid"):
            #If bid is for a running auction, add to hashtable
            if(parsedData.transactionID in auctiontable):
                auctiontable[parsedData.transactionID].append(parsedData)

# TODO: wenn ich zuerst auf TCP lausche, kommt UDP nicht zum zug
def startUDP(masterFactory):
    global udpProtocol

    udpProtocol = NexusUDPProtocol()
    reactor.listenUDP(udpPort, udpProtocol)
 

    return masterFactory

def startTCP(masterProtocol):
    log.msg(reactor.listenTCP(nexusTcpPort, NexusTCPFactory(masterProtocol)))

def errorhandler(masterProtocol):
    log.err("failed to start connection to master")

def finishAuction((self, data)):
    bids = auctiontable[data.transactionID]


    #If list is not empty
    if bids:
        min_bid = 1000000
        winner = None
        #get biggest bid
        for b in bids:
            if(b.bid < min_bid):
                min_bid = b.bid
                winner = b

        #Build Bidwin
        bidwin = BidWin()
        data.destinationIP = winner.sourceIP

        #Make bidwin and data send rdy
        bw_send_rdy = bidwin.buildPayload(data.transactionID,winner.sourceIP, winner.bid, data.fine)
        d_send_rdy = data.serialize()

        #Send out bidwin and data to mesh
        udpProtocol.sendData(bw_send_rdy,BROADCAST_ADDRESS,udpPort)
        udpProtocol.sendData(d_send_rdy, winner.sourceIP, udpPort)

        #Send to master
        bidwinJSON = bidwin.toJSON().encode('ascii','replace')
        dataJSON = data.toJSON().encode('ascii','replace')
        self.sendData(bidwinJSON)
        self.sendData(dataJSON)
        log.msg("sent to master:", bidwinJSON, "\n", dataJSON)

def buildCheck(checkDict):
    check = Check() 

    #turn [{key: value}] into [(key, value)]
    updateList = checkDict["balanceUpdates"]
    balanceUpdates =[tuple(x.values()) for x in updateList]

    checkSerialized = check.buildPayload(0, checkDict["balance"], balanceUpdates)
    log.msg("built check: ", check)
    return checkSerialized

#def connectMaster(mFac):
    #reactor.connectTCP(masterIP, masterTcpPort, MasterFactory())

def dummyConnect(mFac):
    d = defer.Deferred()
    reactor.callLater(0, d.callback, mFac)
    return d

def main():
    log.startLogging(sys.stdout)

    # create Endpoint for master
    
    mFac = MasterFactory()


    #connectMaster(point)
    # this will be called once the master is done connecting (and thus 
    # masterDeferred has a value, namely the protocol created)
    reactor.connectTCP(masterIP, masterTcpPort, mFac)

    masterDeferred = dummyConnect(mFac)
    masterDeferred.addCallback(startUDP)
    masterDeferred.addCallback(startTCP)
    # TODO add errback
    reactor.run()

if __name__ == '__main__':
    main()