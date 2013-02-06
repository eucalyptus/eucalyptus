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

from .balanceinterface import BalanceInterface

# This class provides an implmentation of the clcinterface that caches responses
# from the underlying clcinterface. It will only make requests to the underlying layer
# at the rate defined by pollfreq. It is assumed this will be created per-session and
# therefore will only contain data for a single user. If a more global cache is desired,
# some things will need to be re-written.
class CachingBalanceInterface(BalanceInterface):
    bal = None

    # load saved state to simulate Walrus
    def __init__(self, balanceinterface, config):
        self.bal = balanceinterface
        pollfreq = config.getint('server', 'pollfreq')
        try:
            freq = config.getint('server', 'pollfreq.balancers')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.balancers = Cache(freq)
        try:
            freq = config.getint('server', 'pollfreq.elb_instances')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.instances = Cache(freq)

    ##
    # elb methods
    ##
    def create_load_balancer(self, name, zones, listeners, subnets=None,
                             security_groups=None, scheme='internet-facing', callback=None):
        self.balancers.expireCache()
        params = {'name':name, 'zones':zones, 'listeners':listeners, 'subnets':subnets,
                  'security_groups':security_groups, 'scheme':scheme}
        Threads.instance().runThread(self.__create_load_balancer_cb__, (params, callback))
    
    def __create_load_balancer_cb__(self, kwargs, callback):
        try:
            ret = self.bal.create_load_balancer(kwargs['name'], kwargs['zones'], kwargs['listeners'],
                                    kwargs['subnets'], kwargs['security_groups'], kwargs['scheme'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def delete_load_balancer(self, name, callback=None):
        self.balancers.expireCache()
        params = {'name':name}
        Threads.instance().runThread(self.__delete_load_balancer_cb__, (params, callback))

    def __delete_load_balancer_cb__(self, kwargs, callback):
        try:
            ret = self.bal.delete_load_balancer(kwargs['name'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def get_all_load_balancers(self, load_balancer_names=None, callback=None):
        if self.balancers.isCacheStale():
            params = {'load_balancer_names':load_balancer_names}
            Threads.instance().runThread(self.__get_all_load_balancers_cb__, (params, callback))
        else:
            callback(Response(data=self.balancers.values))

    def __get_all_load_balancers_cb__(self, kwargs, callback):
        try:
            self.balancers.values = self.bal.get_all_load_balancers(kwargs['load_balancer_names'])
            Threads.instance().invokeCallback(callback, Response(data=self.balancers.values))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def deregister_instances(self, load_balancer_name, instances, callback=None):
        params = {'load_balancer_name':load_balancer_name, 'instances':instances}
        Threads.instance().runThread(self.__deregister_instances_cb__, (params, callback))

    def __deregister_instances_cb__(self, kwargs, callback):
        try:
            ret = self.bal.deregister_instances(kwargs['load_balancer_name'], kwargs['instances'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def register_instances(self, load_balancer_name, instances, callback=None):
        params = {'load_balancer_name':load_balancer_name, 'instances':instances}
        Threads.instance().runThread(self.__register_instances_cb__, (params, callback))

    def __register_instances_cb__(self, kwargs, callback):
        try:
            ret = self.bal.register_instances(kwargs['load_balancer_name'], kwargs['instances'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def create_load_balancer_listeners(self, name, listeners, callback=None):
        params = {'name':name, 'listeners':listeners}
        Threads.instance().runThread(self.__create_load_balancer_listeners_cb__, (params, callback))

    def __create_load_balancer_listeners_cb__(self, kwargs, callback):
        try:
            ret = self.bal.create_load_balancer_listeners(kwargs['name'], kwargs['listeners'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def delete_load_balancer_listeners(self, name, ports, callback=None):
        params = {'name':name, 'ports':ports}
        Threads.instance().runThread(self.__delete_load_balancer_listeners_cb__, (params, callback))

    def __delete_load_balancer_listeners_cb__(self, kwargs, callback):
        try:
            ret = self.bal.delete_load_balancer_listeners(kwargs['name'], kwargs['ports'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def configure_health_check(self, name, health_check, callback=None):
        params = {'name':name, 'health_check':health_check}
        Threads.instance().runThread(self.__configure_health_check_cb__, (params, callback))

    def __configure_health_check_cb__(self, kwargs, callback):
        try:
            ret = self.bal.configure_health_check(kwargs['name'], kwargs['health_check'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def describe_instance_health(self, load_balancer_name, instances=None, callback=None):
        if self.instances.isCacheStale():
            params = {'load_balancer_name':load_balancer_name, 'instances':instances}
            Threads.instance().runThread(self.__describe_instance_health_cb__, (params, callback))
        else:
            callback(Response(data=self.instancesalancers.values))

    def __describe_instance_health_cb__(self, kwargs, callback):
        try:
            ret = self.bal.describe_instance_health(kwargs['load_balancer_name'], kwargs['instances'])
            Threads.instance().invokeCallback(callback, Response(data=ret))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

class Response(object):
    data = None
    error = None

    def __init__(self, data=None, error=None):
        self.data = data
        self.error = error
