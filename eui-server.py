import tornado.ioloop
import tornado.web
import os
import socket
import random
import boto
import json
from json import JSONEncoder
from boto.ec2 import EC2Connection
from boto.ec2.ec2object import EC2Object
from boto.regioninfo import RegionInfo
from boto.ec2.instance import Group
from boto.ec2.volume import AttachmentSet

# dictionary for in-memory sessions
sessions = {};

class UserSession(object):
  def __init__(self, username, password, access_id, secret_key):
    self.username = username
    self.password = password
    self.access_id = access_id
    self.secret_key = secret_key

class BotoEncoder(JSONEncoder):
  def default(self, obj):
    if isinstance(obj, RegionInfo):
      return (obj.__dict__)
    if isinstance(obj, EC2Connection):
      return []
    elif issubclass(obj.__class__, EC2Object):
      return (obj.__dict__)
    elif issubclass(obj.__class__, Group):
      return (obj.__dict__)
    elif issubclass(obj.__class__, AttachmentSet):
      return (obj.__dict__)
    return super(BotoEncoder, self).default(obj)


class BaseHandler(tornado.web.RequestHandler):
    def get_current_user(self):
        return self.get_secure_cookie("user")

class EC2Handler(BaseHandler):
    @tornado.web.authenticated
    
    def get(self):
#        print "Hello, " + self.current_user
        endpoint='http://192.168.25.10:8773/services/Eucalyptus'
        session_id = self.get_cookie("session-id")
        if not(session_id):
          self.redirect("/login")
        try:
          session = sessions[session_id]
        except KeyError:
          self.redirect("/login")
        conn = boto.connect_euca(host='192.168.25.10', aws_access_key_id=session.access_id, aws_secret_access_key=session.secret_key)
        data_type = self.get_argument("type")
        obj = []
        if data_type == "image":
          obj = conn.get_all_images()
        if data_type == "instance":
          obj = conn.get_all_instances()
        elif data_type == "address":
          obj = conn.get_all_addresses()
        elif data_type == "key":
          obj = conn.get_all_key_pairs()
        elif data_type == "group":
          obj = conn.get_all_security_groups()
        elif data_type == "volume":
          obj = conn.get_all_volumes()
        elif data_type == "snapshot":
          obj = conn.get_all_snapshots()
        self.write(json.dumps(obj, cls=BotoEncoder, indent=2))

class LoginHandler(BaseHandler):
    def get(self):
        # ensure session is cleared
        self.set_cookie("session-id", "")
        # could redirect to login page...
        self.write('<html><body><form action="/login" method="post">'
                   'Name: <input type="text" name="name"><br/>'
                   'Password: <input type="text" name="passwd"><br/>'
                   '<input type="submit" value="Sign in">'
                   '</form></body></html>')

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
        self.redirect("/static/eui.html")

settings = {
  "static_path": os.path.join(os.path.dirname(__file__), "."),
  "cookie_secret": "YzRmYThkNzU1NDU2NmE1ZjYxMDZiZDNmMzI4YmMzMmMK",
  "login_url": "/login",
}

application = tornado.web.Application([
    (r"/ec2", EC2Handler),
    (r"/login", LoginHandler),
], **settings)

if __name__ == "__main__":
#    (hostname, alt_host, ipaddrs) = socket.gethostbyaddr(socket.gethostname())
#    for ip in ipaddrs:
#      print "host IP: "+ip
    application.listen(8888)
    tornado.ioloop.IOLoop.instance().start()


