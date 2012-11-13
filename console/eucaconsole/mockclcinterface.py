import boto
import copy
import json
import os
import datetime

from operator import itemgetter
from boto.ec2.image import Image
from boto.ec2.instance import Instance
from boto.ec2.keypair import KeyPair

from .botojsonencoder import BotoJsonDecoder
from .clcinterface import ClcInterface
from .configloader import ConfigLoader

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
    consoleoutput = None

    # load saved state to simulate CLC
    def __init__(self):
        self.config = ConfigLoader().getParser()
        if self.config.has_option('server', 'mockpath'):
            self.mockpath = self.config.get('server', 'mockpath')
        else:
            self.mockpath = 'mockdata'

        with open(os.path.join(self.mockpath, 'Zones.json')) as f:
            self.zones = json.load(f, cls=BotoJsonDecoder)
        with open(os.path.join(self.mockpath, 'Images.json')) as f:
            self.images = json.load(f, cls=BotoJsonDecoder)
        with open(os.path.join(self.mockpath, 'Instances.json')) as f:
            self.instances = json.load(f, cls=BotoJsonDecoder)
        with open(os.path.join(self.mockpath, 'Addresses.json')) as f:
            self.addresses = json.load(f, cls=BotoJsonDecoder)
        with open(os.path.join(self.mockpath, 'Keypairs.json')) as f:
            self.keypairs = json.load(f, cls=BotoJsonDecoder)
        with open(os.path.join(self.mockpath, 'Groups.json')) as f:
            self.groups = json.load(f, cls=BotoJsonDecoder)
        with open(os.path.join(self.mockpath, 'Volumes.json')) as f:
            self.volumes = json.load(f, cls=BotoJsonDecoder)
        with open(os.path.join(self.mockpath, 'Snapshots.json')) as f:
            self.snapshots = json.load(f, cls=BotoJsonDecoder)
        with open(os.path.join(self.mockpath, 'ConsoleOutput.json')) as f:
            self.consoleoutput = json.load(f, cls=BotoJsonDecoder)

    def get_all_zones(self, callback=None):
        return self.zones

    def get_all_images(self, owners, callback=None):
        return self.images

    # returns list of snapshots attributes
    def get_image_attribute(self, image_id, attribute):
        return None

    # returns True if successful
    def modify_image_attribute(self, image_id, attribute, operation, users, groups):
        return None

    # returns True if successful
    def reset_image_attribute(self, image_id, attribute):
        return None

    def get_all_instances(self, callback=None):
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
        return None

    # returns instance list
    def terminate_instances(self, instance_ids):
        # find instance in local store, then change state
        found = False
        for id in instance_ids:
            for res in self.instances:
                for inst in res['instances']:
                    if inst['id'] == id:
                        inst['state'] = 'terminated'
                        found = True
                        break
        return found

    # returns instance list
    def stop_instances(self, instance_ids, force=False):
        # find instance in local store, then change state
        found = False
        for id in instance_ids:
            for res in self.instances:
                for inst in res['instances']:
                    if inst['id'] == id:
                        inst['state'] = 'stopped'
                        found = True
                        break
        return found

    # returns instance list
    def start_instances(self, instance_ids):
        # find instance in local store, then change state
        found = False
        for id in instance_ids:
            for res in self.instances:
                for inst in res['instances']:
                    if inst['id'] == id:
                        inst['state'] = 'running'
                        found = True
                        break
        return found

    # returns instance status
    def reboot_instances(self, instance_ids):
        # find instance in local store, then change state
        found = False
        for id in instance_ids:
            for res in self.instances:
                for inst in res['instances']:
                    if inst['id'] == id:
                        inst['state'] = 'derp'
                        found = True
                        break
        return found

    # returns console output
    def get_console_output(self, instance_id):
        return self.consoleoutput

    # returns password data
    def get_password_data(self, instance_id):
        return None

    def get_all_addresses(self, callback=None):
        return self.addresses

    # returns address info
    def allocate_address(self):
        return None

    # returns True if successful
    def release_address(self, publicip):
        return False

    # returns True if successful
    def associate_address(self, publicip, instanceid):
        return False

    # returns True if successful
    def disassociate_address(self, publicip):
        return False

    def get_all_key_pairs(self, callback=None):
        return self.keypairs

    # returns keypair info and key
    def create_key_pair(self, key_name):
        newkey = {
                'name': key_name,
                'fingerprint': 'd0:0d:01:02:03:04:05:06:07:08:09:0a:0b:0c:0d:0e:0f:d0:0d',
                'material': 'sorry, no keymaterial, this came from the mock interface',
                '__obj_name__': 'KeyPair'
            }
        self.keypairs.append(newkey)
        return newkey

    # returns nothing
    def delete_key_pair(self, key_name):
        idx = map(itemgetter('name'), self.keypairs).index(key_name)
        self.keypairs.remove(self.keypairs[idx])
        return True

    # returns keypair info and key
    def import_key_pair(self, key_name, public_key_material):
        newkey = {
                'name': key_name,
                'fingerprint': 'd0:0d:01:02:03:04:05:06:07:08:09:0a:0b:0c:0d:0e:0f:d0:0d',
                'material': public_key_material,
                '__obj_name__': 'KeyPair'
            }
        self.keypairs.append(newkey)
        return newkey

    def get_all_security_groups(self, callback=None):
        return self.groups

    # returns True if successful
    def create_security_group(self, name, description):
        newgroup = {
                'name': 'default', 
                'description': 'default group',
                '__obj_name__': 'SecurityGroup',
                'tags': {}, 
                'rules': [], 
                'ipPermissions': '', 
                'connection': [], 
                'vpc_id': '', 
                'owner_id': '072279894205'
            }
        self.groups.append(newgroup)
        return True

    # returns True if successful
    def delete_security_group(self, name=None, group_id=None, callback=None):
        if name:
            idx = map(itemgetter('name'), self.groups).index(name)
        if group_id:
            raise NotImplementedError("Are you sure you're using the right class?")
        self.groups.remove(self.groups[idx])
        return True

    # returns True if successful
    def authorize_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        newrule = {
                'ip_protocol': ip_protocol, 
                'from_port': from_port, 
                'to_port': to_port, 
                'parent': '', 
                '__obj_name__': 'IPPermissions', 
                'grants': [
                  {
                    '__obj_name__': 'GroupOrCIDR', 
                    'name': '', 
                    'group_id': '', 
                    'cidr_ip': cidr_ip,
                    'owner_id': ''
                  }
                ], 
                'ipRanges': '', 
                'groups': ''
            }
        idx = map(itemgetter('name'), self.groups).index(name)
        self.groups[idx].rules.append(newrule)
        return True

    # returns True if successful
    def revoke_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        return False

    def get_all_volumes(self, callback=None):
        return self.volumes

    def __gen_id__(self, prefix):
        id = os.urandom(4).encode('hex')
        return prefix+'-'+id

    # returns volume info
    def create_volume(self, size, availability_zone, snapshot_id):
        numToCreate = 1;
        if (int(size) > 1000):
            numToCreate = int(size) - 1000;
            size = '1'

        newvol = {
                'id': self.__gen_id__('vol'),
                'size': size,
                'status': 'available',
                '__obj_name__': 'Volume',
                'zone': availability_zone,
                'tags': {},
                'attach_data': {
                  'status': '',
                  'instance_id': '',
                  '__obj_name__': 'AttachmentSet',
                  'attachmentSet': '',
                  'attach_time': '',
                  'device': '',
                  'id': ''
                },
                'create_time': datetime.datetime.now().strftime('%Y-%m-%dT%H:%M:%S.%fZ'),
                'snapshot_id': snapshot_id,
            }
        if (numToCreate > 1):
            for i in range(0, numToCreate-1):
                self.volumes.append(newvol)
                newvol = copy.copy(newvol)
                newvol['id'] = self.__gen_id__('vol');
                newvol['size'] = "%d"  % (i+2)
        self.volumes.append(newvol)
        return newvol

    # returns True if successful
    def delete_volume(self, volume_id):
        idx = map(itemgetter('id'), self.volumes).index(volume_id)
        self.volumes.remove(self.volumes[idx])
        return True

    # returns True if successful
    def attach_volume(self, volume_id, instance_id, device):
        return True

    # returns True if successful
    def detach_volume(self, volume_id, force=False):
        return True

    def get_all_snapshots(self, callback=None):
        return self.snapshots

    # returns snapshot info
    def create_snapshot(self, volume_id, description):
        idx = map(itemgetter('id'), self.volumes).index(volume_id)
        newsnap = {
                'status': 'completed', 
                '__obj_name__': 'Snapshot', 
                'description': description, 
                'tags': {}, 
                'start_time': datetime.datetime.now().strftime('%Y-%m-%dT%H:%M:%S.%fZ'),
                'id': self.__gen_id__('snap'),
                'volume_size': self.volumes[idx]['size'], 
                'volume_id': volume_id, 
                'progress': '100%', 
                'owner_id': '072279894205'
            }
        self.snapshots.append(newsnap)
        return newsnap

    # returns True if successful
    def delete_snapshot(self, snapshot_id):
        idx = map(itemgetter('id'), self.snapshots).index(snapshot_id)
        self.snapshots.remove(self.snapshots[idx])
        return True

    # returns list of snapshots attributes
    def get_snapshot_attribute(self, snapshot_id, attribute):
        pass

    # returns True if successful
    def modify_snapshot_attribute(self, snapshot_id, attribute, operation, users, groups):
        pass

    # returns True if successful
    def reset_snapshot_attribute(self, snapshot_id, attribute):
        pass
    
    def deregister_image(self, image_id):
        pass

    def register_image(self, name, image_location=None, description=None, architecture=None, kernel_id=None, ramdisk_id=None, root_dev_name=None, block_device_map=None):
        pass
