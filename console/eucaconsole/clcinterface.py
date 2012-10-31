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
    def get_all_images(self, owners):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns list of image attributes
    def get_image_attribute(self, image_id, attribute):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def modify_image_attribute(self, image_id, attribute, operation, users, groups):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def reset_image_attribute(self, image_id, attribute):
        raise NotImplementedError("Are you sure you're using the right class?")

    ##
    # Instance methods
    ##
    def get_all_instances(self):
        raise NotImplementedError("Are you sure you're using the right class?")

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
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns instance list
    def terminate_instances(self, instance_ids):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns instance list
    def stop_instances(self, instance_ids, force=False):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns instance list
    def start_instances(self, instance_ids):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns instance status
    def reboot_instances(self, instance_ids):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns console output
    def get_console_output(self, instance_id):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns password data
    def get_password_data(self, instance_id):
        raise NotImplementedError("Are you sure you're using the right class?")

    ##
    # Elastic IP methods
    ##
    def get_all_addresses(self):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns address info
    def allocate_address(self):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def release_address(self, publicip):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def associate_address(self, publicip, instanceid):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def disassociate_address(self, publicip):
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

    # returns keypair info and key
    def import_key_pair(self, key_name, public_key_material):
        raise NotImplementedError("Are you sure you're using the right class?")

    ##
    # Security Group methods
    ##
    def get_all_security_groups(self):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def create_security_group(self, name, description):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def delete_security_group(self, name=None, group_id=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def authorize_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def revoke_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
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
    def detach_volume(self, volume_id, force=False):
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
    
    def register_image(self, name, image_location=None, description=None, architecture=None, kernel_id=None, ramdisk_id=None, root_dev_name=None, block_device_map=None):
        raise NotImplementedError("Are you sure you're using the right class?")
