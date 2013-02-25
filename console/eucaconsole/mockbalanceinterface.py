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
from .balanceinterface import BalanceInterface
from .configloader import ConfigLoader

# This class provides an implmentation of the clcinterface using canned json
# strings. Might be better to represent as object graph so we can modify
# values in the mock.
class MockBalanceInterface(BalanceInterface):
    balancers = None
    instances = None

    # load saved state to simulate CLC
    def __init__(self):
        self.config = ConfigLoader().getParser()
        if self.config.has_option('server', 'mockpath'):
            self.mockpath = self.config.get('server', 'mockpath')
        else:
            self.mockpath = 'mockdata'

        with open(os.path.join(self.mockpath, 'ELB_Balancers.json')) as f:
            self.balancers = json.load(f, cls=BotoJsonDecoder)
        with open(os.path.join(self.mockpath, 'ELB_Instances.json')) as f:
            self.instances = json.load(f, cls=BotoJsonDecoder)

    def create_load_balancer(self, name, zones, listeners, subnets=None,
                             security_groups=None, scheme='internet-facing', callback=None):
        return None
    
    def delete_load_balancer(self, name, callback=None):
        return None

    def get_all_load_balancers(self, load_balancer_names=None, callback=None):
        return self.balancers

    def deregister_instances(self, load_balancer_name, instances, callback=None):
        return None

    def register_instances(self, load_balancer_name, instances, callback=None):
        return None

    def create_load_balancer_listeners(self, name, listeners, callback=None):
        return None

    def delete_load_balancer_listeners(self, name, ports, callback=None):
        return None

    def configure_health_check(self, name, health_check, callback=None):
        return None

    def describe_instance_health(self, load_balancer_name, instances=None, callback=None):
        return self.instances

