import boto
import json

from boto.ec2.image import Image
from boto.ec2.instance import Instance
from boto.ec2.keypair import KeyPair

from .botojsonencoder import BotoJsonDecoder
from .clcinterface import ClcInterface

# This class provides an implmentation of the clcinterface using canned json
# strings. Might be better to represent as object graph so we can modify
# values in the mock.
class MockClcInterface(ClcInterface):
    zones = None
    images = None
    instances = None
    addresses = None
    keypairs = None
    groups = None
    volumes = None
    snapshots = None

    # load saved state to simulate CLC
    def __init__(self):
        with open('mockdata/Zones.json') as f:
            self.zones = json.load(f, cls=BotoJsonDecoder)
        with open('mockdata/Images.json') as f:
            self.images = json.load(f, cls=BotoJsonDecoder)
        with open('mockdata/Instances.json') as f:
            self.instances = json.load(f, cls=BotoJsonDecoder)
        with open('mockdata/Addresses.json') as f:
            self.addresses = json.load(f, cls=BotoJsonDecoder)
        with open('mockdata/Keypairs.json') as f:
            self.keypairs = json.load(f, cls=BotoJsonDecoder)
        with open('mockdata/Groups.json') as f:
            self.groups = json.load(f, cls=BotoJsonDecoder)
        with open('mockdata/Volumes.json') as f:
            self.volumes = json.load(f, cls=BotoJsonDecoder)
        with open('mockdata/Snapshots.json') as f:
            self.snapshots = json.load(f, cls=BotoJsonDecoder)

    def get_all_zones(self):
        return self.zones

    def get_all_images(self):
        return self.images

    def get_all_instances(self):
        return self.instances

    def get_all_addresses(self):
        return self.addresses

    def get_all_key_pairs(self):
        return self.keypairs

    # returns keypair info and key
    def create_key_pair(self, key_name):
        return None #self.keypairs.append(KeyPair(key_name))

    # returns nothing
    def delete_key_pair(self, key_name):
        self.keypairs.remove(key_name)
        return None

    def get_all_security_groups(self):
        return self.groups

    # returns True if successful
    def create_security_group(self, name, description):
        return False

    # returns True if successful
    def delete_security_group(self, name=None, group_id=None):
        return False

    # returns True if successful
    def authorize_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        return False

    # returns True if successful
    def revoke_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        return False

    def get_all_volumes(self):
        return self.volumes

    # returns volume info
    def create_volume(self, size, availability_zone, snapshot_id):
        pass

    # returns True if successful
    def delete_volume(self, volume_id):
        pass

    # returns True if successful
    def attach_volume(self, volume_id, instance_id, device):
        pass

    # returns True if successful
    def detach_volume(self, volume_id, instance_id, device, force=False):
        pass

    def get_all_snapshots(self):
        return self.snapshots

    # returns snapshot info
    def create_snapshot(self, volume_id, description):
        pass

    # returns True if successful
    def delete_snapshot(self, snapshot_id):
        pass

    # returns list of snapshots attributes
    def get_snapshot_attribute(self, snapshot_id, attribute):
        pass

    # returns True if successful
    def modify_snapshot_attribute(self, snapshot_id, attribute, operation, users, groups):
        pass

    # returns True if successful
    def reset_snapshot_attribute(self, snapshot_id, attribute):
        pass
