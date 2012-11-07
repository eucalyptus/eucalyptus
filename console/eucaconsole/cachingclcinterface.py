from cache import Cache
from threading import Thread
import ConfigParser

from boto.ec2.image import Image
from boto.ec2.instance import Instance
from boto.ec2.keypair import KeyPair
from eucaconsole.threads import Threads

from .clcinterface import ClcInterface

# This class provides an implmentation of the clcinterface that caches responses
# from the underlying clcinterface. It will only make requests to the underlying layer
# at the rate defined by pollfreq. It is assumed this will be created per-session and
# therefore will only contain data for a single user. If a more global cache is desired,
# some things will need to be re-written.
class CachingClcInterface(ClcInterface):
    clc = None

    # load saved state to simulate CLC
    def __init__(self, clcinterface, config):
        self.clc = clcinterface
        pollfreq = config.getint('server', 'pollfreq')
        try:
            freq = config.getint('server', 'pollfreq.zones')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.zones = Cache(freq)

        try:
            freq = config.getint('server', 'pollfreq.images')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.images = Cache(freq)

        try:
            freq = config.getint('server', 'pollfreq.instances')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.instances = Cache(freq)

        try:
            freq = config.getint('server', 'pollfreq.keypairs')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.keypairs = Cache(freq)

        try:
            freq = config.getint('server', 'pollfreq.groups')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.groups = Cache(freq)

        try:
            freq = config.getint('server', 'pollfreq.addresses')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.addresses = Cache(freq)

        try:
            freq = config.getint('server', 'pollfreq.volumes')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.volumes = Cache(freq)

        try:
            freq = config.getint('server', 'pollfreq.snapshots')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.snapshots = Cache(freq)

    def get_all_zones(self, callback):
        # if cache stale, update it
        if self.zones.isCacheStale():
            Threads.instance().runThread(self.__get_all_zones_cb__, ({}, callback))
        else:
            callback(Response(data=self.zones.values))

    def __get_all_zones_cb__(self, kwargs, callback):
        self.zones.values = self.clc.get_all_zones()
        Threads.instance().invokeCallback(callback, Response(data=self.zones.values))

    def get_all_images(self, owners, callback):
        if self.images.isCacheStale():
            Threads.instance().runThread(self.__get_all_images_cb__, ({'owners': owners}, callback))
        else:
            callback(Response(data=self.images.values))

    def __get_all_images_cb__(self, kwargs, callback):
        self.images.values = self.clc.get_all_images(kwargs['owners'])
        Threads.instance().invokeCallback(callback, Response(data=self.images.values))

    # returns list of image attributes
    def get_image_attribute(self, image_id, attribute):
        return self.clc.get_image_attribute(image_id, attribute)

    # returns True if successful
    def modify_image_attribute(self, image_id, attribute, operation, users, groups):
        self.images.expireCache()
        return self.clc.modify_image_attribute(image_id, attribute, operation, users, groups)

    # returns True if successful
    def reset_image_attribute(self, image_id, attribute):
        self.images.expireCache()
        return self.clc.reset_image_attribute(image_id, attribute)

    def get_all_instances(self, callback):
        if self.instances.isCacheStale():
            Threads.instance().runThread(self.__get_all_instances_cb__, ({}, callback))
        else:
            callback(Response(data=self.instances.values))

    def __get_all_instances_cb__(self, kwargs, callback):
        self.instances.values = self.clc.get_all_instances()
        Threads.instance().invokeCallback(callback, Response(data=self.instances.values))

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
        self.instances.expireCache()
        return self.clc.run_instances(image_id, min_count, max_count,
                      key_name, security_groups,
                      user_data, addressing_type,
                      instance_type, placement,
                      kernel_id, ramdisk_id,
                      monitoring_enabled, subnet_id,
                      block_device_map,
                      disable_api_termination,
                      instance_initiated_shutdown_behavior,
                      private_ip_address,
                      placement_group, client_token,
                      security_group_ids,
                      additional_info, instance_profile_name,
                      instance_profile_arn, tenancy)

    # returns instance list
    def terminate_instances(self, instance_ids):
        self.instances.expireCache()
        return self.clc.terminate_instances(instance_ids)

    # returns instance list
    def stop_instances(self, instance_ids, force=False):
        self.instances.expireCache()
        return self.clc.stop_instances(instance_ids, force)

    # returns instance list
    def start_instances(self, instance_ids):
        self.instances.expireCache()
        return self.clc.start_instances(instance_ids)

    # returns instance status
    def reboot_instances(self, instance_ids):
        self.instances.expireCache()
        return self.clc.reboot_instances(instance_ids)

    # returns console output
    def get_console_output(self, instance_id):
        return self.clc.get_console_output(instance_id)

    # returns password data
    def get_password_data(self, instance_id):
        return self.clc.get_password_data(instance_id)

    def get_all_addresses(self, callback):
        if self.addresses.isCacheStale():
            Threads.instance().runThread(self.__get_all_addresses_cb__, ({}, callback))
        else:
            callback(Response(data=self.addresses.values))

    def __get_all_addresses_cb__(self, kwargs, callback):
        self.addresses.values = self.clc.get_all_addresses()
        Threads.instance().invokeCallback(callback, Response(data=self.addresses.values))

    # returns address info
    def allocate_address(self):
        self.addresss.expireCache()
        return self.clc.allocate_address()

    # returns True if successful
    def release_address(self, publicip):
        self.addresss.expireCache()
        return self.clc.release_address(publicip)

    # returns True if successful
    def associate_address(self, publicip, instanceid):
        self.addresss.expireCache()
        return self.clc.associate_address(publicip, instanceid)

    # returns True if successful
    def disassociate_address(self, publicip):
        self.addresss.expireCache()
        return self.clc.disassociate_address(publicip)

    def get_all_key_pairs(self, callback):
        if self.keypairs.isCacheStale():
            Threads.instance().runThread(self.__get_all_key_pairs_cb__, ({}, callback))
        else:
            callback(Response(data=self.keypairs.values))

    def __get_all_key_pairs_cb__(self, kwargs, callback):
        self.keypairs.values = self.clc.get_all_key_pairs()
        Threads.instance().invokeCallback(callback, Response(data=self.keypairs.values))

    # returns keypair info and key
    def create_key_pair(self, key_name):
        self.keypairs.expireCache()
        return self.clc.create_key_pair(key_name)

    # returns nothing
    def delete_key_pair(self, key_name):
        self.keypairs.expireCache()
        return self.clc.delete_key_pair(key_name)

    # returns keypair info and key
    def import_key_pair(self, key_name, public_key_material):
        self.keypairs.expireCache()
        return self.clc.import_key_pair(key_name, public_key_material)

    def get_all_security_groups(self, callback):
        if self.groups.isCacheStale():
            Threads.instance().runThread(self.__get_all_security_groups_cb__, ({}, callback))
        else:
            callback(Response(data=self.groups.values))

    def __get_all_security_groups_cb__(self, kwargs, callback):
        self.groups.values = self.clc.get_all_security_groups()
        Threads.instance().invokeCallback(callback, Response(data=self.groups.values))

    # returns True if successful
    def create_security_group(self, name, description, callback):
        self.groups.expireCache()
        Threads.instance().runThread(self.__create_security_group_cb__,
                    ({'name':name, 'description':description}, callback))

    def __create_security_group_cb__(self, kwargs, callback):
        ret = self.clc.create_security_group(kwargs['name'], kwargs['description'])
        Threads.instance().invokeCallback(callback, Response(data=ret))

    # returns True if successful
    def delete_security_group(self, name=None, group_id=None, callback=None):
        # invoke this on a separate thread
        self.groups.expireCache()
        Threads.instance().runThread(self.__delete_security_group_cb__,
                    ({'name':name, 'group_id':group_id}, callback))

    def __delete_security_group_cb__(self, kwargs, callback):
        ret = self.clc.delete_security_group(kwargs['name'], kwargs['group_id'])
        #pass results back using callback on main thread
        Threads.instance().invokeCallback(callback, Response(data=ret))

    # returns True if successful
    def authorize_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        self.groups.expireCache()
        return self.clc.authorize_security_group(name, 
                                 src_security_group_name,
                                 src_security_group_owner_id,
                                 ip_protocol, from_port, to_port,
                                 cidr_ip, group_id,
                                 src_security_group_group_id)

    # returns True if successful
    def revoke_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        self.groups.expireCache()
        return self.clc.revoke_security_group(name,
                                 src_security_group_name,
                                 src_security_group_owner_id,
                                 ip_protocol, from_port, to_port,
                                 cidr_ip, group_id,
                                 src_security_group_group_id)

    def get_all_volumes(self, callback):
        if self.volumes.isCacheStale():
            Threads.instance().runThread(self.__get_all_volumes_cb__, ({}, callback))
        else:
            callback(Response(data=self.volumes.values))

    def __get_all_volumes_cb__(self, kwargs, callback):
        self.volumes.values = self.clc.get_all_volumes()
        Threads.instance().invokeCallback(callback, Response(data=self.volumes.values))

    # returns volume info
    def create_volume(self, size, availability_zone, snapshot_id):
        self.volumes.expireCache()
        return self.clc.create_volume(size, availability_zone, snapshot_id)

    # returns True if successful
    def delete_volume(self, volume_id):
        self.volumes.expireCache()
        return self.clc.delete_volume(volume_id)

    # returns True if successful
    def attach_volume(self, volume_id, instance_id, device):
        self.volumes.expireCache()
        return self.clc.attach_volume(volume_id, instance_id, device)

    # returns True if successful
    def detach_volume(self, volume_id, force=False):
        self.volumes.expireCache()
        return self.clc.detach_volume(volume_id, force)

    def get_all_snapshots(self, callback):
        if self.snapshots.isCacheStale():
            Threads.instance().runThread(self.__get_all_snapshots_cb__, ({}, callback))
        else:
            callback(Response(data=self.snapshots.values))

    def __get_all_snapshots_cb__(self, kwargs, callback):
        self.snapshots.values = self.clc.get_all_snapshots()
        Threads.instance().invokeCallback(callback, Response(data=self.snapshots.values))

    # returns snapshot info
    def create_snapshot(self, volume_id, description):
        self.snapshots.expireCache()
        return self.clc.create_snapshot(volume_id, description)

    # returns True if successful
    def delete_snapshot(self, snapshot_id):
        self.snapshots.expireCache()
        return self.clc.delete_snapshot(snapshot_id)

    # returns list of snapshots attributes
    def get_snapshot_attribute(self, snapshot_id, attribute):
        self.snapshots.expireCache()
        return self.clc.get_snapshot_attribute(snapshot_id, attribute)

    # returns True if successful
    def modify_snapshot_attribute(self, snapshot_id, attribute, operation, users, groups):
        self.snapshots.expireCache()
        return self.clc.modify_snapshot_attribute(snapshot_id, attribute, operation, users, groups)

    # returns True if successful
    def reset_snapshot_attribute(self, snapshot_id, attribute):
        self.snapshots.expireCache()
        return self.clc.reset_snapshot_attribute(snapshot_id, attribute)

    # returns True if successful
    def register_image(self, name, image_location=None, description=None, architecture=None, kernel_id=None, ramdisk_id=None, root_dev_name=None, block_device_map=None):
        self.images.expireCache()
        return self.clc.register_image(name, image_location, description, architecture, kernel_id, ramdisk_id, root_dev_name, block_device_map)

class Response(object):
    data = None
    error = None

    def __init__(self, data=None, error=None):
        self.data = data
        self.error = error
