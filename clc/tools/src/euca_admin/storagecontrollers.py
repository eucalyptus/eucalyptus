import boto,sys,euca_admin
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin import EucaAdmin
from optparse import OptionParser

SERVICE_PATH = '/services/Configuration'
class StorageController():
  
  
  def __init__(self, storage_name=None, host_name=None, port=None):
    self.storage_name = storage_name
    self.host_name = host_name
    self.euca = EucaAdmin(path=SERVICE_PATH)

          
  def __repr__(self):
      return 'STORAGE\t%s\t%s' % (self.storage_name, self.host_name) 

  def startElement(self, name, attrs, connection):
      return None

  def endElement(self, name, value, connection):
    if name == 'euca:detail':
      self.host_name = value
    elif name == 'euca:name':
      self.storage_name = value
    else:
      setattr(self, name, value)
  
  def get_describe_parser(self):
    parser = OptionParser("usage: %prog [SCNAME...]",version="Eucalyptus %prog VERSION")
    return parser.parse_args()

  def cli_describe(self):
    (options, args) = self.get_describe_parser()
    self.describe(args)

  def describe(self,scs=None):
    params = {}
    if scs:
			self.euca.connection.build_list_params(params,groups,'Names')
    try:
      list = self.euca.connection.get_list('DescribeStorageControllers', params, [('euca:item', StorageController)])
      for i in list:
        print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)


  def get_register_parser(self):
    parser = OptionParser("usage: %prog [options] SCNAME",version="Eucalyptus %prog VERSION")
    parser.add_option("-H","--host",dest="sc_host",help="Hostname of the storage.")
    parser.add_option("-p","--port",dest="sc_port",type="int",default=8773,help="Port for the storage.")
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument SCNAME is missing or malformed."
      parser.print_help()
      sys.exit(1)
    else:
      return (options,args)  

  def cli_register(self):
    (options, args) = self.get_register_parser()
    self.register(args[0],options.sc_host,options.sc_port)

  def register(self, sc_name, sc_host, sc_port=8773):
    try:
      reply = self.euca.connection.get_object('RegisterStorageController', {'Name':sc_name,'Host':sc_host,'Port':sc_port}, BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_deregister_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument SCNAME is missing or malformed."
      parser.print_help()
      sys.exit(1)
    else:
      return (options,args)  

  def cli_deregister(self):
    (options, args) = self.get_deregister_parser()
    self.deregister(args[0])
            
  def deregister(self, sc_name):
    try:
      reply = self.euca.connection.get_object('DeregisterStorageController', {'Name':sc_name},BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)
        
