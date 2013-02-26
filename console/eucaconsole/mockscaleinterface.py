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

import boto
import copy
import json
import os
import datetime

from operator import itemgetter
from boto.ec2.image import Image
from boto.ec2.instance import Instance
from boto.ec2.keypair import KeyPair

from .botojsonencoder import BotoJsonDecoder
from .scaleinterface import ScaleInterface
from .configloader import ConfigLoader

# This class provides an implmentation of the clcinterface using canned json
# strings. Might be better to represent as object graph so we can modify
# values in the mock.
class MockScaleInterface(ScaleInterface):
    groups = None
    instances = None
    configs = None

    # load saved state to simulate CLC
    def __init__(self):
        self.config = ConfigLoader().getParser()
        if self.config.has_option('server', 'mockpath'):
            self.mockpath = self.config.get('server', 'mockpath')
        else:
            self.mockpath = 'mockdata'

        with open(os.path.join(self.mockpath, 'AS_Groups.json')) as f:
            self.groups = json.load(f, cls=BotoJsonDecoder)
        with open(os.path.join(self.mockpath, 'AS_Instances.json')) as f:
            self.instances = json.load(f, cls=BotoJsonDecoder)
        with open(os.path.join(self.mockpath, 'AS_LaunchConfigs.json')) as f:
            self.configs = json.load(f, cls=BotoJsonDecoder)

    ##
    # autoscaling methods
    ##
    def create_auto_scaling_group(self, as_group, callback=None):
        return None

    def delete_auto_scaling_group(self, name, force_delete=False, callback=None):
        return None

    def get_all_groups(self, names=None, max_records=None, next_token=None, callback=None):
        return self.groups

    def get_all_autoscaling_instances(self, instance_ids=None, max_records=None, next_token=None, callback=None):
        return self.instances

    def set_desired_capacity(self, group_name, desired_capacity, honor_cooldown=False, callback=None):
        return None

    def set_instance_health(self, instance_id, health_status, should_respect_grace_period=True):
        return None

    def terminate_instance(self, instance_id, decrement_capacity=True):
        return None

    def update_autoscaling_group(self, as_group, callback=None):
        return None

    def create_launch_configuration(self, launch_config, callback=None):
        return None

    def delete_launch_configuration(self, launch_config_name, callback=None):
        return None

    def get_all_launch_configurations(self, configuration_names=None, max_records=None, next_token=None, callback=None):
        return self.configs


