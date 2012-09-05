#!/usr/bin/python

import time
from operator import itemgetter
from uiproxyclient import UIProxyClient

if __name__ == "__main__":
    instance_id = 'i-45FD403F'

    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('localhost', '8888', 'test', 'admin', 'testing123')

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
        time.sleep(3)
        volumes = client.get_volumes()
    print volumes
    print 
    print "=== attaching volume to instance ==="
    print 
    print client.attach_volume(volid, instance_id, '/dev/sdd')
    volumes = client.get_volumes()
    while volumes['results'][map(itemgetter('id'), volumes['results']).index(volid)]['attach_data']['status'] != 'attached':
        time.sleep(3)
        volumes = client.get_volumes()
    print client.get_volumes()
    print 
    print "=== detaching volume from instance ==="
    print 
    print client.detach_volume(volid)
    volumes = client.get_volumes()
    while volumes['results'][map(itemgetter('id'), volumes['results']).index(volid)]['status'] != 'available':
        time.sleep(3)
        volumes = client.get_volumes()
    
    print client.get_volumes()
    print 
    print "=== deleting test volume ==="
    print 
    print client.delete_volume(volid)
