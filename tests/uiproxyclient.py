
import base64
import urllib2
import json

# This is a client to test the interface that the browser uses
# to talk to the UI proxy. It can make all of the REST calls as you
# would from the browser GUI

class UIProxyClient(object):
    session_cookie = None

    def __init__(self):
        pass

    def login(self, account, username, password):
        # make request, storing cookie
        req = urllib2.Request("http://localhost:8888/")
        data = "action=login"
        encoded_auth = base64.encodestring("%s:%s:%s" % (account, username, password))[:-1]
        print "encoded "+encoded_auth
        req.add_header('Authorization', "Basic %s" % encoded_auth)
        response = urllib2.urlopen(req, data)
        self.session_cookie = response.headers.get('Set-Cookie')
        print self.session_cookie
        print response.read()

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

