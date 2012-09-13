#!/usr/bin/python -tt
import tornado.ioloop
import os

import server
from server import api
from server import support
from server.configloader import ConfigLoader

settings = {
  "cookie_secret": "getting-from-config-now",
  "xsrf_cookies": True,
}

# default webroot location for development
webroot = os.path.join(os.path.dirname(__file__), 'static')

server.config = ConfigLoader.getParser()
# When staticpath is in the config we will assume that it is
# not a relative path
if server.config.has_option('eui', 'staticpath'):
    webroot = server.config.get('eui', 'staticpath')

if server.config.has_option('eui', 'cookie.secret'):
    settings['cookie_secret'] = server.config.get('eui', 'cookie.secret')

application = tornado.web.Application([
        (r"/(favicon\.ico)", tornado.web.StaticFileHandler, {'path': os.path.join(webroot, 'images')}),
        (r"/css/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(webroot, 'css')}),
        (r"/lib/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(webroot, 'lib')}),
        (r"/js/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(webroot, 'js')}),
        (r"/custom/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(webroot, 'custom')}),
        (r"/images/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(webroot, 'images')}),
        (r"/help/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(webroot, 'help')}),
        (r"/fonts/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(webroot, 'fonts')}),
        (r"/ec2", api.ComputeHandler),
        (r"/support", support.ComputeHandler),
        (r"/checkip", server.CheckIpHandler),
        (r"/(.*)", server.RootHandler),
    ], **settings)

if __name__ == "__main__":
#    (hostname, alt_host, ipaddrs) = socket.gethostbyaddr(socket.gethostname())
#    for ip in ipaddrs:
#      print "host IP: "+ip
    application.listen(server.config.getint('eui', 'uiport'))
    tornado.ioloop.IOLoop.instance().start()
