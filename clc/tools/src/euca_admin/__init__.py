import boto, os, sys, re
from boto.ec2.regioninfo import RegionInfo
from urlparse import urlparse
        
class EucaAdmin:
  def __init__(self,path='/services/Eucalyptus'):
    if not 'EC2_ACCESS_KEY' in os.environ:
      print 'Environment variable EC2_ACCESS_KEY is unset.'
      sys.exit(1)
    if not 'EC2_SECRET_KEY' in os.environ:
      print 'Environment variable EC2_SECRET_KEY is unset.'
      sys.exit(1)
    if not 'EC2_URL' in os.environ:
      print 'Environment variable EC2_URL is unset.'
      sys.exit(1)
    self.path = path
    self.region='eucalyptus'
    self.access_key = os.getenv('EC2_ACCESS_KEY')
    self.secret_key = os.getenv('EC2_SECRET_KEY')
    self.url = os.getenv('EC2_URL')
    self.parsed = urlparse(self.url)
    self.connection = boto.connect_ec2(aws_access_key_id=self.access_key, 
                                 aws_secret_access_key=self.secret_key, 
                                 is_secure=False, 
                                 region=RegionInfo(None, 'eucalyptus', self.parsed.hostname),
                                 port=8773,
                                 path=self.path)
    self.connection.APIVersion = 'eucalyptus'
    
  def get_connection(self):
    return self.conn

  def handle_error(self,ex):
    s = ""
    if ex.errors.__len__() != 0:
      for i in ex.errors:
        s = '%sERROR %s %s %s: %s\n' % (s,ex.status, ex.reason, i[0], i[1])
    else:
      s = 'ERROR %s %s %s' % (ex.status, ex.reason, ex)
    if not re.match(".*Exception.*\n",s) == None:
      while s.count("\n") != 3:
        s = re.sub(".*Exception.*\n", ": ", s)
    print s.replace("\n","")
    sys.exit(1)
    

