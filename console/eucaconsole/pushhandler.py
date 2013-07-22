import logging
import tornado.websocket

push_handler = None

class PushHandler(tornado.websocket.WebSocketHandler):
    def initialize(self):
        global push_handler
        push_handler = self

    def open(self):
        print "WebSocket opened"

    def on_message(self, message):
        self.write_message(u"You said: " + message)

    def on_close(self):
        print "WebSocket closed"

