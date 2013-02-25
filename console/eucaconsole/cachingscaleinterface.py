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

from .scaleinterface import ScaleInterface

# This class provides an implmentation of the clcinterface that caches responses
# from the underlying clcinterface. It will only make requests to the underlying layer
# at the rate defined by pollfreq. It is assumed this will be created per-session and
# therefore will only contain data for a single user. If a more global cache is desired,
# some things will need to be re-written.
class CachingScaleInterface(ScaleInterface):
    scaling = None

    # load saved state to simulate Walrus
    def __init__(self, scaleinterface, config):
        self.scaling = scaleinterface
        pollfreq = config.getint('server', 'pollfreq')
        try:
            freq = config.getint('server', 'pollfreq.scalinggroups')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.groups = Cache(freq)
        try:
            freq = config.getint('server', 'pollfreq.scalinginstances')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.instances = Cache(freq)
        try:
            freq = config.getint('server', 'pollfreq.launchconfigs')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.launchconfigs = Cache(freq)

    ##
    # autoscaling methods
    ##
    def create_auto_scaling_group(self, as_group, callback=None):
        self.groups.expireCache()
        params = {'as_group':as_group}
        Threads.instance().runThread(self.__create_auto_scaling_group_cb__, (params, callback))

    def __create_auto_scaling_group_cb__(self, kwargs, callback):
        try:
            ret = self.scaling.create_auto_scaling_group(kwargs['as_group'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def delete_auto_scaling_group(self, name, force_delete=False, callback=None):
        self.groups.expireCache()
        params = {'name':name, 'force_delete':force_delete}
        Threads.instance().runThread(self.__delete_auto_scaling_group_cb__, (params, callback))

    def __delete_auto_scaling_group_cb__(self, kwargs, callback):
        try:
            ret = self.scaling.delete_auto_scaling_group(kwargs['name'], kwargs['force_delete'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def get_all_groups(self, names=None, max_records=None, next_token=None, callback=None):
        params = {'names':names, 'max_records':max_records, 'next_token':next_token}
        Threads.instance().runThread(self.__get_all_groups_cb__, (params, callback))

    def __get_all_groups_cb__(self, kwargs, callback):
        try:
            self.groups.values = self.scaling.get_all_groups(kwargs['names'],
                                    kwargs['max_records'], kwargs['next_token'])
            Threads.instance().invokeCallback(callback, Response(data=self.groups.values))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def get_all_autoscaling_instances(self, instance_ids=None, max_records=None, next_token=None, callback=None):
        params = {'instance_ids':instance_ids, 'max_records':max_records,
                  'next_token':next_token}
        Threads.instance().runThread(self.__get_all_autoscaling_instances_cb__,
                                    (params, callback))

    def __get_all_autoscaling_instances_cb__(self, kwargs, callback):
        try:
            self.instances.values = self.scaling.get_all_autoscaling_instances(
                                            kwargs['instance_ids'],
                                            kwargs['max_records'], kwargs['next_token'])
            Threads.instance().invokeCallback(callback, Response(data=self.instances.values))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def set_desired_capacity(self, group_name, desired_capacity, honor_cooldown=False, callback=None):
        self.groups.expireCache()
        params = {'group_name':group_name, 'desired_capacity':desired_capacity,
                  'honor_cooldown':honor_cooldown}
        Threads.instance().runThread(self.__set_desired_capacity_cb__, (params, callback))

    def __set_desired_capacity_cb__(self, kwargs, callback):
        try:
            ret = self.scaling.set_desired_capacity(kwargs['group_name'],
                            kwargs['desired_capacity'], kwargs['honor_cooldown'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def set_instance_health(self, instance_id, health_status, should_respect_grace_period=True):
        params = {'instance_id':instance_id, 'health_status':health_status,
                  'should_respect_grace_period':should_respect_grace_period}
        Threads.instance().runThread(self.__set_instance_health_cb__, (params, callback))

    def __set_instance_health_cb__(self, kwargs, callback):
        try:
            ret = self.scaling.set_instance_health(kwargs['instance_id'],
                            kwargs['health_status'], kwargs['should_respect_grace_period'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def terminate_instance(self, instance_id, decrement_capacity=True):
        params = {'instance_id':instance_id, 'decrement_capacity':decrement_capacity}
        Threads.instance().runThread(self.__terminate_instance_cb__, (params, callback))

    def __terminate_instance_cb__(self, kwargs, callback):
        try:
            ret = self.scaling.set_instance_health(kwargs['instance_id'],
                            kwargs['decrement_capacity'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def update_autoscaling_group(self, as_group, callback=None):
        self.groups.expireCache()
        params = {'as_group':as_groupaunch_config_name}
        Threads.instance().runThread(self.__update_autoscaling_group_cb__, (params, callback))

    def __update_autoscaling_group_cb__(self, kwargs, callback):
        try:
            ret = self.scaling.update_autoscaling_group(kwargs['as_group'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def create_launch_configuration(self, launch_config, callback=None):
        self.launchconfigs.expireCache()
        params = {'launch_config':launch_config}
        Threads.instance().runThread(self.__create_launch_configuration_cb__, (params, callback))

    def __create_launch_configuration_cb__(self, kwargs, callback):
        try:
            ret = self.scaling.create_launch_configuration(kwargs['launch_config'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def delete_launch_configuration(self, launch_config_name, callback=None):
        self.launchconfigs.expireCache()
        params = {'launch_config_name':launch_config_name}
        Threads.instance().runThread(self.__delete_launch_configuration_cb__, (params, callback))

    def __delete_launch_configuration_cb__(self, kwargs, callback):
        try:
            ret = self.scaling.delete_launch_configuration(kwargs['launch_config_name'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def get_all_launch_configurations(self, configuration_names=None, max_records=None, next_token=None, callback=None):
        params = {'names':configuration_names,
                  'max_records':max_records, 'next_token':next_token}
        Threads.instance().runThread(self.__get_all_launch_configurations_cb__, (params, callback))

    def __get_all_launch_configurations_cb__(self, kwargs, callback):
        try:
            self.launchconfigs.values = self.scaling.get_all_launch_configurations(
                                 kwargs['names'], kwargs['max_records'], kwargs['next_token'])
            Threads.instance().invokeCallback(callback, Response(data=self.launchconfigs.values))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

class Response(object):
    data = None
    error = None

    def __init__(self, data=None, error=None):
        self.data = data
        self.error = error
