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

import base64
#from poster.encode import multipart_encode
#from poster.streaminghttp import register_openers
import urllib
import urllib2
import json

# This is a client to test the interface that the browser uses
# to talk to the UI proxy. It can make all of the REST calls as you
# would from the browser GUI

class UIProxyClient(object):
    session_cookie = None
    xsrf = None

    def __init__(self):
        pass

    def login(self, host, port, account, username, password):
        # make request, storing cookie
        self.host = host
        self.port = port
        req = urllib2.Request("http://%s:%s/"%(host, port))
        encoded_auth = base64.encodestring("%s:%s:%s" % (account, username, password))[:-1]
        data = "action=login&remember=no&Authorization="+encoded_auth
        response = urllib2.urlopen(req, data)
        self.session_cookie = response.headers.get('Set-Cookie')
        print self.session_cookie
        idx = self.session_cookie.find('_xsrf=')+6
        self.xsrf = self.session_cookie[idx:idx+32]
        print "_xsrf="+self.xsrf
        print response.read()

    def logout(self):
        # forget cookie
        self.session_cookie = None

    def __check_logged_in__(self, request):
        if not(self.session_cookie):
            print "Need to login first!!"
        request.add_header('cookie', self.session_cookie)

    def __add_param_list__(self, params, name, list):
        for idx, val in enumerate(list):
            params["%s.%d" % (name, idx + 1)] = val

    def __make_request__(self, action, params):
        url = 'http://%s:%s/ec2?'%(self.host, self.port)
        for param in params.keys():
            if params[param]==None:
                del params[param]
        params['Action'] = action
        params['_xsrf'] = self.xsrf
        data = urllib.urlencode(params)
        try:
            req = urllib2.Request(url)
            self.__check_logged_in__(req)
            response = urllib2.urlopen(req, data)
            return json.loads(response.read())
        except urllib2.URLError, err:
            print "Error! "+str(err.code)

    ##
    # Zone methods
    ##
    def get_zones(self):
        return self.__make_request__('DescribeAvailabilityZones', {})

    ##
    # Image methods
    ##
    def get_images(self):
        return self.__make_request__('DescribeImages', {})

    def get_image_attribute(self, image_id, attribute='launchPermission'):
        return self.__make_request__('DescribeImageAttribute', {'ImageId': image_id, 'Attribute': attribute})

    def modify_image_attribute(self, image_id, attribute='launchPermission', operation='add', user_ids=None, groups=None):
        params = {'ImageId': image_id, 'Attribute': attribute, 'OperationType': operation}
        if user_ids:
            self.__add_param_list__(params, 'UserId', user_ids);
        if groups:
            self.__add_param_list__(params, 'UserGroup', groups);
        return self.__make_request__('ModifyImageAttribute', params)

    def reset_image_attribute(self, image_id, attribute='launchPermission'):
        return self.__make_request__('ResetImageAttribute', {'ImageId': image_id, 'Attribute': attribute})

    ##
    # Instance methods
    ##
    def get_instances(self):
        return self.__make_request__('DescribeInstances', {})

    def run_instances(self, image_id, min_count=1, max_count=1,
                      key_name=None, security_groups=None,
                      user_data=None, addressing_type=None,
                      instance_type='m1.small', placement=None,
                      kernel_id=None, ramdisk_id=None,
                      monitoring_enabled=False, subnet_id=None,
                      block_device_map=None,
                      disable_api_termination=False,
                      instance_initiated_shutdown_behavior=None,
                      private_ip_address=None,
                      placement_group=None, client_token=None,
                      security_group_ids=None,
                      additional_info=None, instance_profile_name=None,
                      instance_profile_arn=None, tenancy=None):
        params = {'ImageId':image_id,
                  'MinCount':min_count,
                  'MaxCount':max_count}
        if key_name:
            params['KeyName'] = key_name
        if security_group_ids:
            self.__add_param_list__(params, 'SecurityGroupId', security_group_ids);
        if security_groups:
            self.__add_param_list__(params, 'SecurityGroup', security_groups);
        if user_data:
            params['UserData'] = base64.b64encode(user_data)
        if addressing_type:
            params['AddressingType'] = addressing_type
        if instance_type:
            params['InstanceType'] = instance_type
        if placement:
            params['Placement.AvailabilityZone'] = placement
        if placement_group:
            params['Placement.GroupName'] = placement_group
        if tenancy:
            params['Placement.Tenancy'] = tenancy
        if kernel_id:
            params['KernelId'] = kernel_id
        if ramdisk_id:
            params['RamdiskId'] = ramdisk_id
        if monitoring_enabled:
            params['Monitoring.Enabled'] = 'true'
        if subnet_id:
            params['SubnetId'] = subnet_id
        if private_ip_address:
            params['PrivateIpAddress'] = private_ip_address
        if block_device_map:
            block_device_map.build_list_params(params)
        if disable_api_termination:
            params['DisableApiTermination'] = 'true'
        if instance_initiated_shutdown_behavior:
            val = instance_initiated_shutdown_behavior
            params['InstanceInitiatedShutdownBehavior'] = val
        if client_token:
            params['ClientToken'] = client_token
        if additional_info:
            params['AdditionalInfo'] = additional_info
        return self.__make_request__('RunInstances', params)

    def terminate_instances(self, instanceids):
        params = {}
        self.__add_param_list__(params, 'InstanceId', instanceids)
        return self.__make_request__('TerminateInstances', params)

    def stop_instances(self, instanceids):
        params = {}
        self.__add_param_list__(params, 'InstanceId', instanceids)
        return self.__make_request__('StopInstances', params)

    def start_instances(self, instanceids):
        params = {}
        self.__add_param_list__(params, 'InstanceId', instanceids)
        return self.__make_request__('StartInstances', params)

    def reboot_instances(self, instanceids):
        params = {}
        self.__add_param_list__(params, 'InstanceId', instanceids)
        return self.__make_request__('RebootInstances', params)

    def get_console_output(self, instanceid):
        return self.__make_request__('GetConsoleOutput', {'InstanceId': instanceid})

    def get_password(self, instanceid, keypair_file):
