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
        port=8773
        if clc_host[len(clc_host)-13:] == 'amazonaws.com':
            clc_host = clc_host.replace('ec2', 'autoscaling', 1)
            path = '/'
            reg = None
            port=443
        reg = RegionInfo(name='eucalyptus', endpoint=clc_host)
        self.conn = AutoScaleConnection(access_id, secret_key, region=reg,
                                  port=port, path=path, validate_certs=False,
                                  is_secure=True, security_token=token, debug=0)
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
        return []
        obj = self.conn.get_all_groups(names, max_records, next_token)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/AS_Groups.json")
        return obj

    def get_all_autoscaling_instances(self, instance_ids=None, max_records=None, next_token=None):
        return []
        obj = self.conn.get_all_autoscaling_instances(instance_ids, max_records, next_token)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/AS_Instances.json")
        return obj

    def set_desired_capacity(self, group_name, desired_capacity, honor_cooldown=False):
        return self.conn.set_desired_capacity(group_name, desired_capacity, honor_cooldown)

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

    def get_all_launch_configurations(self, config_names, max_records, next_token):
        obj = self.conn.get_all_launch_configurations(names=config_names, max_records=max_records, next_token=next_token)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/AS_LaunchConfigs.json")
        return obj

