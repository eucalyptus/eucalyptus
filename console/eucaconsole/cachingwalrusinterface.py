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

from .walrusinterface import WalrusInterface

# This class provides an implmentation of the clcinterface that caches responses
# from the underlying clcinterface. It will only make requests to the underlying layer
# at the rate defined by pollfreq. It is assumed this will be created per-session and
# therefore will only contain data for a single user. If a more global cache is desired,
# some things will need to be re-written.
class CachingWalrusInterface(WalrusInterface):
    walrus = None
    caches = None

    # load saved state to simulate Walrus
    def __init__(self, walrusinterface, config):
        self.walrus = walrusinterface
        self.caches = {}
        pollfreq = config.getint('server', 'pollfreq')
        try:
            freq = config.getint('server', 'pollfreq.buckets')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.caches['buckets'] = Cache(freq, self.walrus.get_all_buckets)
        try:
            freq = config.getint('server', 'pollfreq.objects')
        except ConfigParser.NoOptionError:
            freq = pollfreq
        self.caches['objects'] = Cache(freq, self.walrus.get_all_objects)

    def get_all_buckets(self, callback):
        # if cache stale, update it
        if self.caches['buckets'].isCacheStale():
            Threads.instance().runThread(self.__get_all_buckets_cb__, ({}, callback))
        else:
            callback(Response(data=self.caches['buckets'].values))

    def __get_all_buckets_cb__(self, kwargs, callback):
        try:
            self.caches['buckets'].values = self.walrus.get_all_buckets()
            Threads.instance().invokeCallback(callback, Response(data=self.caches['buckets'].values))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))

    def get_all_objects(self, bucket, callback):
        # if cache stale, update it
        if self.caches['objects'].isCacheStale():
            Threads.instance().runThread(self.__get_all_objects_cb__, ({'bucket':bucket}, callback))
        else:
            callback(Response(data=self.caches['objects'].values))

    def __get_all_objects_cb__(self, kwargs, callback):
        try:
            self.caches['buckets'].values = self.walrus.get_all_objects(kwargs['bucket'])
            Threads.instance().invokeCallback(callback, Response(data=self.caches['objects'].values))
        except Exception as ex:
            Threads.instance().invokeCallback(callback, Response(error=ex))


class Response(object):
    data = None
    error = None

    def __init__(self, data=None, error=None):
        self.data = data
        self.error = error
