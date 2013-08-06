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

from uiproxyclient import UIProxyClient

if __name__ == "__main__":
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('localhost', '8888', 'ui-test-acct-03', 'admin', 'mypassword6')
    print 
    print "=== listing groups ==="
    print 
    groups = client.get_security_groups()
    print groups
    print 
    print "=== creating group ==="
    print 
    groupinfo = client.create_security_group("test_grp", "group for testing")
    print groupinfo
    print 
    print "=== listing groups ==="
    print 
    print client.get_security_groups()
    print 
    print "=== authorizing port 22 ingress ==="
    print 
    print client.authorize_security_group(name="test_grp",
                                ip_protocol=['tcp', 'tcp'],
                                from_port=['22', '22'],
                                to_port=['22', '22'],
                                cidr_ip=['10.0.0.0/24', '10.20.30.0/24'])
    print 
    print "=== listing groups ==="
    print 
    print client.get_security_groups()
    print 
    print "=== revoking port 22 ingress ==="
    print 
    print client.revoke_security_group(name="test_grp", ip_protocol='tcp', from_port='22', to_port='22', cidr_ip=['10.0.0.0/24'])
    print client.revoke_security_group(name="test_grp", ip_protocol='tcp', from_port='22', to_port='22', cidr_ip=['10.20.30.0/24'])
    print 
    print "=== listing groups ==="
    print 
    print client.get_security_groups()
    print 
    print "=== deleting test group ==="
    print 
    print client.delete_security_group(name="test_grp")
