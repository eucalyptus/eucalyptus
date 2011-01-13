import boto,sys,euca_admin
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin import EucaAdmin
from optparse import OptionParser
import re

SERVICE_PATH = '/services/Configuration'
class Arbitrator():
  
  def __init__(self, arbitrator_name=None, host_name=None,
               port=None, partition=None):
    self.arbitrator_name = arbitrator_name
    self.host_name = host_name
    self.port = port
    self.partition = partition
    self.euca = EucaAdmin(path=SERVICE_PATH)

          
  def __repr__(self):
      return 'ARBITRATOR\t%s\t%s\t%s' % (self.arbitrator_name, self.partition,
                                      self.host_name) 

  def startElement(self, name, attrs, connection):
      return None

  def endElement(self, name, value, connection):
    if name == 'euca:detail':
      self.host_name = value
    elif name == 'euca:name':
      self.arbitrator_name = value
    elif name == 'euca:partition':
      self.partition = value
    else:
      setattr(self, name, value)
  
  def get_describe_parser(self):
    parser = OptionParser("usage: %prog [ARBITRATORS...]",
                          version="Eucalyptus %prog VERSION")
    return parser.parse_args()
  
  def cli_describe(self):
    (options, args) = self.get_describe_parser()
    self.arbitrator_describe(args)
    
  def arbitrator_describe(self,arbitrators=None):
    params = {}
    if arbitrators:
      self.euca.connection.build_list_params(params,arbitrators,'Name')
    try:
      list = self.euca.connection.get_list('DescribeArbitrators', params,
                                           [('euca:item', Arbitrator)])
      for i in list:
        print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_register_parser(self):
    parser = OptionParser("usage: %prog [options]",
                          version="Eucalyptus %prog VERSION")
    parser.add_option("-H","--host",dest="host",
                      help="Hostname of the arbitrator.")
    parser.add_option("-p","--port",dest="port",type="int",default=8774,
                      help="Port for the arbitrator.")
    parser.add_option("-P","--partition",dest="partition",
                      help="Partition for the arbitrator.")
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument ARBITRATORNAME is missing or malformed."
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
      reply = self.euca.connection.get_object('RegisterArbitrator',
                                              params,
                                              BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)


  def get_deregister_parser(self):
    parser = OptionParser("usage: %prog [options] ARBITRATORNAME",
                          version="Eucalyptus %prog VERSION")
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument ARBITRATORNAME is missing or malformed."
      parser.print_help()
      sys.exit(1)
    else:
      return (options,args)  
            
  def cli_deregister(self):
    (options,args) = self.get_deregister_parser()
    self.deregister(args[0])

  def deregister(self, name):
    try:
      reply = self.euca.connection.get_object('DeregisterArbitrator',
                                              {'Name' : name},
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
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument ARBITRATORNAME is missing or malformed."
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
    self.modify(args(1),options.props)

  def modify(self,name,modify_list):
    for entry in modify_list:
      key, value = entry.split("=")
      try:
        reply = self.euca.connection.get_object('ModifyArbitratorAttribute',
                                                {'Name' : name, 'Attribute' : key,'Value' : value},
                                                BooleanResponse)
        print reply
      except EC2ResponseError, ex:
        self.euca.handle_error(ex)

