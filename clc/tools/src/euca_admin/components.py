import boto,sys,euca_admin,re
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin.generic import StringList
from euca_admin import EucaAdmin
from optparse import OptionParser

SERVICE_PATH = '/services/Configuration'
class Component():
  
  def __init__(self, component_name=None, host_name=None,
               port=None, partition=None, state=None):
    self.component_name = component_name
    self.host_name = host_name
    self.port = port
    self.partition = partition
    self.state = state
    self.euca = EucaAdmin(path=SERVICE_PATH)
    self.verbose = False
          
  def __repr__(self):
      return 'COMPONENT\t%-15.15s\t%-15.15s\t%-25s\t%s\t%s' % (self.partition, self.component_name,
                                      self.host_name, self.state, self.detail ) 

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
      self.component_name = value
    elif name == 'euca:partition':
      self.partition = value
    else:
      setattr(self, name, value)
  
  def get_describe_parser(self):
    parser = OptionParser("usage: %prog [COMPONENTS...]",
                          version="Eucalyptus %prog VERSION")
    parser.add_option("-v", "--verbose", dest="verbose", default=False, action="store_true", help="Report verbose details about the state of the component.")
    return parser.parse_args()
  
  def cli_describe(self):
    (options, args) = self.get_describe_parser()
    self.component_describe(args,options.verbose)
    
  def component_describe(self,components=None,verbose=False):
    params = {}
    if components:
      self.euca.connection.build_list_params(params,components,'Name')
    try:
      list = self.euca.connection.get_list('DescribeComponents', params,
                                           [('euca:item', Component)])
      for i in list:
        if verbose:
          print i
        elif not verbose and not i.host_name == 'detail':
          print i
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
                      help="Partition for the component.")                          
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument COMPONENTNAME is missing or malformed."
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
        reply = self.euca.connection.get_object('ModifyComponentAttribute',
                                                {'Partition' : partition, 'Name' : name, 'Attribute' : key,'Value' : value},
                                                BooleanResponse)
        print reply
      except EC2ResponseError, ex:
        self.euca.handle_error(ex)

class ServiceStatus():
  def __init__(self, service_uuid=None, service_name=None, service_partition=None, service_type=None, service_url=None):
    self.service_uuid = service_uuid
    self.service_name = service_name
    self.service_partition = service_partition
    self.service_type = service_type
    self.service_url = service_url

  def __repr__(self):
      return '%-15.15s\t%s' % (self.service_type.upper(), ('%s.%s'% (self.service_partition, self.service_name)))

  def startElement(self, name, attrs, connection):
      return None    

  def endElement(self, name, value, connection):
    if name == 'euca:uuid':
      self.service_uuid = value
    elif name == 'euca:partition':
      self.service_partition = value
    elif name == 'euca:name':
      self.service_name = value
    elif name == 'euca:type':
      self.service_type = value
    elif name == 'euca:uri':
      self.service_uri = value
    else:
      setattr(self, name, value)


class Service():

  def __init__(self, service_epoch=None, service_state=None, service_detail=None):
    self.service_epoch = service_epoch
    self.service_state = service_state
    self.service_detail = StringList()
    self.service_id = ServiceStatus()
    self.euca = EucaAdmin(path=SERVICE_PATH)
    self.verbose = False

  def __repr__(self):
      return 'SERVICE\t%s\t%s\t%s\t%s' % (self.service_id, self.service_state, self.service_epoch, self.service_id.service_url )

  def startElement(self, name, attrs, connection):
    if name == 'euca:serviceId':
      return self.service_id
    elif name == 'euca:details':
      return self.service_detail
    else:
      return None    

  def endElement(self, name, value, connection):
    if name == 'euca:item':
      self.service_detail = '%s, %s' % (self.service_detail, value)
    elif name == 'euca:localState':
      self.service_state = value
    elif name == 'euca:localEpoch':
      self.service_epoch = value
    else:
      setattr(self, name, value)

  def get_describe_parser(self):
    parser = OptionParser("usage: %prog [COMPONENTS...]",
                          version="Eucalyptus %prog VERSION")
    parser.add_option("-v", "--verbose", dest="verbose", default=False, action="store_true", help="Report verbose details about the state of the component.")
    return parser.parse_args()

  def cli_describe(self):
    (options, args) = self.get_describe_parser()
    self.service_describe(args,options.verbose)

  def service_describe(self,components=None,verbose=False):
    params = {}
    if components:
      self.euca.connection.build_list_params(params,components,'Name')
    try:
      list = self.euca.connection.get_list('DescribeServices', params,
                                           [('euca:item', Service)])
      for i in list:
        if verbose:
          print i
        elif not verbose:
          print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_modify_parser(self):
    parser = OptionParser("usage: %prog [COMPONENTS...]",
                          version="Eucalyptus %prog VERSION")
    parser.add_option("-s", "--state", dest="state", default=False, action="store_true", help="Attempt to change the state of the service to be <state>.")
    return parser.parse_args()

  def cli_modify(self):
    (options, args) = self.get_modify_parser()
    self.service_modify(args,options.verbose)

  def service_modify(self,operation=None,service_type=None,service_partition=None,service_name=None,verbose=False):
    params = {}
    if components:
      self.euca.connection.build_list_params(params,components,'Name')
    try:
      list = self.euca.connection.get_list('DescribeServices', params,
                                           [('euca:item', Service)])
      for i in list:
        if verbose:
          print i
        elif not verbose:
          print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)
