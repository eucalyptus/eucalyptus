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

def waitForState (client, instanceid, state):
    instances = client.get_instances()
    while instances:
        for inst in instances['results']:
            if inst['id']==instanceid:
                print inst['state']
                if inst['state'] == state:
                    return;
        time.sleep(5)
        instances = client.get_instances()

if __name__ == "__main__":
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('localhost', '8888', 'ui-test-acct-03', 'admin', 'mypassword6')
    print 
    print "=== listing images ==="
    print 
    images = client.get_images()
    print images
    for i in images['results']:
        if i['id'].find('emi') == 0:
            emi = i['id']
    print "image to run: "+emi
    print 
    print "=== listing instances ==="
    print 
    instances = client.get_instances()
    print instances
    print 
    print "=== launching instance ==="
    print 
    instanceinfo = client.run_instances(emi, instance_type='c1.medium')
    print instanceinfo
    instanceid = instanceinfo['results'][0]['id']
    print 
    print "=== listing instances ==="
    print 
    waitForState(client, instanceid, 'running')

    print 
    print "=== console output ==="
    print 
    print client.get_console_output(instanceid)
    print 
    print "=== rebooting instance ==="
    print 
    print client.reboot_instances([instanceid])
    print 
    print "=== listing instances ==="
    print 
    print client.get_instances()
    waitForState(client, instanceid, 'running')

    print 
    print "=== terminating instance ==="
    print 
    print client.terminate_instances([instanceid])
    waitForState(client, instanceid, 'terminated')
    print 
    print "=== listing instances ==="
    print 
    print client.get_instances()
