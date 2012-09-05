#!/usr/bin/python

import time
from operator import itemgetter
from uiproxyclient import UIProxyClient

if __name__ == "__main__":
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('localhost', '8888', 'test', 'admin', 'testing123')
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
    instanceinfo = client.run_instances(emi, instance_type='m1.large')
    print instanceinfo
    instanceid = instanceinfo['results']['instances'][0]['id']
    print 
    print "=== listing instances ==="
    print 
    instances = client.get_instances()
    while instances:
        for res in instances['results']:
            for inst in res:
                print inst['id']
                if inst['id']==instanceid:
                    print "status: "+inst['status']
        time.sleep(5)
        instances = client.get_instances()

