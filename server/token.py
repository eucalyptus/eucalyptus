import base64
import boto
import sys
import traceback
import urllib2
import xml.sax

from boto.sts.credentials import Credentials

class TokenAuthenticator(object):

    def __init__(self, host, duration):
        # make the call to STS service to authenticate with the CLC
        self.auth_url = "http://%s:8773/services/Tokens?Action=GetSessionToken&DurationSeconds=%d&Version=2011-06-15" % (host, duration)

    def authenticate(self, account, user, passwd):
        try:
            req = urllib2.Request(self.auth_url)
            auth_string = "%s@%s:%s" % \
                            (base64.b64encode(user), \
                            base64.b64encode(account), \
                            passwd)
            encoded_auth = base64.b64encode(auth_string.encode('utf8'))
            req.add_header('Authorization', "Basic %s" % encoded_auth)
            response = urllib2.urlopen(req)
            body = response.read()

            # parse AccessKeyId, SecretAccessKey and SessionToken
            creds = Credentials(None)
            h = boto.handler.XmlHandler(creds, None)
            xml.sax.parseString(body, h)
            return creds
        except urllib2.URLError, err:
            print err
            traceback.print_exc(file=sys.stdout)
            return None


