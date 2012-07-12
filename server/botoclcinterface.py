
import boto
import json
from clcinterface import ClcInterface
from botojsonencoder import BotoJsonEncoder

# This class provides an implmentation of the clcinterface using boto
class BotoClcInterface(ClcInterface):
  conn = None
  saveclcdata = False

  def __init__(self, clc_host, access_id, secret_key):
    self.conn = boto.connect_euca(host=clc_host,
                aws_access_key_id=access_id,
                aws_secret_access_key=secret_key)

  def __save_json__(self, obj, name):
    f = open(name, 'w')
    json.dump(obj, f, cls=BotoJsonEncoder, indent=2)
    f.close()

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

  def get_all_security_groups(self):
    obj = self.conn.get_all_security_groups()
    if self.saveclcdata:
      self.__save_json__(obj, "mockdata/Groups.json")
    return obj

  def get_all_volumes(self):
    obj = self.conn.get_all_volumes()
    if self.saveclcdata:
      self.__save_json__(obj, "mockdata/Volumes.json")
    return obj

  def get_all_snapshots(self):
    obj = self.conn.get_all_snapshots()
    if self.saveclcdata:
      self.__save_json__(obj, "mockdata/Snapshots.json")
    return obj

