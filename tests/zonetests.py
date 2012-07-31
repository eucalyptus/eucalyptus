#!/usr/bin/python

from uiproxyclient import UIProxyClient

if __name__ == "__main__":
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('test', 'admin', 'testing123')
    print "=== Getting Zones ==="
    print client.get_zones()
