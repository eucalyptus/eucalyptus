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

from cache import Cache
import ConfigParser

from eucaconsole.threads import Threads

from .watchinterface import WatchInterface

# This class provides an implmentation of the clcinterface that caches responses
# from the underlying clcinterface. It will only make requests to the underlying layer
# at the rate defined by pollfreq. It is assumed this will be created per-session and
# therefore will only contain data for a single user. If a more global cache is desired,
# some things will need to be re-written.
class CachingWatchInterface(WatchInterface):
    cw = None

    # load saved state to simulate Walrus
    def __init__(self, watchinterface, config):
        self.cw = watchinterface
        pollfreq = config.getint('server', 'pollfreq')
        try:
            freq = config.getint('server', 'pollfreq.metrics')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.metrics = Cache(freq)
        try:
            freq = config.getint('server', 'pollfreq.alarms')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.alarms = Cache(freq)

    ##
    # cloud watch methods
    ##
    def get_metric_statistics(self, period, start_time, end_time, metric_name, namespace, statistics, dimensions=None, unit=None, callback=None):
        params = {'period':period, 'start_time':start_time, 'end_time':end_time, 'metric_name':metric_name,
                  'namespace':namespace, 'statistics':statistics, 'dimensions':dimensions, 'unit':unit}
        Threads.instance().runThread(self.__get_metric_statistics_cb__, (params, callback))

    def __get_metric_statistics_cb__(self, kwargs, callback):
        try:
            ret = self.cw.get_metric_statistics(kwargs['period'], kwargs['start_time'], kwargs['end_time'],
                            kwargs['metric_name'], kwargs['namespace'], kwargs['statistics'],
                            kwargs['dimensions'], kwargs['unit'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def list_metrics(self, next_token=None, dimensions=None, metric_name=None, namespace=None, callback=None):
        if self.metrics.isCacheStale():
            params = {'next_token':next_token, 'dimensions':dimensions, 'metric_name':metric_name, 'namespace':namespace}
            Threads.instance().runThread(self.__list_metrics_cb__, (params, callback))
        else:
            callback(Response(data=self.metrics.values))

    def __list_metrics_cb__(self, kwargs, callback):
        try:
            self.metrics.values = self.cw.list_metrics(kwargs['next_token'], kwargs['dimensions'],
                                       kwargs['metric_name'], kwargs['namespace'])
            Threads.instance().invokeCallback(callback, Response(data=self.metrics.values))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def put_metric_data(self, namespace, data, callback=None):
        params = {'namespace':namespace, 'data':data}
        Threads.instance().runThread(self.__put_metric_data_cb__, (params, callback))

    def __put_metric_data_cb__(self, kwargs, callback):
        try:
            data = kwargs['data']
            for d in data:
              ret = self.cw.put_metric_data(d['namespace'], d['name'], d['value'],
                                       d['timestamp'], d['unit'], d['dimensions'],
                                       d['statistics'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def describe_alarms(self, action_prefix=None, alarm_name_prefix=None, alarm_names=None, max_records=None,
                        state_value=None, next_token=None, callback=None):
        if self.alarms.isCacheStale():
            params = {'action_prefix':action_prefix, 'alarm_name_prefix':alarm_name_prefix, 'alarm_names':alarm_names,
                      'max_records':max_records, 'state_value':state_value, 'next_token':next_token}
            Threads.instance().runThread(self.__describe_alarms_cb__, (params, callback))
        else:
            callback(Response(data=self.alarms.values))

    def __describe_alarms_cb__(self, kwargs, callback):
        try:
            self.alarms.values = self.cw.describe_alarms(kwargs['action_prefix'], kwargs['alarm_name_prefix'], kwargs['alarm_names'],
                                                         kwargs['max_records'], kwargs['state_value'], kwargs['next_token'])
            Threads.instance().invokeCallback(callback, Response(data=self.alarms.values))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def delete_alarms(self, alarm_names, callback=None):
        Threads.instance().runThread(self.__delete_alarms_cb__, ({'names':alarm_names}, callback))

    def __delete_alarms_cb__(self, kwargs, callback):
        try:
            ret = self.cw.delete_alarms(kwargs['names'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def enable_alarm_actions(self, alarm_names, callback=None):
        Threads.instance().runThread(self.__enable_alarm_actions_cb__, ({'names':alarm_names}, callback))

    def __enable_alarm_actions_cb__(self, kwargs, callback):
        try:
            ret = self.cw.enable_alarm_actions(kwargs['names'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def disable_alarm_actions(self, alarm_names, callback=None):
        Threads.instance().runThread(self.__disable_alarm_actions_cb__, ({'names':alarm_names}, callback))

    def __disable_alarm_actions_cb__(self, kwargs, callback):
        try:
            ret = self.cw.disable_alarm_actions(kwargs['names'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def put_metric_alarm(self, alarm, callback=None):
        Threads.instance().runThread(self.__put_metric_alarm_cb__, ({'alarm':alarm}, callback))

    def __put_metric_alarm_cb__(self, kwargs, callback):
        try:
            ret = self.cw.put_metric_alarm(kwargs['alarm'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

class Response(object):
    data = None
    error = None

    def __init__(self, data=None, error=None):
        self.data = data
        self.error = error
