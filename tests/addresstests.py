#!/usr/bin/python

from uiproxyclient import UIProxyClient

if __name__ == "__main__":
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('localhost', '8888', 'test', 'admin', 'testing123')
    print 
    print "=== listing addresses ==="
    print 
    addresses = client.get_addresses()
    print addresses
    print 
    print "=== allocate address ==="
    print 
    addressinfo = client.allocate_address()
    print addressinfo
    print 
    print "=== listing keypairs ==="
    print 
    print client.get_addresses()
    print 
    print "=== releasing address ==="
    print 
    print client.release_address(addressinfo['results']['public_ip'])
