import ConfigParser
from datetime import datetime, timedelta

from boto.ec2.image import Image
from boto.ec2.instance import Instance
from boto.ec2.keypair import KeyPair

from .clcinterface import ClcInterface

# This class provides an implmentation of the clcinterface that caches responses
# from the underlying clcinterface. It will only make requests to the underlying layer
# at the rate defined by pollfreq. It is assumed this will be created per-session and
# therefore will only contain data for a single user. If a more global cache is desired,
# some things will need to be re-written.
class CachingClcInterface(ClcInterface):
    clc = None

    zones = None
    zoneUpdate = datetime.min
    zoneFreq = 0

    images = None
    imageUpdate = datetime.min
    imageFreq = 0

    instances = None
    instanceUpdate = datetime.min
    instanceFreq = 0

    addresses = None
    addressUpdate = datetime.min
    addressFreq = 0

    keypairs = None
    keypairUpdate = datetime.min
    keypairFreq = 0

    groups = None
    groupUpdate = datetime.min
    groupFreq = 0

    volumes = None
    volumeUpdate = datetime.min
    volumeFreq = 0

    snapshots = None
    snapshotUpdate = datetime.min
    snapshotFreq = 0

    # load saved state to simulate CLC
    def __init__(self, clcinterface, config):
        self.clc = clcinterface
        pollfreq = config.getint('server', 'pollfreq')
        try:
            self.zoneFreq = config.getint('server', 'pollfreq.zones')
        except ConfigParser.NoOptionError:
            self.zoneFreq = pollfreq
        try:
            self.imageFreq = config.getint('server', 'pollfreq.images')
        except ConfigParser.NoOptionError:
            self.imageFreq = pollfreq
        try:
            self.instanceFreq = config.getint('server', 'pollfreq.instances')
        except ConfigParser.NoOptionError:
            self.instanceFreq = pollfreq
        try:
            self.keypairFreq = config.getint('server', 'pollfreq.keypairs')
        except ConfigParser.NoOptionError:
            self.keypairFreq = pollfreq
        try:
            self.groupFreq = config.getint('server', 'pollfreq.groups')
        except ConfigParser.NoOptionError:
            self.groupFreq = pollfreq
        try:
            self.addressFreq = config.getint('server', 'pollfreq.addresses')
        except ConfigParser.NoOptionError:
            self.addressFreq = pollfreq
        try:
            self.volumeFreq = config.getint('server', 'pollfreq.volumes')
        except ConfigParser.NoOptionError:
            self.volumeFreq = pollfreq
        try:
            self.snapshotFreq = config.getint('server', 'pollfreq.snapshots')
        except ConfigParser.NoOptionError:
            self.snapshotFreq = pollfreq

    def get_all_zones(self):
        # if cache stale, update it
        if (datetime.now() - self.zoneUpdate) > timedelta(seconds = self.zoneFreq):
            self.zones = self.clc.get_all_zones()
            self.zoneUpdate = datetime.now()
        return self.zones

    def get_all_images(self, owners):
        if (datetime.now() - self.imageUpdate) > timedelta(seconds = self.imageFreq):
            self.images = self.clc.get_all_images(owners)
            self.imageUpdate = datetime.now()
        return self.images

    # returns list of image attributes
    def get_image_attribute(self, image_id, attribute):
        return self.clc.get_image_attribute(image_id, attribute)

    # returns True if successful
    def modify_image_attribute(self, image_id, attribute, operation, users, groups):
        self.imageUpdate = datetime.min   # invalidate cache
        return self.clc.modify_image_attribute(image_id, attribute, operation, users, groups)

    # returns True if successful
    def reset_image_attribute(self, image_id, attribute):
        self.imageUpdate = datetime.min   # invalidate cache
        return self.clc.reset_image_attribute(image_id, attribute)

    def get_all_instances(self):
        if (datetime.now() - self.instanceUpdate) > timedelta(seconds = self.instanceFreq):
            self.instances = self.clc.get_all_instances()
            self.instanceUpdate = datetime.now()
        return self.instances

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
        self.instanceUpdate = datetime.min   # invalidate cache
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
        self.instanceUpdate = datetime.min   # invalidate cache
        return self.clc.terminate_instances(instance_ids)

    # returns instance list
    def stop_instances(self, instance_ids, force=False):
        self.instanceUpdate = datetime.min   # invalidate cache
        return self.clc.stop_instances(instance_ids, force)

    # returns instance list
    def start_instances(self, instance_ids):
        self.instanceUpdate = datetime.min   # invalidate cache
        return self.clc.start_instances(instance_ids)

    # returns instance status
    def reboot_instances(self, instance_ids):
        self.instanceUpdate = datetime.min   # invalidate cache
        return self.clc.reboot_instances(instance_ids)

    # returns console output
    def get_console_output(self, instance_id):
        return self.clc.get_console_output(instance_id)

    # returns password data
    def get_password_data(self, instance_id):
        return self.clc.get_password_data(instance_id)

    def get_all_addresses(self):
        if (datetime.now() - self.addressUpdate) > timedelta(seconds = self.addressFreq):
            self.addresses = self.clc.get_all_addresses()
            self.addressUpdate = datetime.now()
        return self.addresses

    # returns address info
    def allocate_address(self):
        self.addressUpdate = datetime.min   # invalidate cache
        return self.clc.allocate_address()

    # returns True if successful
    def release_address(self, publicip):
        self.addressUpdate = datetime.min   # invalidate cache
        return self.clc.release_address(publicip)

    # returns True if successful
    def associate_address(self, publicip, instanceid):
        self.addressUpdate = datetime.min   # invalidate cache
        return self.clc.associate_address(publicip, instanceid)

    # returns True if successful
    def disassociate_address(self, publicip):
        self.addressUpdate = datetime.min   # invalidate cache
        return self.clc.disassociate_address(publicip)

    def get_all_key_pairs(self):
        if (datetime.now() - self.keypairUpdate) > timedelta(seconds = self.keypairFreq):
            self.keypairs = self.clc.get_all_key_pairs()
            self.keypairUpdate = datetime.now()
        return self.keypairs

    # returns keypair info and key
    def create_key_pair(self, key_name):
        self.keypairUpdate = datetime.min   # invalidate cache
        return self.clc.create_key_pair(key_name)

    # returns nothing
    def delete_key_pair(self, key_name):
        self.keypairUpdate = datetime.min   # invalidate cache
        return self.clc.delete_key_pair(key_name)

    # returns keypair info and key
    def import_key_pair(self, key_name, public_key_material):
        self.keypairUpdate = datetime.min   # invalidate cache
        return self.clc.import_key_pair(key_name, public_key_material)

    def get_all_security_groups(self):
        if (datetime.now() - self.groupUpdate) > timedelta(seconds = self.groupFreq):
            self.groups = self.clc.get_all_security_groups()
            self.groupUpdate = datetime.now()
        return self.groups

    # returns True if successful
    def create_security_group(self, name, description):
        self.groupUpdate = datetime.min   # invalidate cache
        return self.clc.create_security_group(name, description)

    # returns True if successful
    def delete_security_group(self, name=None, group_id=None):
        self.groupUpdate = datetime.min   # invalidate cache
        return self.clc.delete_security_group(name, group_id)

    # returns True if successful
    def authorize_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        self.groupUpdate = datetime.min   # invalidate cache
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
        self.groupUpdate = datetime.min   # invalidate cache
        return self.clc.revoke_security_group(name,
                                 src_security_group_name,
                                 src_security_group_owner_id,
                                 ip_protocol, from_port, to_port,
                                 cidr_ip, group_id,
                                 src_security_group_group_id)

    def get_all_volumes(self):
        if (datetime.now() - self.volumeUpdate) > timedelta(seconds = self.volumeFreq):
            self.volumes = self.clc.get_all_volumes()
            self.volumeUpdate = datetime.now()
        return self.volumes

    # returns volume info
    def create_volume(self, size, availability_zone, snapshot_id):
        self.volumeUpdate = datetime.min   # invalidate cache
        return self.clc.create_volume(size, availability_zone, snapshot_id)

    # returns True if successful
    def delete_volume(self, volume_id):
        self.volumeUpdate = datetime.min   # invalidate cache
        return self.clc.delete_volume(volume_id)

    # returns True if successful
    def attach_volume(self, volume_id, instance_id, device):
        self.volumeUpdate = datetime.min   # invalidate cache
        return self.clc.attach_volume(volume_id, instance_id, device)

    # returns True if successful
    def detach_volume(self, volume_id, force=False):
        self.volumeUpdate = datetime.min   # invalidate cache
        return self.clc.detach_volume(volume_id, force)

    def get_all_snapshots(self):
        if (datetime.now() - self.snapshotUpdate) > timedelta(seconds = self.snapshotFreq):
            self.snapshots = self.clc.get_all_snapshots()
            self.snapshotUpdate = datetime.now()
        return self.snapshots

    # returns snapshot info
    def create_snapshot(self, volume_id, description):
        self.snapshotUpdate = datetime.min   # invalidate cache
        return self.clc.create_snapshot(volume_id, description)

    # returns True if successful
    def delete_snapshot(self, snapshot_id):
        self.snapshotUpdate = datetime.min   # invalidate cache
        return self.clc.delete_snapshot(snapshot_id)

    # returns list of snapshots attributes
    def get_snapshot_attribute(self, snapshot_id, attribute):
        self.snapshotUpdate = datetime.min   # invalidate cache
        return self.clc.get_snapshot_attribute(snapshot_id, attribute)

    # returns True if successful
    def modify_snapshot_attribute(self, snapshot_id, attribute, operation, users, groups):
        self.snapshotUpdate = datetime.min   # invalidate cache
        return self.clc.modify_snapshot_attribute(snapshot_id, attribute, operation, users, groups)

    # returns True if successful
    def reset_snapshot_attribute(self, snapshot_id, attribute):
        self.snapshotUpdate = datetime.min   # invalidate cache
        return self.clc.reset_snapshot_attribute(snapshot_id, attribute)

    # returns True if successful
    def register_image(self, name, image_location=None, description=None, architecture=None, kernel_id=None, ramdisk_id=None, root_dev_name=None, block_device_map=None):
        self.imageUpdate = datetime.min   # invalidate cache
        return self.clc.register_image(name, image_location, description, architecture, kernel_id, ramdisk_id, root_dev_name, block_device_map)
