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
      return 'CLUSTER %s %s' % (self.storage_name, self.host_name) 

  def startElement(self, name, attrs, connection):
      return None

  def endElement(self, name, value, connection):
    if name == 'euca:detail':
      self.host_name = value
    elif name == 'euca:name':
      self.storage_name = value
    else:
      setattr(self, name, value)
  
  def describe(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    (options, args) = parser.parse_args()
    try:
      list = self.euca.connection.get_list('DescribeStorageControllers', {}, [('euca:item', StorageController)])
      for i in list:
        print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)


  def get_register_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    parser.add_option("-n","--name",dest="sc_name",help="Name of the storage controller.")
    parser.add_option("-H","--host",dest="sc_host",help="Hostname of the storage.")
    parser.add_option("-p","--port",dest="sc_port",type="int",default=8773,help="Port for the storage.")
    return parser


  def register(self, sc_name, sc_host, sc_port=8773):
    try:
      reply = self.euca.connection.get_object('RegisterStorageController', {'Name':sc_name,'Host':sc_host,'Port':sc_port}, BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_deregister_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    parser.add_option("-n","--name",dest="sc_name",help="Name of the storage controller.")
    return parser
            
  def deregister(self, sc_name):
    try:
      reply = self.euca.connection.get_object('DeregisterStorageController', {'Name':sc_name},BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)
        
