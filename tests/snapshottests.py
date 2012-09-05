#!/usr/bin/python

from operator import itemgetter
from uiproxyclient import UIProxyClient

if __name__ == "__main__":
    volume_id = 'vol-CC463B31'

    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('localhost', '8888', 'test', 'admin', 'testing123')

    print 
    print "=== listing snapshots ==="
    print 
    print client.get_snapshots()
    print 
    print "=== creating snapshot ==="
    print 
    snapinfo = client.create_snapshot(volume_id)
    print snapinfo
    print snapinfo['results']['id']
    print snapinfo['results']['status']
    snapid = snapinfo['results']['id']
    print 
    print "=== listing snapshots ==="
    print 
    snapshots = client.get_snapshots()
    print snapshots
    print 
    print "=== waiting for new snapshots to be ready ==="
    print 
    while snapshots['results'][map(itemgetter('id'), snapshots['results']).index(snapid)]['status'] != 'completed':
        snapshots = client.get_snapshots()
    print snapshots
## NOTE: Euca does not support snapshot attributes!
#    print 
#    print "=== getting snapshot attribute ==="
#    print 
#    print client.get_snapshot_attribute(snapid)
#    print 
#    print "=== setting snapshot attribute ==="
#    print 
#    print client.modify_snapshot_attribute(snapid, groups=['default'])
#    print 
#    print "=== getting snapshot attribute ==="
#    print 
#    print client.get_snapshot_attribute(snapid)
#    print 
#    print "=== resetting snapshot attribute ==="
#    print 
#    print client.reset_snapshot_attribute(snapid)
    print 
    print "=== deleting test snapshot ==="
    print 
    print client.delete_snapshot(snapid)
