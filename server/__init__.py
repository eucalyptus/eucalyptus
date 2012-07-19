
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
data_helper = None
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
        path = config.get('eui', 'staticpath')+"eui.html" #path
        self.render(path, username=self.get_current_user())

class LoginHandler(tornado.web.RequestHandler):
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
        self.set_cookie("session-id", cookie);
        sessions[cookie] = UserSession(user, passwd, access_id, secret_key)
        context = ContextHelper(user)
        text_data =  {'footer':data_helper.get_data('text','footer')}
        img_data = {'logo':data_helper.get_data('image','logo')}
        self.write({'context':context.get_context(), 'text':text_data, 'image':img_data})

# this class is responsible for getting user-specific, 
# cloud-specific contextual data sent to the browser upon login
class ContextHelper():
    def __init__(self, username):
        self.username=username

    def get_full_name(self):
        return "Sang-Min Park"

    def get_time_zone(self):
        return "-7"
    
    def get_url_home(self):
        return "http://192.168.0.107:8888/"
 
    def get_language(self):
	return "en_US"

    def get_context(self):
        return {'username':self.username, 'fullname':self.get_full_name(), 'timezone':self.get_time_zone(), 'language':self.get_language(), 'url_home':self.get_url_home()}

# this class abstracts the customizable text sent to the browser 
class DataHelper():
    def __init__(self):
        data_path = config.get('eui', 'datapath')
        self.data_config = ConfigParser.ConfigParser()
	if len(self.data_config.read(data_path)) != 1:
	    raise Exception("Cannot read the data file: %s" % data_path)

    def get_data(self, scope, key):
        return self.data_config.get(scope, key)

