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

from twisted.internet import reactor, protocol
from twisted.internet.protocol import DatagramProtocol
from twisted.python import log
import sys
from packetBuilder import Advert, Data, Check, Bid, BidWin, Packet

'''
    This is a dummy class that emulates a Client for quick tests to see if the 
    backbone is operating properly. 
'''

class BackboneProtocol(protocol.Protocol):
    def __init__(self):
        self.packet = Packet()

    def sendData(self, data):
        self.transport.write(data)
    
    def dataReceived(self, data):
        parsedData = self.packet.parse(data, "")
        log.msg("received data: ", parsedData)

    def connectionMade(self):
        print "I am Nexus and connected to Backbone\n"
        a = Advert()
        self.sendData(a.buildPayload(12345, "localhost",1,2,3,4))
    
    def connectionLost(self, reason):
        print "I am Nexus and have LOST connection to Backbone\n"

class BackboneFactory(protocol.ClientFactory):
    protocol = BackboneProtocol

class UDPProtocol(DatagramProtocol):
    host = "127.0.0.1"
    port = 6678

    def startProtocol(self):
        print "[UDP] starting & sending..."

        b = Bid()
        b_payload = b.buildPayload(12345, 55)

        a = Advert()
        a_payload = a.buildPayload(12345, "localhost",1,2,3,4)
        packetJson = a.toJSON()

        bw = BidWin()
        bw_payload = bw.buildPayload(12345, "localhost", 1, 2)

        self.transport.write(bw_payload, (self.host, self.port))

    def datagramReceived(self, data, (host, port)):
        print "[UDP] received %r from %s:%d" % (data, host, port)

def main():
    log.startLogging(sys.stdout)

    reactor.connectTCP('localhost', 5555, BackboneFactory())
    reactor.listenUDP(9999, UDPProtocol())

    reactor.run()

if __name__ == '__main__':
    main()