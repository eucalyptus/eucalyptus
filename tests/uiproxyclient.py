#!/usr/bin/python

import urllib2
import json

# This is a client to test the interface that the browser uses
# to talk to the UI proxy. It can make all of the REST calls as you
# would from the browser GUI

class UIProxyClient(object):
    session_cookie = None

    def __init__(self):
        pass

    def login(self, username, password):
        # make request, storing cookie
        req = urllib2.Request("http://localhost:8888")
        data = "action=login&username="+username+"&password="+password
        response = urllib2.urlopen(req, data)
        self.session_cookie = response.headers.get('Set-Cookie')
        print self.session_cookie
    #    print response.read()

    def logout(self):
        # forget cookie
        self.session_cookie = None

    def __check_logged_in__(self, request):
        if not(self.session_cookie):
            print "Need to login first!!"
        request.add_header('cookie', self.session_cookie)

    def get_keypairs(self):
        req = urllib2.Request('http://localhost:8888/ec2?Action=DescribeKeyPairs')
        self.__check_logged_in__(req)
        response = urllib2.urlopen(req)
        return json.loads(response.read())

    def create_keypair(self, name):
        req = urllib2.Request('http://localhost:8888/ec2?Action=CreateKeyPair&KeyName='+name)
        self.__check_logged_in__(req)
        response = urllib2.urlopen(req)
        return json.loads(response.read())

    def delete_keypair(self, name):
        req = urllib2.Request('http://localhost:8888/ec2?Action=DeleteKeyPair&KeyName='+name)
        self.__check_logged_in__(req)
        response = urllib2.urlopen(req)
        return json.loads(response.read())

if __name__ == "__main__":
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('test', 'test')
    keypairs = client.get_keypairs()
    print 
    print "=== listing keypairs ==="
    print 
    print keypairs
    print 
    print "=== creating keypair ==="
    print 
    keyinfo = client.create_keypair("testkey01")
    print keyinfo
    print 
    print "=== listing keypairs ==="
    print 
    print client.get_keypairs()
    print 
    print "=== deleting test keypair ==="
    print 
    print client.delete_keypair("testkey01")

