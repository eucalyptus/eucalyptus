#!/usr/bin/python

from uiproxyclient import UIProxyClient

if __name__ == "__main__":
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('localhost', '8888', 'test', 'admin', 'testing123')
    print 
    print "=== listing keypairs ==="
    print 
    keypairs = client.get_keypairs()
    print keypairs
    print 
    print "=== creating keypair ==="
    print 
    keyinfo = client.create_keypair("testkey01")
    print keyinfo
    print 
    print "=== listing keypairs ==="
    print 
    print client.get_keypairs()
    print 
    print "=== deleting test keypair ==="
    print 
    print client.delete_keypair("testkey01")
