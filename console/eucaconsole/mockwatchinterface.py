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
from .watchinterface import WatchInterface
from .configloader import ConfigLoader

# This class provides an implmentation of the clcinterface using canned json
# strings. Might be better to represent as object graph so we can modify
# values in the mock.
class MockWatchInterface(WatchInterface):
    metrics = None
    statistics = None

    # load saved state to simulate CLC
    def __init__(self):
        self.config = ConfigLoader().getParser()
        if self.config.has_option('server', 'mockpath'):
            self.mockpath = self.config.get('server', 'mockpath')
        else:
            self.mockpath = 'mockdata'

        with open(os.path.join(self.mockpath, 'CW_Metrics.json')) as f:
            self.metrics = json.load(f, cls=BotoJsonDecoder)
        with open(os.path.join(self.mockpath, 'CW_Statistics.json')) as f:
            self.statistics = json.load(f, cls=BotoJsonDecoder)

    def get_metric_statistics(self, period, start_time, end_time, metric_name, namespace, statistics, dimensions=None, unit=None, callbcack=None):
        return self.statistics

    def list_metrics(self, next_token=None, dimensions=None, metric_name=None, namespace=None, callbcack=None):
        return self.metrics

    def put_metric_data(self, namespace, name, value=None, timestamp=None, unit=None, dimensions=None, statistics=None, callbcack=None):
        return True

    def delete_alarms(self, alarm_names):
        return True

    def enable_alarm_actions(self, alarm_names):
        return True

    def disable_alarm_actions(self, alarm_names):
        return True

    def put_metric_alarm(self, alarm):
        return True

