import tornado.web
import os
import io
import socket
import random
import json
import sys
import traceback
import base64
import ConfigParser
from botoclcinterface import BotoClcInterface
from mockclcinterface import MockClcInterface

sessions = {}
use_mock = True
config = None
global_session = None

class UserSession(object):
    def __init__(self, username, session_token, access_key, secret_key):
        self.obj_username = username
        self.obj_session_token = session_token
        self.obj_access_key = access_key
        self.obj_secret_key = secret_key
        self.obj_fullname = None

    @property
    def username(self):
        return self.obj_username

    @property
    def session_token(self):
        return self.obj_session_token
 
    @property
    def access_key(self):
        return self.obj_access_key

    @property
    def secret_key(self):
        return self.obj_secret_key

    @property
    def fullname(self):
        return self.obj_fullname
  
    @fullname.setter
    def fullname(self, name):
        self.obj_fullname = name 

    # return only the info that's to be sent to browsers
    def get_session(self):
        return {'username':self.username, 'fullname':self.fullname}

class GlobalSession(object):
    def __init__(self):
        data_path = config.get('eui', 'datapath')
        self.data_config = ConfigParser.ConfigParser()
        if len(self.data_config.read(data_path)) != 1:
            raise Exception("Cannot read the data file: %s" % data_path)

    def get_value(self, scope, key):
        return self.data_config.get(scope, key) 

    def list_items(self, scope):
        items={}
        for k,v in self.data_config.items(scope):
            items[k] = v
        return items

    @property
    def timezone(self):
        return self.get_value('locale','timezone')
 
    @property
    def language(self):
        return self.get_value('locale','language') 

    @property
    def images(self):
        return self.list_items('image')

    @property
    def texts(self):
        return self.list_items('text')

    @property
    def navigation_menus(self):
        return [k for k,v in self.data_config.items('navigation')]

    @property
    def navigation_submenus(self):
        return self.list_items('navigation')     

    @property
    def help_texts(self):
        return self.list_items('help') 

    # return the collection of global session info 
    def get_session(self):
        return {'timezone':self.timezone,
                'language':self.language, 
                'navigation_menus':self.navigation_menus, 
                'navigation_submenus':self.navigation_submenus,
                'texts':self.texts,
                'images':self.images,
                'help_texts':self.help_texts} 

class EuiException(BaseException):
    def __init__(self, status_code, message):
        self.code = status_code
        self.msg = message

    @property
    def status_code(self):
        return self.code

    @status_code.setter
    def status_code(self, code):
        self.code = code

    @property
    def message(self):
        return self.msg

    @message.setter
    def message(self, msg):
        self.msg = msg

class BaseHandler(tornado.web.RequestHandler):
    def should_use_mock(self):
        use_mock = config.getboolean('eui', 'usemock')
        return use_mock

    def authorized(self):
        try:
            sid = self.get_cookie("session-id")
        except:
            return False
        
        if not sid or not sessions.has_key(sid):
            return False
        self.user_session = sessions[sid]
        return True     

class RootHandler(BaseHandler):
    def get(self, path):
        path = config.get('eui', 'staticpath')+"eui.html"
        self.render(path)

    def post(self, arg):
        action = self.get_argument("action")
        response=None
        try:
            if action == 'login':
                try:
                    response=LoginProcessor.post(self)
                except Exception, err:
                    traceback.print_exc(file=sys.stdout)
                    raise EuiException(401, 'not authorized')
            else:
	        if not self.authorized():
                    raise EuiException(401, 'not authorized')
   
                if action == 'session':
	            try:
	                response=SessionProcessor.post(self)
	            except Exception, err:
                        traceback.print_exc(file=sys.stdout)
                        raise EuiException(500, 'can\'t retrieve session info')
                else:
                    raise EuiException(500, 'unknown action')
        except EuiException, err:  
            if err:
                raise tornado.web.HTTPError(err.status_code, err.message)
            else:
                raise tornado.web.HTTPError(500, 'unknown error occured')

        if not response:
            raise tornado.web.HTTPError(500, 'unknown error occured')

        self.write(response.get_response())

    def check_xsrf_cookie(self):
        action = self.get_argument("action")
	if action == 'login':
            xsrf=self.xsrf_token
        else:
            super(RootHandler, self).check_xsrf_cookie()

class ProxyProcessor():
    @staticmethod
    def get(web_req):
        raise "not supported"

    @staticmethod 
    def post(web_req):
        raise "not supported"

class LoginProcessor(ProxyProcessor):
    @staticmethod
    def post(web_req):
        auth_hdr = web_req.request.headers.get('Authorization')
        if not auth_hdr:
            raise "auth header not found"
        if not auth_hdr.startswith('Basic '):
            raise "auth header in wrong format"
        auth_decoded = base64.decodestring(auth_hdr[6:])
        user, passwd = auth_decoded.split(':',2)

        #hardcoded temporarily
        session_token='PLACEHOLDER'
        access_id='L52ISGKFHSEXSPOZYIZ1K'
        secret_key='YRRpiyw333aq1se5PneZEnskI9MMNXrSoojoJjat'

        # create session and store info there, set session id in cookie
        while 1: 
          sid = os.urandom(16).encode('hex');
          if sessions.has_key(sid):
              continue
          break
        web_req.set_cookie("session-id", sid);
        sessions[sid] = UserSession(user, session_token, access_id, secret_key)

        return LoginResponse(sessions[sid])

class SessionProcessor(ProxyProcessor):
    @staticmethod
    def post(web_req):
	return LoginResponse(web_req.user_session)

class LoginResponse(object):
    def __init__(self, session):
        self.user_session = session
     
    def get_response(self):
        global global_session
        if not global_session:
            global_session = GlobalSession() 

        return {'global_session':global_session.get_session(), 'user_session':self.user_session.get_session()}

