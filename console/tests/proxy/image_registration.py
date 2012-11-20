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

 
