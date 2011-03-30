import boto,sys,euca_admin,re
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin import EucaAdmin
from optparse import OptionParser

SERVICE_PATH = '/services/Configuration'
class Walrus():
  
  def __init__(self, walrus_name=None, host_name=None,
               port=None, partition=None, state=None):
    self.walrus_name = walrus_name
    self.host_name = host_name
    self.port = port
    self.partition = partition
    self.state = state
    self.euca = EucaAdmin(path=SERVICE_PATH)
          
  def __repr__(self):
      return 'WALRUS\t%s\t%s\t%s\t%s\t%s' % (self.partition, self.walrus_name, 
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
      self.walrus_name = value
    elif name == 'euca:partition':
      self.partition = value
    else:
      setattr(self, name, value)
  
  def describe(self):
    parser = OptionParser("usage: %prog [options]",
                          version="Eucalyptus %prog VERSION")
    (options, args) = parser.parse_args()
    try:
      list = self.euca.connection.get_list('DescribeWalruses', {},
                                           [('euca:item', Walrus)])
      for i in list:
        print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_register_parser(self):
    parser = OptionParser("usage: %prog [options]",
                          version="Eucalyptus %prog VERSION")
    parser.add_option("-H","--host",dest="host",
                      help="Hostname of the walrus.")
    parser.add_option("-p","--port",dest="port",type="int",
                      default=8773,help="Port for the walrus.")
    parser.add_option("-P","--partition",dest="partition",
                      help="Partition for the walrus.")
    (options,args) = parser.parse_args()    
    if options.host == None:
      self.euca.handle_error("You must provide a hostname (-H or --host)")
    return (options,args)  

  def cli_register(self):
    (options, args) = self.get_register_parser()
    if len(args) != 1:
      self.register(options.host,port=options.port)
    else:
      self.register(options.host,name=args[0],
                    port=options.port, partition=options.partition)

  def register(self, host, name='walrus', port=8773, partition=None):
    if host == None:
      self.euca.handle_error("Missing hostname")
    params = {'Partition':'walrus',
              'Name':name,
              'Host':host,
              'Port':port}
    if partition:
      params['Partition'] = partition
    try:
      reply = self.euca.connection.get_object('RegisterWalrus',
                                              params,
                                              BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_deregister_parser(self):
    parser = OptionParser("usage: %prog [options] NAME",
                          version="Eucalyptus %prog VERSION")
    parser.add_option("-P","--partition",dest="partition",
                      help="Partition for the walrus.")                          
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument NAME is missing or malformed."
      parser.print_help()
      sys.exit(1)
    else:
      return (options,args)  

  def cli_deregister(self):
    (options, args) = self.get_deregister_parser()
    if len(args) != 1:
      self.deregister()
    else:
      self.deregister(args[0])
            
  def deregister(self, name, partition='walrus'):
    params = {'Name':name}
    if partition:
      params['Partition'] = partition
    try:
      reply = self.euca.connection.get_object('DeregisterWalrus',
                                              {'Name':name},BooleanResponse)
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
                      help="Partition for the storage.")                          
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument WALRUSNAME is missing or malformed."
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
        reply = self.euca.connection.get_object('ModifyWalrusAttribute',
                                                {'Partition' : partition, 'Name' : name, 'Attribute' : key,'Value' : value},
                                                BooleanResponse)
        print reply
      except EC2ResponseError, ex:
        self.euca.handle_error(ex)
