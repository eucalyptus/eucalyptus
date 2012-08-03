
import base64
import urllib
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
        for param in params.keys():
            if params[param]==None:
                del params[param]
        params['Action'] = action
        url = 'http://localhost:8888/ec2?' + urllib.urlencode(params)
        req = urllib2.Request(url)
        self.__check_logged_in__(req)
        response = urllib2.urlopen(req)
        return json.loads(response.read())

    ##
    # Zone methods
    ##
    def get_zones(self):
        return self.__make_request__('DescribeAvailabilityZones', {})

    ##
    # Keypair methods
    ##
    def get_keypairs(self):
        return self.__make_request__('DescribeKeyPairs', {})

    def create_keypair(self, name):
        return self.__make_request__('CreateKeyPair', {'KeyName': name})

    def delete_keypair(self, name):
        return self.__make_request__('DeleteKeyPair', {'KeyName': name})

    def get_keypairs(self):
        return self.__make_request__('DescribeKeyPairs', {})

    ##
    # Security Group methods
    ##
    def get_security_groups(self):
        return self.__make_request__('DescribeSecurityGroups', {})

    # returns True if successful
    def create_security_group(self, name, description):
        return self.__make_request__('CreateSecurityGroup', {'GroupName': name, 'GroupDescription': description})

    # returns True if successful
    def delete_security_group(self, name=None, group_id=None):
        return self.__make_request__('DeleteSecurityGroup', {'GroupName': name, 'GroupId': group_id})

    # returns True if successful
    def authorize_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        params = {'GroupName': name, 'GroupId': group_id,
                  'IpPermissions.1.Groups.1.GroupName': src_security_group_name,
                  'IpPermissions.1.Groups.1.UserId': src_security_group_owner_id,
                  'IpPermissions.1.Groups.1.GroupId': src_security_group_group_id,
                  'IpPermissions.1.IpProtocol': ip_protocol,
                  'IpPermissions.1.FromPort': from_port,
                  'IpPermissions.1.ToPort': to_port}
        if cidr_ip:
            if not isinstance(cidr_ip, list):
                cidr_ip = [cidr_ip]
            for i, single_cidr_ip in enumerate(cidr_ip):
                params['IpPermissions.1.IpRanges.%d.CidrIp' % (i+1)] = \
                    single_cidr_ip
        return self.__make_request__('AuthorizeSecurityGroupIngress', params)

    # returns True if successful
    def revoke_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        params = {'GroupName': name, 'GroupId': group_id,
                  'IpPermissions.1.Groups.1.GroupName': src_security_group_name,
                  'IpPermissions.1.Groups.1.UserId': src_security_group_owner_id,
                  'IpPermissions.1.Groups.1.GroupId': src_security_group_group_id,
                  'IpPermissions.1.IpProtocol': ip_protocol,
                  'IpPermissions.1.FromPort': from_port,
                  'IpPermissions.1.ToPort': to_port}
        if cidr_ip:
            if not isinstance(cidr_ip, list):
                cidr_ip = [cidr_ip]
            for i, single_cidr_ip in enumerate(cidr_ip):
                params['IpPermissions.1.IpRanges.%d.CidrIp' % (i+1)] = \
                    single_cidr_ip
        return self.__make_request__('RevokeSecurityGroupIngress', params)

    ##
    # Addresss methods
    ##
    def get_addresses(self):
        return self.__make_request__('DescribeAddresses', {})

    def allocate_address(self):
        return self.__make_request__('AllocateAddress', {})

    def release_address(self, publicip):
        return self.__make_request__('ReleaseAddress', {'PublicIp': publicip})

    def associate_address(self, publicip, instanceid):
        return self.__make_request__('AssociateAddress', {'PublicIp': publicip, 'InstanceId': instanceid})

    def disassociate_address(self, publicip):
        return self.__make_request__('DisassociateAddress', {'PublicIp': publicip})

    ##
    # Volume methods
    ##
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

