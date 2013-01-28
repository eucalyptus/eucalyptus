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

import boto
import ConfigParser
import json
from boto.ec2.connection import EC2Connection
from boto.ec2.regioninfo import RegionInfo

from .botojsonencoder import BotoJsonEncoder
from .clcinterface import ClcInterface

# This class provides an implmentation of the clcinterface using boto
class BotoClcInterface(ClcInterface):
    conn = None
    saveclcdata = False

    def __init__(self, clc_host, access_id, secret_key, token):
        #boto.set_stream_logger('foo')
        reg = RegionInfo(name='eucalyptus', endpoint=clc_host)
        path='/services/Eucalyptus'
        port=8773
        if clc_host[len(clc_host)-13:] == 'amazonaws.com':
            path = '/'
            reg = None
            port=443
        if boto.__version__ < '2.6':
            self.conn = EC2Connection(access_id, secret_key, region=reg,
                                  port=port, path=path,
                                  is_secure=True, security_token=token, debug=0)
        else:
            self.conn = EC2Connection(access_id, secret_key, region=reg,
                                  port=port, path=path, validate_certs=False,
                                  is_secure=True, security_token=token, debug=0)
        self.conn.APIVersion = '2012-03-01'
        self.conn.http_connection_kwargs['timeout'] = 30

    def __save_json__(self, obj, name):
        f = open(name, 'w')
        json.dump(obj, f, cls=BotoJsonEncoder, indent=2)
        f.close()

    def get_all_zones(self, filters, callback=None):
        obj = self.conn.get_all_zones(filters)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Zones.json")
        return obj

    def get_all_images(self, owners, filters, callback=None):
        obj = self.conn.get_all_images(owners=owners, filters=filters)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Images.json")
        return obj

    # returns list of image attributes
    def get_image_attribute(self, image_id, attribute):
        return self.conn.get_image_attribute(image_id, attribute)

    # returns True if successful
    def modify_image_attribute(self, image_id, attribute, operation, users, groups):
        return self.conn.modify_image_attribute(image_id, attribute, operation, users, groups)

    # returns True if successful
    def reset_image_attribute(self, image_id, attribute):
        return self.conn.reset_image_attribute(image_id, attribute)

    def get_all_instances(self, filters, callback=None):
        obj = self.conn.get_all_instances(filters)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Instances.json")
        return obj

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
        return self.conn.run_instances(image_id=image_id, min_count=min_count, max_count=max_count,
                      key_name=key_name, security_groups=security_groups,
                      user_data=user_data, addressing_type=addressing_type,
                      instance_type=instance_type, placement=placement,
                      kernel_id=kernel_id, ramdisk_id=ramdisk_id,
                      monitoring_enabled=monitoring_enabled, subnet_id=subnet_id,
                      block_device_map=block_device_map,
                      disable_api_termination=disable_api_termination,
                      instance_initiated_shutdown_behavior=instance_initiated_shutdown_behavior,
                      private_ip_address=private_ip_address,
                      placement_group=placement_group, client_token=client_token,
                      security_group_ids=security_group_ids)
                      # we're not using these, so leave them out so we're compatible with boto 2.2.2
                      #additional_info, instance_profile_name,
                      #instance_profile_arn, tenancy)

    # returns instance list
    def terminate_instances(self, instance_ids):
        return self.conn.terminate_instances(instance_ids)

    # returns instance list
    def stop_instances(self, instance_ids, force=False):
        return self.conn.stop_instances(instance_ids, force)

    # returns instance list
    def start_instances(self, instance_ids):
        return self.conn.start_instances(instance_ids)

    # returns instance status
    def reboot_instances(self, instance_ids):
        return self.conn.reboot_instances(instance_ids)

    # returns console output
    def get_console_output(self, instance_id):
        obj = self.conn.get_console_output(instance_id)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/ConsoleOutput.json")
        return obj

    # returns password data
    def get_password_data(self, instance_id):
        return self.conn.get_password_data(instance_id)

    def get_all_addresses(self, filters, callback=None):
        obj = self.conn.get_all_addresses(filters)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Addresses.json")
        return obj

    # returns address info
    def allocate_address(self):
        return self.conn.allocate_address()

    # returns True if successful
    def release_address(self, publicip):
        return self.conn.release_address(publicip)

    # returns True if successful
    def associate_address(self, publicip, instanceid):
        return self.conn.associate_address(instanceid, publicip)

    # returns True if successful
    def disassociate_address(self, publicip):
        return self.conn.disassociate_address(publicip)

    def get_all_key_pairs(self, filters, callback=None):
        obj = self.conn.get_all_key_pairs(filters)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Keypairs.json")
        return obj

    # returns keypair info and key
    def create_key_pair(self, key_name):
        return self.conn.create_key_pair(key_name)

    # returns nothing
    def delete_key_pair(self, key_name):
        return self.conn.delete_key_pair(key_name)

    # returns keypair info and key
    def import_key_pair(self, key_name, public_key_material):
        return self.conn.import_key_pair(key_name, public_key_material)

    def get_all_security_groups(self, filters, callback=None):
        obj = self.conn.get_all_security_groups(filters)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Groups.json")
        return obj

    # returns True if successful
    def create_security_group(self, name, description):
        return self.conn.create_security_group(name, description)

    # returns True if successful
    def delete_security_group(self, name=None, group_id=None, callback=None):
        return self.conn.delete_security_group(name, group_id)

    # returns True if successful
    def authorize_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        return self.conn.authorize_security_group_deprecated(name, 
                                 src_security_group_name,
                                 src_security_group_owner_id,
                                 ip_protocol, from_port, to_port,
                                 cidr_ip)#, group_id,
                                 #src_security_group_group_id)

    # returns True if successful
    def revoke_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        return self.conn.revoke_security_group_deprecated(name,
                                 src_security_group_name,
                                 src_security_group_owner_id,
                                 ip_protocol, from_port, to_port,
                                 cidr_ip)#, group_id,
                                 #src_security_group_group_id)

    def get_all_volumes(self, filters, callback=None):
        obj = self.conn.get_all_volumes(filters)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Volumes.json")
        return obj

    # returns volume info
    def create_volume(self, size, availability_zone, snapshot_id):
        return self.conn.create_volume(size, availability_zone, snapshot_id)

    # returns True if successful
    def delete_volume(self, volume_id):
        return self.conn.delete_volume(volume_id)

    # returns True if successful
    def attach_volume(self, volume_id, instance_id, device):
        return self.conn.attach_volume(volume_id, instance_id, device)

    # returns True if successful
    def detach_volume(self, volume_id, force=False):
        return self.conn.detach_volume(volume_id, None, None, force)

    def get_all_snapshots(self, filters, callback=None):
        obj = self.conn.get_all_snapshots(filters)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Snapshots.json")
        return obj

    # returns snapshot info
    def create_snapshot(self, volume_id, description):
        return self.conn.create_snapshot(volume_id, description)

    # returns True if successful
    def delete_snapshot(self, snapshot_id):
        return self.conn.delete_snapshot(snapshot_id)

    # returns list of snapshots attributes
    def get_snapshot_attribute(self, snapshot_id, attribute):
        return self.conn.get_snapshot_attribute(snapshot_id, attribute)

    # returns True if successful
    def modify_snapshot_attribute(self, snapshot_id, attribute, operation, users, groups):
        return self.conn.modify_snapshot_attribute(snapshot_id, attribute, operation, users, groups)

    # returns True if successful
    def reset_snapshot_attribute(self, snapshot_id, attribute):
        return self.conn.reset_snapshot_attribute(snapshot_id, attribute)

    # returs True if successful
    def deregister_image(self, image_id):
        return self.conn.deregister_image(image_id)

    def register_image(self, name, image_location=None, description=None, architecture=None, kernel_id=None, ramdisk_id=None, root_dev_name=None, block_device_map=None):
#        if is_windows:
#            kernel_id = 'windows'
        return self.conn.register_image(name, description, image_location, architecture, kernel_id, ramdisk_id, root_dev_name, block_device_map)

    def get_all_tags(self, filters):
        obj = self.conn.get_all_tags(filters)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Tags.json")
        return obj

    # returns tag info
    def create_tags(self, resourceIds, tags):
        return self.conn.create_tags(resourceIds, tags)

    # returns True if successful
    def delete_tags(self, resourceIds, tags):
        return self.conn.delete_tags(resourceIds, tags)
