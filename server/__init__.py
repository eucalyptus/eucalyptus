
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

class LoginHandler(tornado.web.RequestHandler): #(BaseHandler):

    def get(self):
        path = None
        try:
            path = config.get('eui', 'staticpath')+"eui.html"
            # ensure session is cleared
            self.set_cookie("session-id", "")
            # could redirect to login page...
            self.render(path, username=None)
        except Exception, err:
            print "Error: %s-%s" % (path,err)
            return

    def post(self):
        # validate user from args, get back tokens, then
        user = self.get_argument("username")
        passwd = self.get_argument("password")
        access_id='L52ISGKFHSEXSPOZYIZ1K'
        secret_key='YRRpiyw333aq1se5PneZEnskI9MMNXrSoojoJjat'
        # create session and store info there, set session id in cookie
        cookie = os.urandom(16).encode('hex');
        print "session id : "+cookie
        self.set_cookie("session-id", cookie);
        sessions[cookie] = UserSession(user, passwd, access_id, secret_key)
        self.redirect("/eui.html")
