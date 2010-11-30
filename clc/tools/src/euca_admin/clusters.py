import boto,sys,euca_admin,re
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin import EucaAdmin
from optparse import OptionParser

SERVICE_PATH = '/services/Configuration'
class Cluster():
  
  def __init__(self, cluster_name=None, host_name=None,
               port=None, partition=None, state=None):
    self.cluster_name = cluster_name
    self.host_name = host_name
    self.port = port
    self.partition = partition
    self.state = state
    self.euca = EucaAdmin(path=SERVICE_PATH)

          
  def __repr__(self):
      return 'CLUSTER\t%s\t%s\t%s\t%s\t%s' % (self.partition, self.cluster_name,
                                      self.host_name, self.state, self.detail) 

  def startElement(self, name, attrs, connection):
      return None

  def endElement(self, name, value, connection):
    if name == 'euca:detail':
      self.detail = value
    elif name == 'euca:state':
      self.state = value
    elif name == 'euca:hostName':
      self.host_name = value
    elif name == 'euca:name':
      self.cluster_name = value
    elif name == 'euca:partition':
      self.partition = value
    else:
      setattr(self, name, value)
  
  def get_describe_parser(self):
    parser = OptionParser("usage: %prog [CLUSTERS...]",
                          version="Eucalyptus %prog VERSION")
    return parser.parse_args()
  
  def cli_describe(self):
    (options, args) = self.get_describe_parser()
    self.cluster_describe(args)
    
  def cluster_describe(self,clusters=None):
    params = {}
    if clusters:
      self.euca.connection.build_list_params(params,clusters,'Name')
    try:
      list = self.euca.connection.get_list('DescribeClusters', params,
                                           [('euca:item', Cluster)])
      for i in list:
        print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_register_parser(self):
    parser = OptionParser("usage: %prog [options]",
                          version="Eucalyptus %prog VERSION")
    parser.add_option("-H","--host",dest="host",
                      help="Hostname of the cluster.")
    parser.add_option("-p","--port",dest="port",type="int",default=8774,
                      help="Port for the cluster.")
    parser.add_option("-P","--partition",dest="partition",
                      help="Partition for the cluster.")
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument CLUSTERNAME is missing or malformed."
      parser.print_help()
      sys.exit(1)
    else:
      return (options,args)  

  def cli_register(self):
    (options,args) = self.get_register_parser()
    self.register(args[0], options.host,
                  options.port, options.partition)

  def register(self, name, host, port=8773, partition=None):
    params = {'Name':name,
              'Host':host,
              'Port':port}
    if partition:
      params['Partition'] = partition
    try:
      reply = self.euca.connection.get_object('RegisterCluster',
                                              params,
                                              BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)


  def get_deregister_parser(self):
    parser = OptionParser("usage: %prog [options] CLUSTERNAME",
                          version="Eucalyptus %prog VERSION")
    parser.add_option("-P","--partition",dest="partition",
                      help="Partition for the cluster.")                          
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

  def deregister(self, name, partition=None):
    params = {'Name':name}
    if partition:
      params['Partition'] = partition
    try:
      reply = self.euca.connection.get_object('DeregisterCluster',
                                              params,
                                              BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)
        
  def get_modify_parser(self):
    parser = OptionParser("usage: %prog [options]",
                          version="Eucalyptus %prog VERSION")
    parser.add_option("-p","--property",dest="props",
                      action="append",
                      help="Modify KEY to be VALUE.  Can be given multiple times.",
                      metavar="KEY=VALUE")
    parser.add_option("-P","--partition",dest="partition",
                      help="Partition for the cluster.")                          
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument CLUSTERNAME is missing or malformed."
      parser.print_help()
      sys.exit(1)
    if not options.props:
      print "ERROR No options were specified."
      parser.print_help()
      sys.exit(1)
    for i in options.props:
      if not re.match("^[\w.]+=[\w\.]+$",i):
        print "ERROR Options must be of the form KEY=VALUE.  Illegally formatted option: %s" % i
        parser.print_help()
        sys.exit(1)
    return (options,args)

  def cli_modify(self):
    (options,args) = self.get_modify_parser()
    self.modify(options.partition,args[0],options.props)

  def modify(self,partition,name,modify_list):
    for entry in modify_list:
      key, value = entry.split("=")
      try:
        reply = self.euca.connection.get_object('ModifyClusterAttribute',
                                                {'Partition' : partition, 'Name' : name, 'Attribute' : key,'Value' : value},
                                                BooleanResponse)
        print reply
      except EC2ResponseError, ex:
        self.euca.handle_error(ex)

