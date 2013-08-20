# Copyright 2012 Eucalyptus Systems, Inc.
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the following
# conditions are met:
#
#   Redistributions of source code must retain the above copyright notice,
#   this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer in the
#   documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

#
# This class defines an interface that must be extended for either talking
# to the CLC itself or for a testing mock
#
# NOTE: all methods are expected to return boto value objects.
#
class ClcInterface(object):

    def get_all_regions(self, filters, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def get_all_zones(self, filters, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    ##
    # Image methods
    ##
    def get_all_images(self, owners, filters, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns list of image attributes
    def get_image_attribute(self, image_id, attribute, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def modify_image_attribute(self, image_id, attribute, operation, users, groups, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def reset_image_attribute(self, image_id, attribute, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    ##
    # Instance methods
    ##
    def get_all_instances(self, filters, callback=None):
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
                      instance_profile_arn=None, tenancy=None, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns instance list
    def terminate_instances(self, instance_ids, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns instance list
    def stop_instances(self, instance_ids, force=False, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns instance list
    def start_instances(self, instance_ids, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns instance status
    def reboot_instances(self, instance_ids, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns console output
    def get_console_output(self, instance_id, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns password data
    def get_password_data(self, instance_id, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    ##
    # Elastic IP methods
    ##
    def get_all_addresses(self, filters, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns address info
    def allocate_address(self, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def release_address(self, publicip, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def associate_address(self, publicip, instanceid, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def disassociate_address(self, publicip, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    ##
    # Keypair methods
    ##
    # return list of keypairs
    def get_all_key_pairs(self, filters, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns keypair info and key
    def create_key_pair(self, key_name, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns nothing
    def delete_key_pair(self, key_name, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns keypair info and key
    def import_key_pair(self, key_name, public_key_material, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    ##
    # Security Group methods
    ##
    def get_all_security_groups(self, filters, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def create_security_group(self, name, description, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def delete_security_group(self, name=None, group_id=None, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def authorize_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def revoke_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    ##
    # Volume methods
    ##
    def get_all_volumes(self, filters, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns volume info
    def create_volume(self, volume_id, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def delete_volume(self, size, availability_zone, snapshot_id, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def attach_volume(self, volume_id, instance_id, device, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def detach_volume(self, volume_id, force=False, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    ##
    # Snapshot methods
    ##
    def get_all_snapshots(self, filters, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns snapshot info
    def create_snapshot(self, volume_id, description, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def delete_snapshot(self, snapshot_id, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns list of snapshots attributes
    def get_snapshot_attribute(self, snapshot_id, attribute, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def modify_snapshot_attribute(self, snapshot_id, attribute, operation, users, groups, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def reset_snapshot_attribute(self, snapshot_id, attribute, callback):
        raise NotImplementedError("Are you sure you're using the right class?")
    
    def register_image(self, name, image_location=None, description=None, architecture=None, kernel_id=None, ramdisk_id=None, root_dev_name=None, block_device_map=None, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    ##
    # Tag methods
    ##
    def get_all_tags(self, filters, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns tag info
    def create_tags(self, resourceIds, tags, callback):
        raise NotImplementedError("Are you sure you're using the right class?")

    # returns True if successful
    def delete_tags(self, resourceIds, tags, callback):
        raise NotImplementedError("Are you sure you're using the right class?")
