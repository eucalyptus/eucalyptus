
import tornado.web
import os
import io
import socket
import random
import json
import sys
import traceback
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
            return None
            #self.redirect("/login")
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
            self.redirect("/")
        try:
            self.session = sessions[session_id]
        except KeyError:
            self.redirect("/")

    def should_use_mock(self):
        use_mock = config.getboolean('eui', 'usemock')
        return use_mock

class RootHandler(BaseHandler):
    def get(self, path):
        path = config.get('eui', 'staticpath')+"eui.html"
        self.render(path, username=self.get_current_user())

    def post(self, arg):
        action = self.get_argument("action")
        response=None
        error=None
        if action == 'login':
            try:
                response=LoginProcessor.post(self)
            except Exception, err:
                traceback.print_exc(file=sys.stdout)
                error={'status_code':401,'message':'failed to log-in'}
        elif action == 'session':
	    try:
	        response=SessionProcessor.post(self)
	    except Exception, err:
                traceback.print_exc(file=sys.stdout)
		error={'status_code':400,'message':'can\'t retrieve session info'}
        else:
            error={'status_code':400,'message':'unknown action'}
        
        if error:
            raise tornado.web.HTTPError(error['status_code'],error['message'])
        else:
            self.write(ProxyResponse.generate(response))

    def check_xsrf_cookie(self):
        action = self.get_argument("action")
	if action == 'login':
            xsrf=self.xsrf_token
        else:
            super(RootHandler, self).check_xsrf_cookie()

class ProxyProcessor():
    @staticmethod
    def get(web_req):
        pass
    @staticmethod 
    def post(web_req):
        pass    

class LoginProcessor(ProxyProcessor):
    @staticmethod
    def get(web_req):
        return ErrorResponse(ErrorResponse.UNKNOWN_ACTION)
    @staticmethod
    def post(web_req):
        user = web_req.get_argument("username")
        passwd = web_req.get_argument("password")
        access_id='L52ISGKFHSEXSPOZYIZ1K'
        secret_key='YRRpiyw333aq1se5PneZEnskI9MMNXrSoojoJjat'
        # create session and store info there, set session id in cookie
        cookie = os.urandom(16).encode('hex');
        web_req.set_cookie("session-id", cookie);
        sessions[cookie] = UserSession(user, passwd, access_id, secret_key)
        return LoginResponse(user)

class SessionProcessor(ProxyProcessor):
    @staticmethod
    def post(web_req):
        session_id = web_req.get_cookie("session-id")
        if not(session_id):
	    raise "session id not found"
        session = sessions[session_id]
        if not(session):
	    raise "session not found"
        user = session.username
	return LoginResponse(user)

class ProxyResponse(object):
    RESPONSE_TYPE_AUTH = 0
    RESPONSE_TYPE_INFO = 1
    RESPONSE_TYPE_OP = 2

    def __init__(self, resp_type):
        self.response_type = resp_type

    def get_response(self):
        pass

    @staticmethod
    def generate(response):
        if response.response_type == ProxyResponse.RESPONSE_TYPE_AUTH:
            return {'type':'auth', 'data':response.get_response()} 
        elif response.response_type == ProxyResponse.RESPONSE_TYPE_INFO:
            return {'type':'info', 'data':response.get_response()}
        elif response.response_type == ProxyResponse.RESPONSE_TYPE_OP:
            return {'type':'op', 'data':response.get_response()}

class LoginResponse(ProxyResponse):
    def __init__(self, user):
        super(LoginResponse,self).__init__(ProxyResponse.RESPONSE_TYPE_AUTH)
        self.user = user
    
    def get_response(self):
        context = ContextHelper(self.user)
        text_data =  {'footer':data_helper.get_data('text','footer')}
        #img_data = {'logo':data_helper.get_data('image','logo')}
        img_data = {}
        for n,v in data_helper.list_items('image'):
	    img_data[n] = v 

        return {'context':context.get_context(), 'text':text_data, 'image':img_data}

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

    def get_explorers(self):
        return ['dashboard','images','instances','storage','netsec','support']

    def get_context(self):
        return {'username':self.username, 'fullname':self.get_full_name(), 'timezone':self.get_time_zone(), 'language':self.get_language(), 'url_home':self.get_url_home(), 'explorers':self.get_explorers()}

# this class abstracts the customizable text sent to the browser 
class DataHelper():
    def __init__(self):
        data_path = config.get('eui', 'datapath')
        self.data_config = ConfigParser.ConfigParser()
	if len(self.data_config.read(data_path)) != 1:
	    raise Exception("Cannot read the data file: %s" % data_path)

    def get_data(self, scope, key):
        return self.data_config.get(scope, key)

    def list_items(self, scope):
        return self.data_config.items(scope);

