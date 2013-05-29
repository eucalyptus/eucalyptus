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
from boto.ec2.cloudwatch import CloudWatchConnection
from boto.ec2.regioninfo import RegionInfo

import eucaconsole
from .botojsonencoder import BotoJsonWatchEncoder
from .watchinterface import WatchInterface

# This class provides an implmentation of the clcinterface using boto
class BotoWatchInterface(WatchInterface):
    conn = None
    saveclcdata = False

    def __init__(self, clc_host, access_id, secret_key, token):
        boto.set_stream_logger('foo')
        path='/services/CloudWatch'
        port=8773
        if clc_host[len(clc_host)-13:] == 'amazonaws.com':
            clc_host = clc_host.replace('ec2', 'monitoring', 1)
            path = '/'
            reg = None
            port=443
        reg = RegionInfo(name='eucalyptus', endpoint=clc_host)
        self.conn = CloudWatchConnection(access_id, secret_key, region=reg,
                                  port=port, path=path,
                                  is_secure=True, security_token=token, debug=2)
        self.conn.https_validate_certificates = False
        self.conn.http_connection_kwargs['timeout'] = 30

    def __save_json__(self, obj, name):
        f = open(name, 'w')
        json.dump(obj, f, cls=BotoJsonWatchEncoder, indent=2)
        f.close()

    def get_metric_statistics(self, period, start_name, end_time, metric_name, namespace, statistics, dimensions, unit):
        obj = self.conn.get_metric_statistics(period, start_name, end_time, metric_name, namespace, statistics, dimensions, unit)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/CW_Statistics.json")
        return obj

    def list_metrics(self, next_token, dimensions, metric_name, namespace):
        obj = self.conn.list_metrics(next_token, dimensions, metric_name, namespace)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/CW_Metrics.json")
        return obj

    def put_metric_data(self, namespace, name, value, timestamp, unit, dimensions, statistics):
        return self.conn.put_metric_data(namespace, name, value, timestamp, unit, dimensions, statistics)

    def describe_alarms(self, action_prefix=None, alarm_name_prefix=None, alarm_names=None, max_records=None,
                        state_value=None, next_token=None):
        obj = self.conn.describe_alarms(action_prefix, alarm_name_prefix, alarm_names, max_records, state_value, next_token)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/CW_Alarms.json")
        return obj

    def delete_alarms(self, alarm_names):
        return self.conn.delete_alarms(alarm_names)

    def enable_alarm_actions(self, alarm_names):
        return self.conn.enable_alarm_actions(alarm_names)

    def disable_alarm_actions(self, alarm_names):
        return self.conn.disable_alarm_actions(alarm_names)

    def put_metric_alarm(self, alarm):
        return self.conn.put_metric_alarm(alarm)

