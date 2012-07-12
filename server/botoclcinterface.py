
import boto
from clcinterface import ClcInterface

# This class provides an implmentation of the clcinterface using boto
class BotoClcInterface(ClcInterface):
  conn = None

  def __init__(self, clc_host, access_id, secret_key):
    self.conn = boto.connect_euca(host=clc_host,
                aws_access_key_id=access_id,
                aws_secret_access_key=secret_key)

  def get_all_images(self):
    return self.conn.get_all_images()

  def get_all_instances(self):
    return self.conn.get_all_instances()

  def get_all_addresses(self):
    return self.conn.get_all_addresses()

  def get_all_key_pairs(self):
    return self.conn.get_all_key_pairs()

  def get_all_security_groups(self):
    return self.conn.get_all_security_groups()

  def get_all_volumes(self):
    return self.conn.get_all_volumes()

  def get_all_snapshots(self):
    return self.conn.get_all_snapshots()

