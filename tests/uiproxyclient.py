#!/usr/bin/python

import urllib2
import json
from operator import itemgetter

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
        data = "action=login&username=" + username + "&password=" + password
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

    def __make_request__(self, action, params):
        url = 'http://localhost:8888/ec2?Action=' + action
        for k in params.keys():
            url = url + '&' + k + '=' + params[k]
        req = urllib2.Request(url)
        self.__check_logged_in__(req)
        response = urllib2.urlopen(req)
        return json.loads(response.read())

    def get_zones(self):
        return self.__make_request__('DescribeAvailabilityZones', {})

    def get_keypairs(self):
        return self.__make_request__('DescribeKeyPairs', {})

    def create_keypair(self, name):
        return self.__make_request__('CreateKeyPair', {'KeyName': name})

    def delete_keypair(self, name):
        return self.__make_request__('DeleteKeyPair', {'KeyName': name})

    def get_volumes(self):
        return self.__make_request__('DescribeVolumes', {})

    def create_volume(self, size, zone, snapshot_id):
        params = {'Size': size, 'AvailabilityZone': zone}
        if snapshot_id:
            params['SnapshotId'] = snapshot_id
        return self.__make_request__('CreateVolume', params)

    def delete_volume(self, volume_id):
        return self.__make_request__('DeleteVolume', {'VolumeId': volume_id})

    def attach_volume(self, volume_id, instance_id, device):
        return self.__make_request__('AttachVolume',
                    {'VolumeId': volume_id, 'InstanceId': instance_id, 'Device': device})

    def detach_volume(self, volume_id, instance_id, device, force=False):
        return self.__make_request__('DetachVolume',
                    {'VolumeId': volume_id, 'InstanceId': instance_id, 'Device': device, 'Force': str(force)})

if __name__ == "__main__":
    # make some calls to proxy class to test things out
    client = UIProxyClient()
    client.login('test', 'test')
    print "=== Getting Zones ==="
    print client.get_zones()
#    print 
#    print "=== listing keypairs ==="
#    print 
#    keypairs = client.get_keypairs()
#    print keypairs
#    print 
#    print "=== creating keypair ==="
#    print 
#    keyinfo = client.create_keypair("testkey01")
#    print keyinfo
#    print 
#    print "=== listing keypairs ==="
#    print 
#    print client.get_keypairs()
#    print 
#    print "=== deleting test keypair ==="
#    print 
#    print client.delete_keypair("testkey01")

#    print 
#    print "=== listing volumes ==="
#    print 
#    print client.get_volumes()
#    print 
#    print "=== creating volume ==="
#    print 
#    volinfo = client.create_volume('1', 'PARTI00', None)
#    print volinfo
#    print volinfo['results']['id']
#    print volinfo['results']['status']
#    volid = volinfo['results']['id']
#    print 
#    print "=== listing volumes ==="
#    print 
#    volumes = client.get_volumes()
#    print volumes
#    print 
#    print "=== waiting for new volume to be ready ==="
#    print 
#    while volumes['results'][map(itemgetter('id'), volumes['results']).index(volid)]['status'] != 'available':
#        volumes = client.get_volumes()
#    print volumes
#    print 
#    print "=== attaching volume to instance ==="
#    print 
#    print client.attach_volume(volid, 'i-F6574412', '/dev/sdd')
#    volumes = client.get_volumes()
#    while volumes['results'][map(itemgetter('id'), volumes['results']).index(volid)]['attach_data']['status'] != 'attached':
#        volumes = client.get_volumes()
#    print client.get_volumes()
#    print 
#    print "=== attaching volume to instance ==="
#    print 
#    print client.detach_volume(volid, 'i-F6574412', '/dev/sdd')
#    volumes = client.get_volumes()
#    while volumes['results'][map(itemgetter('id'), volumes['results']).index(volid)]['status'] != 'available':
#        volumes = client.get_volumes()
#    
#    print client.get_volumes()
#    print 
#    print "=== deleting test volume ==="
#    print 
#    print client.delete_volume(volid)
