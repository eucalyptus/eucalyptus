#!/usr/bin/python -tt
import tornado.ioloop
import ConfigParser
import os

import server
from server import api
from server import support

settings = {
  "cookie_secret": "YzRmYThkNzU1NDU2NmE1ZjYxMDZiZDNmMzI4YmMzMmMK",
  "xsrf_cookies": True,
}

application = tornado.web.Application([
        (r"/(favicon\.ico)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './static/images/')}),
        (r"/css/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './static/css')}),
        (r"/lib/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './static/lib')}),
        (r"/js/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './static/js')}),
        (r"/custom/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './static/custom')}),
        (r"/images/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './static/images')}),
        (r"/help/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './static/help')}),
        (r"/fonts/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './static/fonts')}),
        (r"/ec2", api.ComputeHandler),
        (r"/support", support.ComputeHandler),
        (r"/(.*)", server.RootHandler),
    ], **settings)

if __name__ == "__main__":
#    (hostname, alt_host, ipaddrs) = socket.gethostbyaddr(socket.gethostname())
#    for ip in ipaddrs:
#      print "host IP: "+ip
    server.config = ConfigParser.ConfigParser()
    server.config.read('server/eui.ini')
    application.listen(server.config.getint('eui', 'uiport'))
    tornado.ioloop.IOLoop.instance().start()
