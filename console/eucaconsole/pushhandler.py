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

import logging
import threading
import tornado.websocket

push_handler = None

# This class handles the websocket connection, primarily used for informing
# the client of new data in caches.
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
