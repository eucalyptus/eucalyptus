from datetime import datetime, timedelta
import threading

class Cache(object):

    def __init__(self, updateFreq):
        self.updateFreq = updateFreq
        self.lastUpdate = datetime.min
        self._values = None
        self._lock = threading.Lock()

    def isCacheStale(self):
        return ((datetime.now() - self.lastUpdate) > timedelta(seconds = self.updateFreq))

    def expireCache(self):
        self.lastUpdate = datetime.min

    @property
    def values(self):
        return self._values

    @values.setter
    def values(self, value):
        self._lock.acquire()
        try:
            self._values = value
            self.lastUpdate = datetime.now()
        finally:
            self._lock.release()
