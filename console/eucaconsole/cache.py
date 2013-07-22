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
import logging
import threading
from boto.ec2.ec2object import EC2Object
from datetime import datetime, timedelta
import pushhandler

# This contains methods to act on all caches within the session.
class CacheManager(object):

    # This function is called by the api layer to get a summary of caches for the dashboard
    def get_cache_summary(self, session, zone):
        # make sparse array containing names of resource with updates
        summary = {}
        summary['image'] = len(session.clc.caches['images'].values)if session.clc.caches['images'].values else 0
        numRunning = 0;
        numStopped = 0;
        #logging.info("CACHE SUMMARY: about to calculate summary info for zone :"+zone)
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
        #logging.info("CACHE SUMMARY: instance running :"+str(numRunning))
        summary['keypair'] = len(session.clc.caches['keypairs'].values)if session.clc.caches['keypairs'].values else 0
        summary['sgroup'] = len(session.clc.caches['groups'].values)if session.clc.caches['groups'].values else 0
        summary['volume'] = len(session.clc.caches['volumes'].values)if session.clc.caches['volumes'].values else 0
        summary['snapshot'] = len(session.clc.caches['snapshots'].values)if session.clc.caches['snapshots'].values else 0
        summary['eip'] = len(session.clc.caches['addresses'].values)if session.clc.caches['addresses'].values else 0
        if session.scaling != None:
            summary['scalinginst'] = len(session.scaling.caches['scalinginsts'].values)if session.scaling.caches['scalinginsts'].values else 0
        return summary

    # This method is called to define which caches are refreshed regularly.
    # The normal mode would be to update all caches based on their configured poll frequency.
    # When min.clc.polling is set, the passed resource list will be used to determine which
    # caches to refresh (as a means to reduce CLC load).
    def set_data_interest(self, session, resources):
        try:
            self.min_polling = eucaconsole.config.getboolean('server', 'min.clc.polling')
        except ConfigParser.NoOptionError:
            self.min_polling = False
        # aggregate caches into single list
        caches = {}
        for res in session.clc.caches:
            caches[res] = session.clc.caches[res]
        if session.scaling:
            for res in session.scaling.caches:
                caches[res] = session.scaling.caches[res]
        if session.cw:
            for res in session.cw.caches:
                caches[res] = session.cw.caches[res]
        if session.elb:
            for res in session.elb.caches:
                caches[res] = session.elb.caches[res]
        # clear previous timers
        for res in caches:
            caches[res].cancelTimer()
        if self.min_polling:
            # start timers for new list of resources
            for res in resources:
                caches[res].startTimer({})
        else:
            # start timers for all cached resources
            for res in caches:
                caches[res].startTimer({})
        return True
    

class Cache(object):

    def __init__(self, name, updateFreq, getcall):
        self.name = name
        self.updateFreq = updateFreq
        self.lastUpdate = datetime.min
        self._getcall = getcall
        self._timer = None
        self._values = []
        self._lock = threading.Lock()
        self._freshData = True
        self._filters = None

    # staleness is determined by an age calculation (based on updateFreq)
    def isCacheStale(self, filters=None):
        if cmp(filters, self._filters) != 0:
            return True
        return ((datetime.now() - self.lastUpdate) > timedelta(seconds = self.updateFreq))

    # freshness is defined (not as !stale, but) as new data which has not been read yet
    def isCacheFresh(self):
        ret = self._freshData
        return ret

    def expireCache(self):
        self.lastUpdate = datetime.min
        # get timer restarted now to get data faster
        self.cancelTimer();
        self.__cache_load_callback__({}, self.updateFreq, False)

    def filters(self, filters):
        self._filters = filters

    @property
    def values(self):
        self._freshData = False
        return self._values

    @values.setter
    def values(self, value):
        self._lock.acquire()
        try:
            # this is a weak test, but mark cache fresh if the number of values changes
            # should do a smarter comparison if lengths are equal to detect changes in state
            #if self._values == None or len(self._values) != len(value):
            self._freshData = True
            self._values = value
            self.lastUpdate = datetime.now()
            if self.isCacheFresh():
                pushhandler.push_handler.write_message(self.name)
        finally:
            self._lock.release()

    def startTimer(self, kwargs): 
        self.__cache_load_callback__(kwargs, self.updateFreq, True)

    def cancelTimer(self):
        if self._timer:
            self._timer.cancel()
            self._timer = None

    def __cache_load_callback__(self, kwargs, interval, firstRun=False):
        local_interval = interval
        if firstRun:
            # use really small interval to cause background fetch very quickly
            local_interval = 0.1    # how about some randomness to space out requests slightly?
        else:
            logging.info("CACHE: fetching values for :"+str(self._getcall.__name__))
            self.values = self._getcall(kwargs)
        self.timer = threading.Timer(local_interval, self.__cache_load_callback__, [kwargs, interval, False])
        self.timer.start()