#        register_openers()
#        datagen, headers = multipart_encode({
#                                'Action': 'GetPassword',
#                                'InstanceId': instanceid,
#                                '_xsrf': self.xsrf,
#                                'priv_key': open(keypair_file)
#                                })
#
#        url = 'http://%s:%s/ec2?'%(self.host, self.port)
#        req = urllib2.Request(url, datagen, headers)
#        self.__check_logged_in__(req)
#        response = urllib2.urlopen(req)
#        return json.loads(response.read())
        pass

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
        return self.__make_request__('CreateSecurityGroup', {'GroupName': name, 'GroupDescription': base64.encodestring(description)})

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
        params = {'GroupName': name, 'GroupId': group_id}
        for i in range(1, len(ip_protocol)+1):
            if src_security_group_name:
                params['IpPermissions.%d.Groups.1.GroupName'%i] = src_security_group_name[i-1]
            if src_security_group_owner_id:
                params['IpPermissions.%d.Groups.1.UserId'%i] = src_security_group_owner_id[i-1]
            if src_security_group_group_id:
                params['IpPermissions.%d.Groups.1.GroupId'%i] = src_security_group_group_id[i-1]
            params['IpPermissions.%d.IpProtocol'%i] = ip_protocol[i-1]
            params['IpPermissions.%d.FromPort'%i] = from_port[i-1]
            params['IpPermissions.%d.ToPort'%i] = to_port[i-1]
            if cidr_ip:
                params['IpPermissions.%d.IpRanges.1.CidrIp' % i] = cidr_ip[i-1]
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

    def detach_volume(self, volume_id, force=False):
        return self.__make_request__('DetachVolume',
                    {'VolumeId': volume_id, 'Force': str(force)})

    ##
    # Snapshot methods
    ##
    def get_snapshots(self):
        return self.__make_request__('DescribeSnapshots', {})

    def create_snapshot(self, volume_id, description=None):
        params = {'VolumeId': volume_id}
        if description:
            params['Description'] = base64.b64encode(description)
        return self.__make_request__('CreateSnapshot', params)

    def delete_snapshot(self, snapshot_id):
        return self.__make_request__('DeleteSnapshot', {'SnapshotId': snapshot_id})

    def get_snapshot_attribute(self, snapshot_id, attribute='createVolumePermission'):
        return self.__make_request__('DescribeSnapshotAttribute', {'SnapshotId': snapshot_id, 'Attribute': attribute})

    def modify_snapshot_attribute(self, snapshot_id, attribute='createVolumePermission', operation='add', users=None, groups=None):
        params = {'SnapshotId': snapshot_id, 'Attribute': attribute, 'OperationType': operation}
        if users:
            self.__add_params_list__(params, 'UserId', users);
        if groups:
            self.__add_params_list__(params, 'UserGroup', groups);
        return self.__make_request__('ModifySnapshotAttribute', params)

    def reset_snapshot_attribute(self, snapshot_id, attribute='createVolumePermission'):
        return self.__make_request__('ResetSnapshotAttribute', {'SnapshotId': snapshot_id, 'Attribute': attribute})

    ##
    # Register/deregister image
    ##
    def register_snapshot_as_image(self, snapshot_id, name):
        return self.__make_request__('RegisterImage', {'SnapshotId': snapshot_id, 'Name': name})
    def deregister_image(self, image_id):
        return self.__make_request__('DeregisterImage', {'ImageId': image_id})
