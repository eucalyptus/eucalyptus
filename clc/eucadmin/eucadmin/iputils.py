# IP address / subnet calculation utilities
#
# Copyright (C) 2012 Eucalyptus Systems, Inc.
# All rights reserved.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

import sys
import string
import re
import os
import struct
import socket
import subprocess

class IPError(Exception):
    pass

class IPMissing(Exception):
    pass

class MacValueException(Exception):
    pass

def dottedQuadToNum(ip):
    "convert decimal dotted quad string to long integer"
    return struct.unpack('>L',socket.inet_aton(ip))[0]

def numToDottedQuad(n):
    "convert long int to dotted quad string"
    return socket.inet_ntoa(struct.pack('>L',n))

def makeMask(n):
    "return a mask of n bits long integer"
    return (2L<<n-1)-1

def getNetworkRange(ip, prefix):
    """Get the network range for an IP address / netmask pair

    :param ip: The IP address in dotted-quad form
    :type ip: str
    :param prefix: The netmask in prefix form
    :type prefix: int
    :return: The network in dotted-quad form
    :rtype: 2-tuple of strings

    """

    n = dottedQuadToNum(ip)
    m = makeMask(32-int(prefix))
    host = n & m
    net = n - host
    brd = net | m

    return numToDottedQuad(net), numToDottedQuad(brd)

def sanityCheckMacString(mac_string):
    octets = mac_string.split(":")
    if len(octets) != 6:
        raise MacValueException, "Incorrect length for MAC address"
    for o in octets:
        num = int(o, 16)
        if num > 255:
            raise MacValueException, "Invalid MAC address: %s" % mac_string

def sanityCheckIPString(ip_string):
    """Sanity check an IP string. Taken from anaconda.network

    :param ip_string: The IP address
    :type ip_string: str.
    :returns: None
    :raises: IPMissing, IPError

    """
    # 

    if ip_string.strip() == "":
        raise IPMissing, ("IP address is missing.")

    if ip_string.find(':') == -1 and ip_string.find('.') > 0:
        family = socket.AF_INET
        errstr = ("IPv4 addresses must contain four numbers between 0 and 255, separated by periods.")
    elif ip_string.find(':') > 0 and ip_string.find('.') == -1:
        family = socket.AF_INET6
        errstr = ("'%s' is not a valid IPv6 address.") % ip_string
    else:
        raise IPError, ("'%s' is an invalid IP address.") % ip_string

    try:
        socket.inet_pton(family, ip_string)
    except socket.error:
        raise IPError, errstr


def getSubnetSize(mask):
    """Get the size (i.e., number of addresses) of a subnet

    :param mask: A dotted-quad netmask string
    :type mask: str
    :returns: The number of addresses in the subnet
    :rtype: int

    """
    
    maskcomp = 0
    for octet in mask.split('.'):
        maskcomp = (maskcomp<<8)+255-int(octet)
    return maskcomp

def netmaskToPrefix(mask):
    """Convert a dotted-quad netmask to an integer "prefix"

    :param mask: A dotted-quad netmask
    :type mask: str
    :returns: The number of prefix bits in the netmask
    :rtype: int
    :raises: ValueError

    """
    
    all_ones = (1<<32) - 1
    nm = dottedQuadToNum(mask)
    prefix = 32
    suffix = 0
    while (suffix | nm) < all_ones:
        suffix = (suffix<<1)+1
        prefix -= 1
    if suffix ^ nm == all_ones:
        return prefix
    else:
        raise ValueError, "netmask %s is not contiguous" % mask

def checkSubnet(net, mask):
    """Make sure the bitwise "AND" of network and netmask is zero.
       This could surely be done better using the socket library.

    :param net: A dotted-quad network string
    :type net: str
    :param mask: A dotted-quad netmask
    :type mask: str
    :returns: True if the combination is valid, else False
    :rtype: bool
    """
    
    return getNetworkRange(net, netmaskToPrefix(mask))[0] == net

def compareIPs(addr1, addr2):
    """Determine whether addr1 is less than, equal to, or greater
       than addr2, using the same convention that cmp does

    :param addr1: The first dotted-quad IP address
    :type addr1: str
    :param addr2: The second dotted-quad IP address
    :type addr2: str
    :returns: negative if addr1 < addr2, zero if addr1 == addr2 and strictly positive if addr1 > addr2.
    :rtype: int
    :raises: ValueError
    """    

    oct1 = [int(x) for x in addr1.split(".")]
    oct2 = [int(x) for x in addr2.split(".")]
    return cmp(oct1[0], oct2[0]) or cmp(oct1[1], oct2[1]) or \
           cmp(oct1[2], oct2[2]) or cmp(oct1[3], oct2[3])

def isAddressInSubnet(ip, subnetinfo):
    """Check whether a given address is inside a subnet,
       where the subnet is expressed as a network/netmask pair.

    :param ip: The dotted-quad IP address
    :type ip: str
    :param subnetinfo: The network/netmask pair defining the subnet.
    :type subnetinfo: tuple of strings
    :returns: True if ip is in the subnet, else False
    :rtype: bool
    """
    
    start, end = getNetworkRange(subnetinfo[0], subnetinfo[1])
    return (compareIPs(start, ip) < 0 and compareIPs(end, ip) > 0)

def getDevices():
    """Get a list of devices

    :return: A list of devices
    :rtype: list of strings

    .. note::

       This function does not filter out any devices such as bridges or
       slaves.  Higher level functions should do this.
    """
    devices = set()
    po = subprocess.Popen("ip -4 -o link show".split(),
                          stdout=subprocess.PIPE)
    out = po.communicate()[0]
    for line in out.strip().split("\n"):
        m = re.match('(\d+):\s+(\S+):\s+.*', line)
        if not m:
            pass
        else:
            devices.add(m.groups()[1])

    return devices


def getDeviceAddresses(dev):
    """Get all address/netmask pairs for a device
 
    :param dev: The network interface to query
    :type dev: str
    :return: A list of addresses bound to this interface
    :rtype: list of 2-tuples of strings

    .. note::

       Expected output format from the "ip" command:
       1: lo    inet 127.0.0.1/8 scope host lo
       2: eth0    inet 10.0.2.15/24 brd 10.0.2.255 scope global eth0
       3: br0    inet 169.254.169.254/32 scope link br0
       3: br0    inet 172.31.252.1/32 brd 172.31.252.1 scope global br0
    """

    addresses = []
    po = subprocess.Popen("ip -4 -o addr show".split(),
                          stdout=subprocess.PIPE)
    out = po.communicate()[0]
    for line in out.strip().split("\n"):
        m = re.match('(\d+):\s+(\S+)\s+inet\s+(\S+)(\s+brd (\S+))?\s+scope\s+(host|link|global)\s+(\S+)', line)
        if not m:
            pass
        else:
            match = m.groups()
            if match[5] != "host" and match[1] == dev:
                addresses.append(match[2].split('/'))

    return addresses

