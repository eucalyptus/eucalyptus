
import tornado.web
import os
import io
import socket
import random
import json
import ConfigParser
from botoclcinterface import BotoClcInterface
from mockclcinterface import MockClcInterface

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
    session = None

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

    def validate_session(self):
        session_id = self.get_cookie("session-id")
        if not(session_id):
            self.redirect("/login")
        try:
            self.session = sessions[session_id]
        except KeyError:
            self.redirect("/login")

    def should_use_mock(self):
        use_mock = config.getboolean('eui', 'usemock')
        return use_mock

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
        self.validate_session()
        if self.should_use_mock():
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

class LoginHandler(tornado.web.RequestHandler): #(BaseHandler):

    def get(self):
        path = None
        try:
            path = config.get('eui', 'staticpath')+"login.html"
            # could redirect to login page...
            self.render(path, username=None)
            # ensure session is cleared
            self.set_cookie("session-id", "")
        except Exception, err:
            print "Error: %s-%s" % (path,err)
            return

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
