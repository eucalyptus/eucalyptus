import base64
import ConfigParser
import io
import json
import os
import random
import sys
import tornado.web
import traceback
import socket
import logging

from token import TokenAuthenticator

sessions = {}
use_mock = True
config = None
global_session = None


class UserSession(object):
    clc = None
    def __init__(self, account, username, session_token, access_key, secret_key):
        self.obj_account = account
        self.obj_username = username
        self.obj_session_token = session_token
        self.obj_access_key = access_key
        self.obj_secret_key = secret_key
        self.obj_fullname = None

    @property
    def account(self):
        return self.obj_account

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
        return {'account':self.account, 'username': self.username, 'fullname': self.fullname}

class GlobalSession(object):
    def __init__(self):
        pass
    def get_value(self, scope, key):
        return config.get(scope, key)

    def list_items(self, scope):
        items = {}
        for k, v in config.items(scope):
            items[k] = v
        return items

    @property
    def language(self):
        return self.get_value('locale', 'language')

    @property
    def email(self):
        return self.get_value('locale', 'email')

    @property
    def instance_type(self):
        '''
        m1.small:1 128 2
        c1.medium: 2 128 5 74
        ... 
        '''
        m1_small = [self.get_value('instance_type','m1.small.cpu'),self.get_value('instance_type','m1.small.mem'),self.get_value('instance_type','m1.small.disk')]; 
        c1_medium = [self.get_value('instance_type','c1.medium.cpu'),self.get_value('instance_type','c1.medium.mem'),self.get_value('instance_type','c1.medium.disk')]; 
        m1_large = [self.get_value('instance_type','m1.large.cpu'),self.get_value('instance_type','m1.large.mem'),self.get_value('instance_type','m1.large.disk')];
        m1_xlarge = [self.get_value('instance_type','m1.xlarge.cpu'),self.get_value('instance_type','m1.xlarge.mem'),self.get_value('instance_type','m1.xlarge.disk')]; 
        c1_xlarge = [self.get_value('instance_type','c1.xlarge.cpu'),self.get_value('instance_type','c1.xlarge.mem'),self.get_value('instance_type','c1.xlarge.disk')]; 
        return {'m1.small':m1_small, 'c1.medium':c1_medium, 'm1.large':m1_large, 'm1.xlarge':m1_xlarge, 'c1.xlarge':c1_xlarge};

    # return the collection of global session info
    def get_session(self):
        return {'language': self.language,
                'email' : self.email,
                'instance_type': self.instance_type 
               }

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

class CheckIpHandler(tornado.web.RequestHandler):
    def get(self):
        remote = self.request.remote_ip
        if remote == '::1':
            remote = '127.0.0.1'
        self.write(remote)

class BaseHandler(tornado.web.RequestHandler):
    user_session = None
    def should_use_mock(self):
        use_mock = config.getboolean('test', 'usemock')
        return use_mock

    def authorized(self):
        try:
            sid = self.get_cookie("session-id")
        except:
            return False

        if not sid or sid not in sessions:
            return False
        self.user_session = sessions[sid]
        return True

class RootHandler(BaseHandler):
    def get(self, path):
        try:
            path = os.path.join(config.get('paths', 'staticpath'), "index.html")
        except ConfigParser.Error:
            print "Caught exception"
            path = '../static/index.html'
        self.render(path)

    def post(self, arg):
        action = self.get_argument("action")
        response = None
        try:
            if action == 'login':
                try:
                    response = LoginProcessor.post(self)
                except Exception, err:
                    traceback.print_exc(file=sys.stdout)
                    if isinstance(err, EuiException):
                        raise err
                    else:
                        raise EuiException(401, 'not authorized')
            elif action == 'init':
                try:
                    response = InitProcessor.post(self)
                except Exception, err:
                    traceback.print_exc(file=sys.stdout)
                    raise EuiException(401, 'not authorized')
            else:
                if not self.authorized():
                    raise EuiException(401, 'not authorized')

                if action == 'session':
                    try:
                        response = SessionProcessor.post(self)
                    except Exception, err:
                        traceback.print_exc(file=sys.stdout)
                        raise EuiException(500, 'can\'t retrieve session info')
                elif action == 'logout':
                  try:
                      response = LogoutProcessor.post(self)
                  except Exception, err:
                      traceback.print_exc(file=sys.stdout)
                      raise EuiException(500, 'unknown error occured')
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
        if action == 'login' or action == 'init':
            xsrf = self.xsrf_token
        else:
            super(RootHandler, self).check_xsrf_cookie()

