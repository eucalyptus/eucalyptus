
#
# This class defines an interface that must be extended for either talking
# to the CLC itself or for a testing mock
#
# NOTE: all methods are expected to return boto value objects.
#
class ClcInterface(object):

  def get_all_zones(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  ##
  # Image methods
  ##
  def get_all_images(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  ##
  # Instance methods
  ##
  def get_all_instances(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  ##
  # Elastic IP methods
  ##
  def get_all_addresses(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  ##
  # Keypair methods
  ##
  # return list of keypairs
  def get_all_key_pairs(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  # returns keypair info and key
  def create_key_pair(self, key_name):
    raise NotImplementedError("Are you sure you're using the right class?")

  # returns nothing
  def delete_key_pair(self, key_name):
    raise NotImplementedError("Are you sure you're using the right class?")

  ##
  # Security Group methods
  ##
  def get_all_security_groups(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  ##
  # Volume methods
  ##
  def get_all_volumes(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  # returns volume info
  def create_volume(self, volume_id):
    raise NotImplementedError("Are you sure you're using the right class?")

  # returns True if successful
  def delete_volume(self, size, availability_zone, snapshot_id):
    raise NotImplementedError("Are you sure you're using the right class?")

  # returns True if successful
  def attach_volume(self, volume_id, instance_id, device):
    raise NotImplementedError("Are you sure you're using the right class?")

  # returns True if successful
  def detach_volume(self, volume_id, instance_id, device, force=False):
    raise NotImplementedError("Are you sure you're using the right class?")

  ##
  # Snapshot methods
  ##
  def get_all_snapshots(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  # returns snapshot info
  def create_snapshot(self, volume_id, description):
    raise NotImplementedError("Are you sure you're using the right class?")

  # returns True if successful
  def delete_snapshot(self, snapshot_id):
    raise NotImplementedError("Are you sure you're using the right class?")

  # returns list of snapshots attributes
  def get_snapshot_attribute(self, snapshot_id, attribute):
    raise NotImplementedError("Are you sure you're using the right class?")

  # returns True if successful
  def modify_snapshot_attribute(self, snapshot_id, attribute, operation, users, groups):
    raise NotImplementedError("Are you sure you're using the right class?")

  # returns True if successful
  def reset_snapshot_attribute(self, snapshot_id, attribute):
    raise NotImplementedError("Are you sure you're using the right class?")
