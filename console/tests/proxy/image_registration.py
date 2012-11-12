#!/usr/bin/python

import time
import sys
from operator import itemgetter
from uiproxyclient import UIProxyClient

if __name__ == "__main__":
    name = 'Test_Image-%(t)i' % {'t':time.time()}
    snapshot_id = None
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('localhost', '8888', 'testuser1', 'admin', 'euca123')
    print
    print "=== getting snapshots ==="
    print
    snashosts = client.get_snapshots()
    if len(snashosts['results'])>0:
      snapshot_id = snashosts['results'][0]['id']
    else:
      sys.exit()
    print "=== registering snapshot ==="
    print
    print 'Name: %(n)s' % {'n':name}
    print 'Snapshot id: %(n)s' % {'n':snapshot_id}
    res = client.register_snapshot_as_image(snapshot_id, name)
    print 'Image ID: %(n)s' % {'n':res['results']}
    print "=== deregistering image ==="
    print
    print client.deregister_image(res['results'])

 
