
#
# This class defines an interface that must be extended for either talking
# to the CLC itself or for a testing mock
#
# NOTE: all methods are expected to return JSON.
#
class ClcInterface(object):

  def get_all_images(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  def get_all_instances(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  def get_all_addresses(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  def get_all_key_pairs(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  def get_all_security_groups(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  def get_all_volumes(self):
    raise NotImplementedError("Are you sure you're using the right class?")

  def get_all_snapshots(self):
    raise NotImplementedError("Are you sure you're using the right class?")

