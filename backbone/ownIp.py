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

import socket

Own_ip = None

def getOwnIp():
    global Own_ip
    if Own_ip == None:
        if socket.gethostbyname(socket.gethostname()) == "127.0.1.1":
            print "Detected IP as localhost, so I am not running on a mesh router"
            Own_ip = "192.168.1.147"
        else:
            Own_ip = socket.gethostbyname(socket.gethostname()+ '-wlan0')
    return  Own_ip

if __name__ == '__main__':
    getOwnIp()