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

from twisted.internet.protocol import Protocol, Factory, DatagramProtocol
from twisted.internet import reactor, defer

'''
    This is a dummy class that emulates the master for quick tests to see if the 
    backbone is operating properly. 
'''

class Echo(Protocol):
    def connectionMade(self):
        self.transport.write("Check Dummy\n\r")
                
    def connectionLost(self, reason):
        print "MASTER: Connection Lost", reason, "\n"
        reactor.stop()

    def dataReceived(self, data):
        print "[MASTER] received:" + data
        self.transport.write("ECHO from Master: " + data)

class BackboneProtocol(Protocol):
    def dataReceived(self, data):
        print "[MASTER] Received packet:", data, "\n"
    
    def sendData(self, data):
        print "sending ", data
        self.transport.write(data)
    
    def connectionMade(self):
        print "[MASTER] Connection to Backbone established\n"
        #checkDeferred = self.getTestChecks()
        #checkDeferred.addCallback(self.sendData)
        advDeferred = self.getTestAdv()
        advDeferred.addCallback(self.sendData)

    def getTestAdv(self):
        d = defer.Deferred()
        self.advStr = """{
                "type": "A",
                "transactionID": 688,
                "sourceIP": "130.133.110.236",
                "destinationIP": "",
                "finalDestinationIP": "172.16.17.12",
                "ceil": 100,
                "deadline": 20,
                "fine": 100,
                "initialBudget": 100
            }"""

        reactor.callLater(5, d.callback, self.advStr)
        return d


    def getTestChecks(self):
        d = defer.Deferred()
        self.checkStr = """
        {
            "type": "C",
            "devices": {
                "172.18.18.156": {
                    "device": "172.18.18.156",
                    "balance": 15,
                    "balanceUpdates": [
                        {
                            "transactionID": 1,
                            "amount": 25
                        },
                        {
                            "transactionID": 2,
                            "amount": -10
                        }
                    ]
                },
                "192.168.1.151": {
                    "device": "192.168.1.151",
                    "balance": -2,
                    "balanceUpdates": [
                        {
                            "transactionID": 2,
                            "amount": -5
                        },
                        {
                            "transactionID": 3,
                            "amount": 3
                        }
                    ]
                },
                "192.168.1.152": {
                    "device": "192.168.1.152",
                    "balance": -2,
                    "balanceUpdates": [
                        {
                            "transactionID": 2,
                            "amount": -5
                        },
                        {
                            "transactionID": 3,
                            "amount": 3
                        },
                        {
                            "transactionID": 83,
                            "amount": 9
                        }

                    ]
                }
            }
        }"""

        reactor.callLater(5, d.callback, self.checkStr)
        return d

    def connectionLost(self, reason):
        print "[MASTER] Connection to backbone Lost", reason, "\n"

class BackboneFactory(Factory):
    protocol = BackboneProtocol
    
    def buildProtocol(self, address):
        p = self.protocol()
        p.factory = self
        return p

def main():
    f = Factory()
    f.protocol = Echo
    reactor.listenTCP(8877, f)
    reactor.listenTCP(6789, BackboneFactory())
    reactor.run()

if __name__ == '__main__':
    main()