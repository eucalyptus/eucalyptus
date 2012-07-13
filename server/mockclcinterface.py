
import boto
import json

from boto.ec2.keypair import KeyPair
from boto.ec2.image import Image
from boto.ec2.instance import Instance

from clcinterface import ClcInterface
from botojsonencoder import BotoJsonDecoder

# This class provides an implmentation of the clcinterface using canned json
# strings. Might be better to represent as object graph so we can modify
# values in the mock.
class MockClcInterface(ClcInterface):
  images = None
  instances = None
  addresses = None
  keypairs = None
  groups = None
  volumes = None
  snapshots = None

  # load saved state to simulate CLC
  def __init__(self):
    f = open('mockdata/Images.json', 'r')
    self.images = json.load(f, cls=BotoJsonDecoder)
    f = open('mockdata/Instances.json', 'r')
    self.instances = json.load(f, cls=BotoJsonDecoder)
    f = open('mockdata/Addresses.json', 'r')
    self.addresses = json.load(f, cls=BotoJsonDecoder)
    f = open('mockdata/Keypairs.json', 'r')
    self.keypairs = json.load(f, cls=BotoJsonDecoder)
    f = open('mockdata/Groups.json', 'r')
    self.groups = json.load(f, cls=BotoJsonDecoder)
    f = open('mockdata/Volumes.json', 'r')
    self.volumes = json.load(f, cls=BotoJsonDecoder)
    f = open('mockdata/Snapshots.json', 'r')
    self.snapshots = json.load(f, cls=BotoJsonDecoder)

  def get_all_images(self):
    return self.images

  def get_all_instances(self):
    return self.instances

  def get_all_addresses(self):
    return self.addresses

  def get_all_key_pairs(self):
    return self.keypairs

  def create_key_pair(self, key_name):
    return None #self.keypairs.append(KeyPair(key_name))

  def delete_key_pair(self, key_name):
    self.keypairs.remove(key_name)
    return None

  def get_all_security_groups(self):
    return self.groups

  def get_all_volumes(self):
    return self.volumes

  def get_all_snapshots(self):
    return self.snapshots

