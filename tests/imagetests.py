#!/usr/bin/python

import time
from operator import itemgetter
from uiproxyclient import UIProxyClient

if __name__ == "__main__":
    image_id = 'emi-F2043872'

    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('localhost', '8888', 'test', 'admin', 'testing123')

    print 
    print "=== listing images ==="
    print 
    print client.get_images()
    print 
    print 
    print "=== getting image attribute ==="
    print 
    print client.get_image_attribute(image_id)
    print 
    print "=== setting image attribute ==="
    print 
    print client.modify_image_attribute(image_id, groups=['all'], attribute='launchPermission', operation='remove')
    print 
    print "=== getting image attribute ==="
    print 
    print client.get_image_attribute(image_id)
    print 
    print "=== resetting image attribute ==="
    print 
    print client.reset_image_attribute(image_id, attribute='launchPermission')
    print 
    print "=== getting image attribute ==="
    print 
    print client.get_image_attribute(image_id)