class ProxyProcessor():
    @staticmethod
    def get(web_req):
        raise NotImplementedError("not supported")

    @staticmethod
    def post(web_req):
        raise NotImplementedError("not supported")

class LogoutProcessor(ProxyProcessor):
    @staticmethod
    def post(web_req):
        sid = web_req.get_cookie("session-id")
        if not sid or sid not in sessions:
            return LogoutResponse();
        del sessions[sid] # clean up session info
        logging.info("Cleared session (%s)" % sid);
        return LogoutResponse();


class LoginProcessor(ProxyProcessor):
    @staticmethod
    def post(web_req):
        auth_hdr = web_req.request.headers.get('Authorization')
        if not auth_hdr:
            raise NotImplementedError("auth header not found")
        if not auth_hdr.startswith('Basic '):
            raise NotImplementedError("auth header in wrong format")
        auth_decoded = base64.decodestring(auth_hdr[6:])
        account, user, passwd = auth_decoded.split(':', 3)
        remember = web_req.get_argument("remember")

        if config.getboolean('test', 'usemock') == False:
            auth = TokenAuthenticator(config.get('server', 'clchost'), 3600)
            creds = auth.authenticate(account, user, passwd)
            session_token = creds.session_token
            access_id = creds.access_key
            secret_key = creds.secret_key
        else:
            # assign bogus values so we never mistake them for the real thing (who knows?)
            session_token = "Larry"
            access_id = "Moe"
            secret_key = "Curly"

        # create session and store info there, set session id in cookie
        while True:
            sid = os.urandom(16).encode('hex')
            if sid in sessions:
                continue
            break
        web_req.set_cookie("session-id", sid)
        if remember == 'yes':
            web_req.set_cookie("account", account)
            web_req.set_cookie("username", user)
            web_req.set_cookie("remember", 'true' if remember else 'false')
        else:
            web_req.clear_cookie("account")
            web_req.clear_cookie("username")
            web_req.clear_cookie("remember")
        sessions[sid] = UserSession(account, user, session_token, access_id, secret_key)

        return LoginResponse(sessions[sid])

class InitProcessor(ProxyProcessor):
    @staticmethod
    def post(web_req):
        language = config.get('locale','language')
        email = config.get('locale','email')
        if web_req.get_argument('host', False): 
          try:
            host = web_req.get_argument('host')
            ip_list = socket.getaddrinfo(host, 0, 0, 0, socket.SOL_TCP)
            for addr in ip_list:
              ip_str = (addr[4])[0]; 
              ip = ip_str.split('.');
              if (len(ip) == 4):
                return InitResponse(language, email, ip_str, host)
            raise Exception
          except:
            return InitResponse(language, email)
        else:
          return InitResponse(language, email)

class SessionProcessor(ProxyProcessor):
    @staticmethod
    def post(web_req):
        return LoginResponse(web_req.user_session)

class ProxyResponse(object):
    def __init__(self):
        pass

    def get_response(self):
        raise NotImplementedError( "Should have implemented this" )

class LogoutResponse(ProxyResponse):
    def __init__(self):
        pass
    def get_response(self):
        return {'result': 'success'}

class LoginResponse(ProxyResponse):
    def __init__(self, session):
        self.user_session = session

    def get_response(self):
        global global_session
        if not global_session:
            global_session = GlobalSession()

        return {'global_session': global_session.get_session(),
                'user_session': self.user_session.get_session()}

class InitResponse(ProxyResponse):
    def __init__(self, lang, email, ip='', hostname=''):
        self.language = lang
        self.email = email
        self.ip = ip
        self.hostname = hostname

    def get_response(self):
        return {'language': self.language, 'email': self.email, 'ipaddr': self.ip, 'hostname': self.hostname}
