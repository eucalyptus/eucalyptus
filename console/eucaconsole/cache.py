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

from datetime import datetime, timedelta
import threading

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
