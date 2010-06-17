import boto,sys,euca_admin
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin import EucaAdmin
from optparse import OptionParser

SERVICE_PATH = '/services/Configuration'
class Walrus():
  
  
  def __init__(self, walrus_name=None, host_name=None, port=None):
    self.walrus_name = walrus_name
    self.host_name = host_name
    self.euca = EucaAdmin(path=SERVICE_PATH)

          
  def __repr__(self):
      return 'WALRUS\t%s\t%s' % (self.walrus_name, self.host_name) 

  def startElement(self, name, attrs, connection):
      return None

  def endElement(self, name, value, connection):
    if name == 'euca:detail':
      self.host_name = value
    elif name == 'euca:name':
      self.walrus_name = value
    else:
      setattr(self, name, value)
  
  def describe(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    (options, args) = parser.parse_args()
    try:
      list = self.euca.connection.get_list('DescribeWalruses', {}, [('euca:item', Walrus)])
      for i in list:
        print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)


  def get_register_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    parser.add_option("-H","--host",dest="walrus_host",help="Hostname of the walrus.")
    parser.add_option("-p","--port",dest="walrus_port",type="int",default=8773,help="Port for the walrus.")
    return parser


  def register(self, walrus_name, walrus_host, walrus_port=8773):
    try:
      reply = self.euca.connection.get_object('RegisterWalrus', {'Name':'walrus','Host':walrus_host,'Port':walrus_port}, BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_deregister_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    parser.add_option("-n","--name",dest="walrus_name",help="Name of the walrus.")
    return parser
            
  def deregister(self, walrus_name):
    try:
      reply = self.euca.connection.get_object('DeregisterWalrus', {'Name':walrus_name},BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)
        
