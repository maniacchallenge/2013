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

import packetBuilder
import json


def decodeJSON(json_pack):
    dic = json.loads(json_pack)
    options = {

        "A": decodeAdvert,
        "B": decodeBid,
        "W": decodeBidWin,
        "C": decodeCheck,
        "D": decodeData
    }
    func = options[dic['type']]


    return func(dic)



def decodeAdvert(dic):
    adv = packetBuilder.Advert()

    adv.transactionID = int(dic['transactionID'])
    adv.sourceIP = dic['sourceIP']
    adv.destinationIP = dic['destinationIP']
    adv.finalDestinationIP = dic['finalDestinationIP']
    adv.ceil = int(dic['ceil'])
    adv.deadline = int(dic['deadline'])
    adv.fine = int(dic['fine'])
    adv.initialBudget = int(dic['initialBudget'])

    return adv

def decodeBid(dic):
    bid = packetBuilder.Bid()

    bid.transactionID = int(dic['transactionID'])
    bid.sourceIP = dic['sourceIP']
    bid.destinationIP = dic['destinationIP']
    bid.bid = int(dic['bid'])

    return bid

def decodeBidWin(dic):
    bidwin = packetBuilder.BidWin()

    bidwin.transactionID = int(dic['transactionID'])
    bidwin.sourceIP = dic['sourceIP']
    bidwin.destinationIP = dic['destinationIP']
    bidwin.winnerIP = dic['winnerIP']
    bidwin.winningBid = int(dic['winningBid'])
    bidwin.fine = int(dic['fine'])

    return bidwin

def decodeCheck(dic):
    check = packetBuilder.Check()

    check.transactionID = int(dic['transactionID'])
    check.sourceIP = dic['sourceIP']
    check.destinationIP = dic['destinationIP']
    check.amount = int(dic['amount'])
    check.newBalance = int(dic['newBalance'])

    return check

def decodeData(dic):
    data = packetBuilder.Data()

    data.transactionID = int(dic['transactionID'])
    data.sourceIP = dic['sourceIP']
    data.destinationIP = dic['destinationIP']
    data.finalDestinationIP = dic['finalDestinationIP']
    data.deadline = int(dic['deadline'])
    data.payload = dic['payload']
    data.fine = int(dic['fine'])

    return data

def decodeXCommand(dic):
    data = packetBuilder.Data()
    adv = packetBuilder.Advert()

    data.type = "D"
    adv.type = "A"
    data.transactionID = adv.transactionID = int(dic['transactionID'])

    data.finalDestinationIP = adv.finalDestinationIP = dic['finalDestinationIP']

    data.deadline = adv.deadline = int(dic['deadline'])
    data.payload = dic['payload']

    data.fine = adv.fine = int(dic['fine'])

    adv.ceil = int(dic['ceil'])
    adv.initialBudget = data.initialBudget = dic['ceil']

    return adv, data
