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

import ConfigParser
import eucaconsole
import threading
from boto.ec2.ec2object import EC2Object
from datetime import datetime, timedelta

# This contains methods to act on all caches within the session.
class CacheManager(object):
    def get_cache_summary(self, session, zone):
        # make sparse array containing names of resource with updates
        summary = {}
        summary['image'] = len(session.clc.caches['images'].values)if session.clc.caches['images'].values else 0
        numRunning = 0;
        numStopped = 0;
        if session.clc.caches['instances'].values:
            for reservation in session.clc.caches['instances'].values:
                if issubclass(reservation.__class__, EC2Object):
                    for inst in reservation.instances:
                        if zone == 'all' or inst.placement == zone:
                            state = inst.state
                            if state == 'running':
                                numRunning += 1
                            elif state == 'stopped':
                                numStopped += 1 
                else:
                    for inst in reservation['instances']:
                        if zone == 'all' or inst['placement'] == zone:
                            state = inst['state']
                            if state == 'running':
                                numRunning += 1
                            elif state == 'stopped':
                                numStopped += 1 
        summary['inst_running'] = numRunning
        summary['inst_stopped'] = numStopped
        summary['keypair'] = len(session.clc.caches['keypairs'].values)if session.clc.caches['keypairs'].values else 0
        summary['sgroup'] = len(session.clc.caches['groups'].values)if session.clc.caches['groups'].values else 0
        summary['volume'] = len(session.clc.caches['volumes'].values)if session.clc.caches['volumes'].values else 0
        summary['snapshot'] = len(session.clc.caches['snapshots'].values)if session.clc.caches['snapshots'].values else 0
        summary['eip'] = len(session.clc.caches['addresses'].values)if session.clc.caches['addresses'].values else 0
        summary['scalinginst'] = len(session.scaling.caches['scalinginsts'].values)if session.scaling.caches['scalinginsts'].values else 0
        return summary

    def __cache_load_callback__(self, caches, resource, kwargs, interval, firstRun=False):
        if firstRun:
            caches[resource].expireCache()
        else:
            caches[resource].values = caches['get_'+resource](kwargs)
        caches['timer_'+resource] = threading.Timer(interval, self.__cache_load_callback__, [caches, resource, kwargs, interval, False])
        caches['timer_'+resource].start()

    def set_data_interest(self, session, resources):
        try:
            self.min_polling = eucaconsole.config.getboolean('server', 'min.clc.polling')
        except ConfigParser.NoOptionError:
            self.min_polling = False
        # aggregate caches into single list
        caches = {}
        for res in session.clc.caches:
            caches[res] = session.clc.caches[res]
        for res in session.scaling.caches:
            caches[res] = session.scaling.caches[res]
        # clear previous timers
        for res in caches:
            if res[:5] == 'timer' and caches[res]:
                caches[res].cancel()
                caches[res] = None
        if self.min_polling:
            # start timers for new list of resources
            for res in resources:
                self.__cache_load_callback__(caches, res, {}, caches[res].updateFreq, True)
        else:
            # start timers for all cached resources
            for vals in caches:
                if isinstance(caches[vals], Cache):
                    self.__cache_load_callback__(caches, vals, {}, caches[vals].updateFreq, True)
        return True
    

class Cache(object):

    def __init__(self, updateFreq):
        self.updateFreq = updateFreq
        self.lastUpdate = datetime.min
        self._values = None
        self._lock = threading.Lock()
        self.freshData = True
        self.filters = None

    # staleness is determined by an age calculation (based on updateFreq)
    def isCacheStale(self, filters=None):
        if cmp(filters, self.filters) != 0:
            return True
        return ((datetime.now() - self.lastUpdate) > timedelta(seconds = self.updateFreq))

    # freshness is defined (not as !stale, but) as new data which has not been read yet
    def isCacheFresh(self):
        ret = self.freshData
        return ret

    def expireCache(self):
        self.lastUpdate = datetime.min

    def filters(self, filters):
        self.filters = filters

    @property
    def values(self):
        self.freshData = False
        return self._values

    @values.setter
    def values(self, value):
        self._lock.acquire()
        try:
            # this is a weak test, but mark cache fresh if the number of values changes
            # should do a smarter comparison if lengths are equal to detect changes in state
            if self._values == None or len(self._values) != len(value):
                self.freshData = True
            self._values = value
            self.lastUpdate = datetime.now()
        finally:
            self._lock.release()
