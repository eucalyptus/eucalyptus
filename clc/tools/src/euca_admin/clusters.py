import boto,sys,euca_admin
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin import EucaAdmin
from optparse import OptionParser

SERVICE_PATH = '/services/Configuration'
class Cluster():
  
  
  def __init__(self, cluster_name=None, host_name=None, port=None):
    self.cluster_name = cluster_name
    self.host_name = host_name
    self.euca = EucaAdmin(path=SERVICE_PATH)

          
  def __repr__(self):
      return 'CLUSTER\t%s\t%s' % (self.cluster_name, self.host_name) 

  def startElement(self, name, attrs, connection):
      return None

  def endElement(self, name, value, connection):
    if name == 'euca:detail':
      self.host_name = value
    elif name == 'euca:name':
      self.cluster_name = value
    else:
      setattr(self, name, value)
  
  def get_describe_parser(self):
    parser = OptionParser("usage: %prog [CLUSTERS...]",version="Eucalyptus %prog VERSION")
    return parser.parse_args()
  
  def cli_describe(self):
    (options, args) = self.get_describe_parser()
    self.cluster_describe(args)
    
  def cluster_describe(self,clusters=None):
    params = {}
    if clusters:
      self.euca.connection.build_list_params(params,clusters,'Name')
    try:
      list = self.euca.connection.get_list('DescribeClusters', params, [('euca:item', Cluster)])
      for i in list:
        print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_register_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    parser.add_option("-H","--host",dest="cc_host",help="Hostname of the cluster.")
    parser.add_option("-p","--port",dest="cc_port",type="int",default=8774,help="Port for the cluster.")
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument CLUSTERNAME is missing or malformed."
      parser.print_help()
      sys.exit(1)
    else:
      return (options,args)  

  def cli_register(self):
    (options,args) = self.get_register_parser()
    self.register(args[0],options.cc_host,options.cc_port)

  def register(self, cc_name, cc_host, cc_port=8773):
    try:
      reply = self.euca.connection.get_object('RegisterCluster', {'Name':cc_name,'Host':cc_host,'Port':cc_port}, BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)


  def get_deregister_parser(self):
    parser = OptionParser("usage: %prog [options] CLUSTERNAME",version="Eucalyptus %prog VERSION")
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument CLUSTERNAME is missing or malformed."
      parser.print_help()
      sys.exit(1)
    else:
      return (options,args)  
            
  def cli_deregister(self):
    (options,args) = self.get_deregister_parser()
    self.deregister(args[0])

  def deregister(self, cc_name):
    try:
      reply = self.euca.connection.get_object('DeregisterCluster', {'Name':cc_name},BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)
        
