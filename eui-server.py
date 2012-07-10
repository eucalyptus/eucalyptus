#!/usr/bin/python

import tornado.ioloop
import tornado.web
import os
import io
import socket
import random
import json
import ConfigParser
from botoclcinterface import BotoClcInterface
from mockclcinterface import MockClcInterface

# dictionary for in-memory sessions
sessions = {}
use_mock = True
config = None

class UserSession(object):
  def __init__(self, username, password, access_id, secret_key):
    self.username = username
    self.password = password
    self.access_id = access_id
    self.secret_key = secret_key

class BaseHandler(tornado.web.RequestHandler):
    def get_current_user(self):
        session_id = self.get_cookie("session-id")
        if not(session_id):
          self.redirect("/login")
        try:
          session = sessions[session_id]
        except KeyError:
          return None
        return session.username

    def get_user_locale(self):
      return None  # we could implement something here.

class RootHandler(BaseHandler):
    @tornado.web.authenticated

    def get(self, path):
        path = config.get('eui', 'staticpath')+path
        if path.endswith('.html'):
          self.render(path, username=self.get_current_user())
        else:
          print "path="+path
          if os.path.exists(path):
            f = open(path, 'r')
            self.write(f.read())
          else:
            print "about to set error status 404"
            self.send_error(status_code=404)

class EC2Handler(BaseHandler):
    @tornado.web.authenticated
    
    def get(self):
#        print "Hello, " + self.current_user
        session_id = self.get_cookie("session-id")
        if not(session_id):
          self.redirect("/login")
        try:
          session = sessions[session_id]
        except KeyError:
          self.redirect("/login")
        if use_mock:
          clc = MockClcInterface()
        else:
          clc = BotoClcInterface(config.get('eui', 'clchost'),
                  session.access_id, session.secret_key)
        data_type = self.get_argument("type")
        ret = []
        if data_type == "image":
          ret = clc.get_all_images()
        if data_type == "instance":
          ret = clc.get_all_instances()
        elif data_type == "address":
          ret = clc.get_all_addresses()
        elif data_type == "key":
          ret = clc.get_all_key_pairs()
        elif data_type == "group":
          ret = clc.get_all_security_groups()
        elif data_type == "volume":
          ret = clc.get_all_volumes()
        elif data_type == "snapshot":
          ret = clc.get_all_snapshots()
        self.write(ret)

class LoginHandler(BaseHandler):
    def get(self):
        # could redirect to login page...
        self.render("login.html", username=None)
        # ensure session is cleared
        self.set_cookie("session-id", "")

    def post(self):
        # validate user from args, get back tokens, then
        user = self.get_argument("name")
        passwd = self.get_argument("passwd")
        access_id='L52ISGKFHSEXSPOZYIZ1K'
        secret_key='YRRpiyw333aq1se5PneZEnskI9MMNXrSoojoJjat'
        # create session and store info there, set session id in cookie
        cookie = os.urandom(16).encode('hex');
        print "session id : "+cookie
        self.set_cookie("session-id", cookie);
        sessions[cookie] = UserSession(user, passwd, access_id, secret_key)
        self.redirect("/eui.html")

settings = {
  "cookie_secret": "YzRmYThkNzU1NDU2NmE1ZjYxMDZiZDNmMzI4YmMzMmMK",
  "login_url": "/login",
}

application = tornado.web.Application([
    (r"/(favicon\.ico)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './')}),
    (r"/css/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './css')}),
    (r"/images/(.*)", tornado.web.StaticFileHandler, {'path': os.path.join(os.path.dirname(__file__), './images')}),
    (r"/ec2", EC2Handler),
    (r"/login", LoginHandler),
    (r"/(.*)", RootHandler),
], **settings)

if __name__ == "__main__":
#    (hostname, alt_host, ipaddrs) = socket.gethostbyaddr(socket.gethostname())
#    for ip in ipaddrs:
#      print "host IP: "+ip
    config = ConfigParser.ConfigParser()
    config.read('eui.ini')
    use_mock = config.getboolean('eui', 'usemock')
    application.listen(config.getint('eui', 'uiport'))
    tornado.ioloop.IOLoop.instance().start()


