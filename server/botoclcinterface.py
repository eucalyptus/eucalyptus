import boto
import json

from .botojsonencoder import BotoJsonEncoder
from .clcinterface import ClcInterface


# This class provides an implmentation of the clcinterface using boto
class BotoClcInterface(ClcInterface):
    conn = None
    saveclcdata = False

    def __init__(self, clc_host, access_id, secret_key):
        #boto.set_stream_logger('foo')
        self.conn = boto.connect_euca(host=clc_host,
                                aws_access_key_id=access_id,
                                aws_secret_access_key=secret_key, debug=0)
        self.conn.APIVersion = '2012-03-01'

    def __save_json__(self, obj, name):
        f = open(name, 'w')
        json.dump(obj, f, cls=BotoJsonEncoder, indent=2)
        f.close()

    def get_all_zones(self):
        obj = self.conn.get_all_zones()
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Zones.json")
        return obj

    def get_all_images(self):
        obj = self.conn.get_all_images()
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Images.json")
        return obj

    def get_all_instances(self):
        obj = self.conn.get_all_instances()
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Instances.json")
        return obj

    def get_all_addresses(self):
        obj = self.conn.get_all_addresses()
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Addresses.json")
        return obj

    def get_all_key_pairs(self):
        obj = self.conn.get_all_key_pairs()
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Keypairs.json")
        return obj

    # returns keypair info and key
    def create_key_pair(self, key_name):
        return self.conn.create_key_pair(key_name)

    # returns nothing
    def delete_key_pair(self, key_name):
        return self.conn.delete_key_pair(key_name)

    def get_all_security_groups(self):
        obj = self.conn.get_all_security_groups()
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/Groups.json")
        return obj

    # returns True if successful
    def create_security_group(self, name, description):
        return self.conn.create_security_group(name, description)

    # returns True if successful
    def delete_security_group(self, name=None, group_id=None):
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

    def get_all_volumes(self):
        obj = self.conn.get_all_volumes()
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
    def detach_volume(self, volume_id, instance_id, device, force=False):
        return self.conn.detach_volume(volume_id, instance_id, device, force)

    def get_all_snapshots(self):
        obj = self.conn.get_all_snapshots()
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
