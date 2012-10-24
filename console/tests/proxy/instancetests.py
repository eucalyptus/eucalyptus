#!/usr/bin/python

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
    client.login('localhost', '8888', 'testuser1', 'admin', 'euca123')
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
