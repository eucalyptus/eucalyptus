#!/usr/bin/python

from uiproxyclient import UIProxyClient

if __name__ == "__main__":
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('localhost', '8888', 'test', 'admin', 'testing123')
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
