#!/usr/bin/python

from uiproxyclient import UIProxyClient

if __name__ == "__main__":
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('localhost', '8888', 'testuser1', 'admin', 'euca123')
    print "=== Getting Zones ==="
    print client.get_zones()
