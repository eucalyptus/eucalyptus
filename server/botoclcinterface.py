
import boto
import json
from clcinterface import ClcInterface

from json import JSONEncoder
from boto.ec2 import EC2Connection
from boto.ec2.ec2object import EC2Object
from boto.regioninfo import RegionInfo
from boto.ec2.instance import Group
from boto.ec2.volume import AttachmentSet

class BotoEncoder(JSONEncoder):
  def default(self, obj):
    if isinstance(obj, RegionInfo):
      return (obj.__dict__)
    if isinstance(obj, EC2Connection):
      return []
    elif issubclass(obj.__class__, EC2Object):
      return (obj.__dict__)
    elif issubclass(obj.__class__, Group):
      return (obj.__dict__)
    elif issubclass(obj.__class__, AttachmentSet):
      return (obj.__dict__)
    return super(BotoEncoder, self).default(obj)

# This class provides an implmentation of the clcinterface using boto
class BotoClcInterface(ClcInterface):
  conn = None

  def __init__(self, clc_host, access_id, secret_key):
    self.conn = boto.connect_euca(host=clc_host,
                aws_access_key_id=access_id,
                aws_secret_access_key=secret_key)

  def __to_json__(self, obj):
    return json.dumps(obj, cls=BotoEncoder, indent=2)

  def get_all_images(self):
    return self.__to_json__(self.conn.get_all_images())

  def get_all_instances(self):
    return self.__to_json__(self.conn.get_all_instances())

  def get_all_addresses(self):
    return self.__to_json__(self.conn.get_all_addresses())

  def get_all_key_pairs(self):
    return self.__to_json__(self.conn.get_all_key_pairs())

  def get_all_security_groups(self):
    return self.__to_json__(self.conn.get_all_security_groups())

  def get_all_volumes(self):
    return self.__to_json__(self.conn.get_all_volumes())

  def get_all_snapshots(self):
    return self.__to_json__(self.conn.get_all_snapshots())

