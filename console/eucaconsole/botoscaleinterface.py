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

import boto
import ConfigParser
import json
from boto.ec2.autoscale import AutoScaleConnection
from boto.ec2.regioninfo import RegionInfo

import eucaconsole
from .botojsonencoder import BotoJsonScaleEncoder
from .scaleinterface import ScaleInterface

# This class provides an implmentation of the clcinterface using boto
class BotoScaleInterface(ScaleInterface):
    conn = None
    saveclcdata = False

    def __init__(self, clc_host, access_id, secret_key, token):
        #boto.set_stream_logger('foo')
        path='/services/AutoScaling'
        reg = RegionInfo(name='eucalyptus', endpoint=clc_host)
        port=8773
        if clc_host[len(clc_host)-13:] == 'amazonaws.com':
            clc_host = clc_host.replace('ec2', 'autoscaling', 1)
            path = '/'
            reg = None
            port=443
        self.conn = AutoScaleConnection(access_id, secret_key, region=reg,
                                  port=port, path=path,
                                  is_secure=True, security_token=token, debug=0)
        self.conn.APIVersion = '2011-01-01'
        if not(clc_host[len(clc_host)-13:] == 'amazonaws.com'):
            self.conn.auth_region_name = 'Eucalyptus'
        self.conn.https_validate_certificates = False
        self.conn.http_connection_kwargs['timeout'] = 30

    def __save_json__(self, obj, name):
        f = open(name, 'w')
        json.dump(obj, f, cls=BotoJsonScaleEncoder, indent=2)
        f.close()

    ##
    # autoscaling methods
    ##
    def create_auto_scaling_group(self, as_group):
        return self.conn.create_auto_scaling_group(as_group)

    def delete_auto_scaling_group(self, name, force_delete=False):
        return self.conn.delete_auto_scaling_group(name, force_delete)

    def get_all_groups(self, names=None, max_records=None, next_token=None):
        obj = self.conn.get_all_groups(names, max_records, next_token)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/AS_Groups.json")
        return obj

    def get_all_autoscaling_instances(self, instance_ids=None, max_records=None, next_token=None):
        obj = self.conn.get_all_autoscaling_instances(instance_ids, max_records, next_token)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/AS_Instances.json")
        return obj

    def set_desired_capacity(self, group_name, desired_capacity, honor_cooldown=False):
        group = self.conn.get_all_groups([group_name])[0];
        # notice, honor_cooldown not supported.
        return group.set_capacity(desired_capacity)

    def set_instance_health(self, instance_id, health_status, should_respect_grace_period=True):
        return self.conn.set_instance_health(instance_id, health_status,
                                             should_respect_grace_period)

    def terminate_instance(self, instance_id, decrement_capacity=True):
        return self.conn.terminate_instance(instance_id, decrement_capacity)

    def update_autoscaling_group(self, as_group):
        as_group.connection = self.conn
        return as_group.update()

    def create_launch_configuration(self, launch_config):
        return self.conn.create_launch_configuration(launch_config)

    def delete_launch_configuration(self, launch_config_name):
        return self.conn.delete_launch_configuration(launch_config_name)

    def get_all_launch_configurations(self, config_names=None, max_records=None, next_token=None):
        obj = self.conn.get_all_launch_configurations(names=config_names, max_records=max_records, next_token=next_token)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/AS_LaunchConfigs.json")
        return obj

    # policy related
    def delete_policy(self, policy_name, autoscale_group=None):
        return self.conn.delete_policy(policy_name, autoscale_group)

    def get_all_policies(self, as_group=None, policy_names=None, max_records=None, next_token=None):
        return self.conn.get_all_policies(as_group, policy_names, max_records, next_token)

    def execute_policy(self, policy_name, as_group=None, honor_cooldown=None):
        return self.conn.execute_policy(policy_name, as_group, honor_cooldown)

    def create_scaling_policy(self, scaling_policy):
        return self.conn.create_scaling_policy(scaling_policy)

    def get_all_adjustment_types(self):
        return self.conn.get_all_adjustment_types()

    # tag related
    def delete_tags(self, tags):
        return self.conn.delete_tags(tags)

    def get_all_tags(self, filters=None, max_records=None, next_token=None):
        return self.conn.get_all_tags(filters, max_records, next_token)

    def create_or_update_tags(self, tags):
        return self.conn.create_or_update_tags(tags)

# stuff to be added later
#  describe_metric_collection_types
#  describe_scaling_activities
#  describe_scaling_process_types
#  describe_metrics_collection
#  enable_metrics_collection
#  resume_processes
#  suspend_processes
