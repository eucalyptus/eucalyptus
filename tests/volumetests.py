#!/usr/bin/python

from operator import itemgetter
from uiproxyclient import UIProxyClient

if __name__ == "__main__":
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('test', 'admin', 'testing123')

    print 
    print "=== listing volumes ==="
    print 
    print client.get_volumes()
    print 
    print "=== creating volume ==="
    print 
    volinfo = client.create_volume('1', 'PARTI00', None)
    print volinfo
    print volinfo['results']['id']
    print volinfo['results']['status']
    volid = volinfo['results']['id']
    print 
    print "=== listing volumes ==="
    print 
    volumes = client.get_volumes()
    print volumes
    print 
    print "=== waiting for new volume to be ready ==="
    print 
    while volumes['results'][map(itemgetter('id'), volumes['results']).index(volid)]['status'] != 'available':
        volumes = client.get_volumes()
    print volumes
    print 
    print "=== attaching volume to instance ==="
    print 
    print client.attach_volume(volid, 'i-F6574412', '/dev/sdd')
    volumes = client.get_volumes()
    while volumes['results'][map(itemgetter('id'), volumes['results']).index(volid)]['attach_data']['status'] != 'attached':
        volumes = client.get_volumes()
    print client.get_volumes()
    print 
    print "=== attaching volume to instance ==="
    print 
    print client.detach_volume(volid, 'i-F6574412', '/dev/sdd')
    volumes = client.get_volumes()
    while volumes['results'][map(itemgetter('id'), volumes['results']).index(volid)]['status'] != 'available':
        volumes = client.get_volumes()
    
    print client.get_volumes()
    print 
    print "=== deleting test volume ==="
    print 
    print client.delete_volume(volid)
