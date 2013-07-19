import tornado.websocket

class PushHandler(tornado.websocket.WebSocketHandler):
    def initialize(self, list):
        list[self.__class__.__name__] = self
        self.list = list

    def open(self):
        print "WebSocket opened"
        self.write_message("welcome!!")

    def on_message(self, message):
        self.write_message(u"You said: " + message)

    def on_close(self):
        print "WebSocket closed"

