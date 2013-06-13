# Copyright 2013 Eucalyptus Systems, Inc.
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
class ScaleInterface(object):

    ##
    # autoscaling methods
    ##
    def create_auto_scaling_group(self, as_group, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def delete_auto_scaling_group(self, name, force_delete=False, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def get_all_groups(self, names=None, max_records=None, next_token=None, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def get_all_autoscaling_instances(self, instance_ids=None, max_records=None, next_token=None, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def set_desired_capacity(self, group_name, desired_capacity, honor_cooldown=False, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def set_instance_health(self, instance_id, health_status, should_respect_grace_period=True):
        raise NotImplementedError("Are you sure you're using the right class?")

    def terminate_instance(self, instance_id, decrement_capacity=True):
        raise NotImplementedError("Are you sure you're using the right class?")

    def update_autoscaling_group(self, as_group, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def create_launch_configuration(self, launch_config, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def delete_launch_configuration(self, launch_config_name, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def get_all_launch_configurations(self, configuration_names=None, max_records=None, next_token=None, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # policy related
    def delete_policy(self, policy_name, autoscale_group=None, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def get_all_policies(self, as_group=None, policy_names=None, max_records=None, next_token=None, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def execute_policy(self, policy_name, as_group=None, honor_cooldown=None, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def create_scaling_policy(self, scaling_policy, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def get_all_adjustment_types(self, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    # tag related
    def delete_tags(self, tags, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def get_all_tags(self, filters=None, max_records=None, next_token=None):
        raise NotImplementedError("Are you sure you're using the right class?")

    def create_or_update_tags(self, tags, callback=None):
        raise NotImplementedError("Are you sure you're using the right class?")

