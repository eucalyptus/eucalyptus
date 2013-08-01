import logging
import threading
import tornado.websocket

push_handler = None

class PushHandler(tornado.websocket.WebSocketHandler):
    LEAK_INTERVAL = 1.0

    def initialize(self):
        global push_handler
        push_handler = self
        self._lock = threading.Condition()
        self._timer = None
        self._queue = []    # use simple array

    def open(self):
        pass

    def on_message(self, message):
        # echo back... 
        self.write_message(message)

    def on_close(self):
        pass

    # These methods implment a modified leaky bucket. Modified in that
    # the queue is never full and the message are emitted together.
    #
    # This method will take messages to send and batch them up
    # in groups. A timer will be started so that any message won't
    # age more than a fixed amount of time. Any messages accumulated
    # in that time will be sent together. Messages might be delayed
    # at most by that timer interval
    def send(self, message):
        self._lock.acquire()
        self._queue.append(message)
        if not(self._timer): # no timer started, get one going
            self._timer = threading.Timer(self.LEAK_INTERVAL, self.__send__, [])
            self._timer.start()
        self._lock.release()

    # This method is the callback that holds a lock long enough to manipulate
    # data structures, then sends the message.
    def __send__(self):
        self._lock.acquire()
        message = str(self._queue)
        self._queue = []
        self._timer = None
        self._lock.release()
        self.write_message(message)
