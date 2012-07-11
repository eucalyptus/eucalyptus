#!/usr/bin/python
import tornado.ioloop
import ConfigParser
import os

import server 

settings = {
  "cookie_secret": "YzRmYThkNzU1NDU2NmE1ZjYxMDZiZDNmMzI4YmMzMmMK",
  "login_url": "/login",
}

application = tornado.web.Application([
    (r"/(favicon\.ico)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './static/images/')}),
    (r"/css/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './static/css')}),
    (r"/lib/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './static/lib')}),
    (r"/js/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './js')}),
    (r"/images/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './static/images')}),
    (r"/ec2", server.EC2Handler),
    (r"/login", server.LoginHandler),
    (r"/(.*)", server.RootHandler),
], **settings)

if __name__ == "__main__":
#    (hostname, alt_host, ipaddrs) = socket.gethostbyaddr(socket.gethostname())
#    for ip in ipaddrs:
#      print "host IP: "+ip
    server.config = ConfigParser.ConfigParser()
    server.config.read('server/eui.ini')
    use_mock = server.config.getboolean('eui', 'usemock')
    application.listen(server.config.getint('eui', 'uiport'))
    tornado.ioloop.IOLoop.instance().start()


