import base64
import boto
import logging
import sys
import socket
import traceback
import urllib2
import xml.sax

from boto.sts.credentials import Credentials
import server

class TokenAuthenticator(object):

    def __init__(self, host, duration):
        # make the call to STS service to authenticate with the CLC
        self.auth_url = "https://%s:8773/services/Tokens?Action=GetSessionToken&DurationSeconds=%d&Version=2011-06-15" % (host, duration)

    # raises EuiExcepiton for "Noth Authorized" or "Timed out"
    def authenticate(self, account, user, passwd):
        try:
            req = urllib2.Request(self.auth_url)
            auth_string = "%s@%s:%s" % \
                            (base64.b64encode(user), \
                            base64.b64encode(account), \
                            passwd)
            encoded_auth = base64.b64encode(auth_string)
            req.add_header('Authorization', "Basic %s" % encoded_auth)
            response = urllib2.urlopen(req, timeout=15)
            body = response.read()

            # parse AccessKeyId, SecretAccessKey and SessionToken
            creds = Credentials(None)
            h = boto.handler.XmlHandler(creds, None)
            xml.sax.parseString(body, h)
            logging.info("authenticated user: "+account+"/"+user)
            return creds
        except urllib2.URLError, err:
            traceback.print_exc(file=sys.stdout)
            if not(issubclass(err.__class__, urllib2.HTTPError)):
                if isinstance(err.reason, socket.timeout):
                    raise server.EuiException(504, 'Timed out')
            raise server.EuiException(401, 'Not Authorized')


