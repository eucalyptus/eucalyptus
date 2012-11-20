from datetime import datetime, timedelta
import threading

class Cache(object):

    def __init__(self, updateFreq):
        self.updateFreq = updateFreq
        self.lastUpdate = datetime.min
        self._values = None
        self._lock = threading.Lock()
        self.freshData = True

    def isCacheStale(self):
        return ((datetime.now() - self.lastUpdate) > timedelta(seconds = self.updateFreq))

    def isCacheFresh(self):
        ret = self.freshData
        self.freshData = False
        return ret

    def expireCache(self):
        self.lastUpdate = datetime.min

    @property
    def values(self):
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
