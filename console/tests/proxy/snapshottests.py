#!/usr/bin/python

# Copyright 2012 Eucalyptus Systems, Inc.
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the following
# conditions are met:
#
#   Redistributions of source code must retain the above copyright notice,
#   this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer in the
#   documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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
