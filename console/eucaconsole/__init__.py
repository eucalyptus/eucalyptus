# Copyright 2012 Eucalyptus Systems, Inc.
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the following
# conditions are met:
#
#   Redistributions of source code must retain the above copyright notice,
#   this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer in the
#   documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import base64
import binascii
import boto
import ConfigParser
import io
import json
import os
import random
import sys
import time
import tornado.web
import traceback
import socket
import logging
import uuid
import urllib
import urllib2
from datetime import datetime
from datetime import timedelta

from boto.sts.credentials import Credentials
from .botoclcinterface import BotoClcInterface
from token import TokenAuthenticator

try:
    from .version import __version__
except:
    __version__ = 'DEVELOPMENT'

sessions = {}
config = None
global_session = None
using_ssl = False

class UserSession(object):
    clc = None
    walrus = None
    cw = None
    elb = None
    scaling = None
    def __init__(self, account, username, session_token, access_key, secret_key):
        self.obj_account = account
        self.obj_username = username
        self.obj_session_token = session_token
        self.obj_access_key = access_key
        self.obj_secret_key = secret_key
        self.obj_fullname = None
        self.session_start = time.time()
        self.session_last_used = time.time()
        self.session_lifetime_requests = 0
        self.keypair_cache = {}

    def cleanup(self):
        # this is for cleaning up resources, like when the session is ended
        for res in self.clc.caches:
            self.clc.caches[res].cancelTimer()
        for res in self.cw.caches:
            self.cw.caches[res].cancelTimer()
        for res in self.elb.caches:
            self.elb.caches[res].cancelTimer()
        for res in self.scaling.caches:
            self.scaling.caches[res].cancelTimer()

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
    def host_override(self):
        return self.obj_host_override

    @host_override.setter
    def host_override(self, val):
        self.obj_host_override = val

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
        self.instancetypes = ""

    def get_value(self, scope, key, default_val = None):
        value = None
        try:
          value = config.get(scope,key)
        except Exception, err:
          value = default_val
        return value

    def list_items(self, scope):
        items = {}
        for k, v in config.items(scope):
            items[k] = v
        return items

    def parse_instancetypes(self, instancetypes):
        self.instancetypes = {}
        for vmt in instancetypes:
            if isinstance(vmt, dict):
                self.instancetypes[vmt['name']] = [vmt['cores'], vmt['memory'], vmt['disk']]
            else:
                self.instancetypes[vmt.name] = [vmt.cores, vmt.memory, vmt.disk]

    @property
    def language(self):
        return self.get_value('locale', 'language')

    @property
    def version(self):
        return __version__
    
    @property
    def admin_console_url(self):
        port = self.get_value('server', 'clcwebport', '8443')
        url = 'https://' + self.get_value('server', 'clchost')
        if port != '443':
            url += ':' + port
        return url

    @property
    def help_url(self):
        return self.get_value('locale', 'help.url')

    @property
    def admin_support_url(self):
        return self.get_value('locale', 'support.url')

    @property
    def instance_type(self):
        return self.instancetypes

    @property
    def ajax_timeout(self):
        timeout = self.get_value('server', 'ajax.timeout', '30000')
        return timeout

    # return the collection of global session info
    def get_session(self):
        return {
                'version': self.version,
                'language': self.language,
                'admin_console_url': self.admin_console_url,
                'help_url': self.help_url,
                'admin_support_url' : self.admin_support_url,
                'instance_type': self.instance_type,
                'ajax_timeout': self.ajax_timeout,
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
            self.clear_cookie("session-id")
            self.clear_cookie("_xsrf")
            return False
        self.user_session = sessions[sid]
        return True

    # this method is overriden from RequestHandler to set a secure cookie with https
    @property
    def xsrf_token(self):
        if not hasattr(self, "_xsrf_token"):
            token = self.get_cookie("_xsrf")
            if not token:
                token = binascii.b2a_hex(uuid.uuid4().bytes)
                expires_days = 30 if self.current_user else None
                if using_ssl:
                    self.set_cookie("_xsrf", token, expires_days=expires_days, secure='yes')
                else:
                    self.set_cookie("_xsrf", token, expires_days=expires_days)
            self._xsrf_token = token
        return self._xsrf_token

class RootHandler(BaseHandler):
    def get(self, path):
        try:
            action = self.get_argument("action", default='')
            print "root action = "+action
            if (action == 'awslogin'):
                access_token = self.get_argument("access_token")
                print "access token = "+access_token
                req = urllib2.Request("https://api.amazon.com/auth/o2/tokeninfo?access_token=" + urllib.quote_plus(access_token))
                print "requesting token auth"
                response = urllib2.urlopen(req, timeout=15)
                body = response.read()
                print "here's the response from the token auth:"+body
                token_info = json.loads(body)
                 
                # compare to client id
                # TODO: Get from config file!
                if token_info['aud'] != 'amzn1.application-oa2-client.02dc0d9e787e49359fde3cf87cee14d9' :
                    # the access token does not belong to us
                    raise BaseException("Invalid Token")
                 
                print "requesting user profile"
                req = urllib2.Request("https://api.amazon.com/user/profile")
                req.add_header("Authorization", "bearer " + access_token)
                response = urllib2.urlopen(req, timeout=15)
                body = response.read()
                print "here's the response for user profile:"+body
                profile = json.loads(body)
                print "%s %s %s"%(profile['name'], profile['email'], profile['user_id'])
                account = profile['user_id']
                user = profile['email']

                role_arn='arn:aws:iam::365812321051:role/authRole'
                role_session_name='testing'

                url = 'https://sts.amazonaws.com?Action=AssumeRoleWithWebIdentity'
                url = url + '&DurationSeconds=3600'
                url = url + '&ProviderId=www.amazon.com'
                url = url + '&RoleSessionName=' + role_session_name
                url = url + '&Version=2011-06-15'
                url = url + '&RoleArn=' + urllib.quote(role_arn)
                url = url + '&WebIdentityToken=' + urllib.quote(access_token)

                logging.info("sts request = "+url)
                request = urllib2.Request(url, headers= {'Accept' : 'application/json'} )
                response = urllib2.urlopen(request)
                assumedRole = response.read() 
                logging.info("assumed role = "+assumedRole)
                assumedRole = json.loads(assumedRole)
                assumedRole = assumedRole['AssumeRoleWithWebIdentityResponse']['AssumeRoleWithWebIdentityResult']
                
                logging.info("here's the response for AssumeRoleWithWebIdentity:\n- AssumedRole.user: %s\n- AssumedRole.credentials: %s"%(assumedRole['AssumedRoleUser'],assumedRole['Credentials']))
                session_token = assumedRole['Credentials']['SessionToken']
                access_id = assumedRole['Credentials']['AccessKeyId']
                secret_key = assumedRole['Credentials']['SecretAccessKey']
                while True:
                    sid = os.urandom(16).encode('hex')
                    if sid in sessions:
                        continue
                    break
                if using_ssl:
                    self.set_cookie("session-id", sid, secure='yes')
                else:
                    self.set_cookie("session-id", sid)
                #expiration = datetime.now() + timedelta(days=180)
                #self.set_cookie("account", account, expires=expiration)
                #self.set_cookie("username", user, expires=expiration)
                #self.set_cookie("remember", 'true', expires=expiration)
                    
                sessions[sid] = UserSession(account, user, session_token, access_id, secret_key)
                sessions[sid].host_override = 'ec2.us-east-1.amazonaws.com'
                # need to get user back to our app since aws callback took us off-page
                self.redirect('/', False, 303);
                return
            else:
                path = os.path.join(config.get('paths', 'staticpath'), "index.html")
        except ConfigParser.Error:
            logging.info("Caught url path exception :"+path)
            path = '../static/index.html'
        self.set_header("X-Frame-Options", "DENY")
        self.set_header("Cache-control", "no-cache")
        self.set_header("Pragma", "no-cache")
        self.render(path)

    def post(self, arg):
        action = self.get_argument("action")
        response = None
        try:
            if action == 'login' or action == 'changepwd':
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
            elif action == 'busy':
                if self.authorized():
                    self.user_session.session_last_used = time.time()
                    response = BusyResponse(self.user_session)
                else:
                    response = BusyResponse(None)
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
        if action == 'login' or action == 'init' or action == 'changepwd':
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
        terminateSession(sid)
        web_req.clear_cookie("session-id")
        web_req.clear_cookie("_xsrf")
        return LogoutResponse();

def terminateSession(id, expired=False):
    msg = 'logged out'
    if expired:
        msg = 'session timed out'
    logging.info("User %s after %d seconds" % (msg, (time.time() - sessions[id].session_start)));
    logging.info("--Proxy processed %d requests during this session", sessions[id].session_lifetime_requests)
    sessions[id].cleanup()
    del sessions[id] # clean up session info

class LoginProcessor(ProxyProcessor):
    @staticmethod
    def post(web_req):
        action = web_req.get_argument("action")
        auth_hdr = web_req.get_argument('Authorization')
        if not auth_hdr:
            raise NotImplementedError("auth header not found")
        auth_decoded = base64.decodestring(auth_hdr)
        newpwd = None
        if action == 'changepwd':
            # fetch/decode old/new passwords
            account, user, passwd, newpwd = auth_decoded.split(':', 3)
            passwd = base64.decodestring(passwd)
            newpwd = base64.decodestring(newpwd)
            remember = 'yes' if (web_req.get_cookie("remember") == 'true') else 'no';
        else:
            account, user, passwd = auth_decoded.split(':', 2);
            remember = web_req.get_argument("remember")

        if config.getboolean('test', 'usemock') == False:
            auth = TokenAuthenticator(config.get('server', 'clchost'),
                            config.getint('server', 'session.abs.timeout')+60)
            creds = auth.authenticate(account, user, passwd, newpwd)
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
        if using_ssl:
            web_req.set_cookie("session-id", sid, secure='yes')
        else:
            web_req.set_cookie("session-id", sid)
        if remember == 'yes':
            expiration = datetime.now() + timedelta(days=180)
            web_req.set_cookie("account", account, expires=expiration)
            web_req.set_cookie("username", user, expires=expiration)
            web_req.set_cookie("remember", 'true' if remember else 'false', expires=expiration)
        else:
            web_req.clear_cookie("account")
            web_req.clear_cookie("username")
            web_req.clear_cookie("remember")
        sessions[sid] = UserSession(account, user, session_token, access_id, secret_key)
        sessions[sid].host_override = None

        return LoginResponse(sessions[sid])

class InitProcessor(ProxyProcessor):
    @staticmethod
    def post(web_req):
        language = config.get('locale','language')
        support_url = config.get('locale','support.url')
        port = '8443'
        try:
          port = config.get('server', 'clcwebport')
        except Exception, err:
          pass
        aws_enabled = False
        aws_client_id = ''
        try:
          aws_enabled = config.get('aws', 'enableAWS')
          aws_client_id = config.get('aws', 'client.id')
        except Exception, err:
          pass
        admin_url = 'https://' + config.get('server', 'clchost')
        if port != '443':
            admin_url += ':' + port
        url_rewrite = config.get('server', 'url.rewrite')
        if web_req.get_argument('host', False) and (url_rewrite in ['true', '1', 'True']): 
          try:
            host = web_req.get_argument('host')
            ip_list = socket.getaddrinfo(host, 0, 0, 0, socket.SOL_TCP)
            for addr in ip_list:
              ip_str = (addr[4])[0]; 
              ip = ip_str.split('.');
              if (len(ip) == 4):
                return InitResponse(language, support_url, ip_str, host, aws_enabled=aws_enabled, aws_client_id=aws_client_id)
            raise Exception
          except:
            return InitResponse(language, support_url, admin_url, aws_enabled=aws_enabled, aws_client_id=aws_client_id)
        else:
          return InitResponse(language, support_url, admin_url, aws_enabled=aws_enabled, aws_client_id=aws_client_id)

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

        instancetypes = []
        use_mock = config.getboolean('test', 'usemock')
        if self.user_session.host_override or use_mock:
            instancetypes.append(dict(name='t1.micro', cores='1', memory='256', disk='5'))
            instancetypes.append(dict(name='m1.small', cores='1', memory='256', disk='5'))
            instancetypes.append(dict(name='m1.medium', cores='1', memory='512', disk='10'))
            instancetypes.append(dict(name='m1.large', cores='2', memory='512', disk='10'))
            instancetypes.append(dict(name='c1.medium', cores='2', memory='512', disk='10'))
            instancetypes.append(dict(name='m1.xlarge', cores='2', memory='1024', disk='10'))
            instancetypes.append(dict(name='c1.xlarge', cores='2', memory='2048', disk='10'))
            instancetypes.append(dict(name='m2.xlarge', cores='2', memory='2048', disk='10'))
            instancetypes.append(dict(name='m3.xlarge', cores='4', memory='2048', disk='15'))
            instancetypes.append(dict(name='m3.2xlarge', cores='4', memory='4096', disk='30'))
            instancetypes.append(dict(name='m2.4xlarge', cores='8', memory='4096', disk='60'))
            instancetypes.append(dict(name='hi1.4xlarge', cores='8', memory='6144', disk='120'))
            instancetypes.append(dict(name='cc2.8xlarge', cores='16', memory='6144', disk='120'))
            instancetypes.append(dict(name='cg1.4xlarge', cores='16', memory='12288', disk='200'))
            instancetypes.append(dict(name='cr1.8xlarge', cores='16', memory='16384', disk='240'))
            instancetypes.append(dict(name='hs1.8xlarge', cores='48', memory='119808', disk='24000'))
        else:
            #boto.set_stream_logger('foo')
            host = config.get('server', 'clchost')
            clc = BotoClcInterface(host, self.user_session.access_key,
                                   self.user_session.secret_key,
                                   self.user_session.session_token, debug=0)
            instancetypes = clc.get_all_instancetypes()
        global_session.parse_instancetypes(instancetypes)

        return {'global_session': global_session.get_session(),
                'user_session': self.user_session.get_session()}

class BusyResponse(ProxyResponse):
    def __init__(self, session):
        self.user_session = session
    def get_response(self):
        if self.user_session:
            return {'result': 'true'}
        else:
            return {'result': 'false'}

class InitResponse(ProxyResponse):
    def __init__(self, lang, support_url, admin_url, ip='', hostname='', aws_enabled=False, aws_client_id=''):
        self.language = lang
        self.support_url = support_url
        self.admin_url = admin_url
        self.ip = ip
        self.hostname = hostname
        self.aws_login_enabled = aws_enabled
        self.aws_client_id = aws_client_id

    def get_response(self):
        return {'language': self.language, 'support_url': self.support_url, 'admin_url': self.admin_url, 'ipaddr': self.ip, 'hostname': self.hostname, 'aws_login_enabled': 'true' if self.aws_login_enabled else 'false', 'aws_client_id': self.aws_client_id}
