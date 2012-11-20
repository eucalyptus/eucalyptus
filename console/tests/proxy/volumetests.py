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
