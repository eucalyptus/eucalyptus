import boto,sys,euca_admin,re,os
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin import EucaAdmin
from optparse import OptionParser
from string import split
import pdb

SERVICE_PATH = '/services/Properties'
VERBOSE = False
class Property():
  
  
  def __init__(self, property_name=None, property_value=None,
               property_description=None, property_old_value=None):
    self.property_name = property_name
    self.property_value = property_value
    self.property_description = property_description
    self.property_old_value = property_old_value
    self.euca = EucaAdmin(path=SERVICE_PATH)

          
  def __repr__(self):
    global VERBOSE
    str = 'PROPERTY\t%s\t%s' % (self.property_name, self.property_value)
    if self.property_old_value is not None:
      str = '%s was %s' % (str, self.property_old_value)
    elif VERBOSE:
      str = '%s\nDESCRIPTION\t%s\t%s' % (str, self.property_name, self.property_description) 
    return str

  def startElement(self, name, attrs, connection):
      return None

  def endElement(self, name, value, connection):
    if name == 'euca:name':
      self.property_name = value
    elif name == 'euca:value':
      self.property_value = value
    elif name == 'euca:oldValue':
      self.property_old_value = value
    elif name == 'euca:description':
      self.property_description = value
    else:
      setattr(self, name, value)

  def get_parser(self):
    parser = OptionParser("usage: %prog [PROPERTY...]", version="Eucalyptus %prog VERSION")
    parser.add_option("-v", "--verbose", dest="verbose", action="store_true",
                      help="Show property descriptions.")
    return parser
        
  def parse_describe(self):
    global VERBOSE
    (options,args) = self.get_parser().parse_args()  
    if options.verbose:
      VERBOSE = True
    return (options,args)

  def cli_describe(self):
    (options,args) = self.parse_describe()
    self.describe(args)

  def describe(self,props=None):
    params = {}
    if props:
      self.euca.connection.build_list_params(params,props,'Properties')
    try:
      list = self.euca.connection.get_list('DescribeProperties',
                                           params, [('euca:item', Property)])
      for i in list:
        print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_parse_modify(self):
    parser = self.get_parser()
    parser.add_option("-p","--property",dest="props",action="append",
                      help="Modify KEY to be VALUE.  Can be given multiple times.",
                      metavar="KEY=VALUE")
    parser.add_option("-f","--property-from-file",dest="files",action="append",
                      help="Modify KEY to be modified with content of a file.",
                      metavar="KEY=<path to file>")
    global VERBOSE
    (options,args) = parser.parse_args()
    if options.verbose:
      VERBOSE = True
    if not options.props and not options.files:
      print "ERROR No options were specified."
      parser.print_help()
      sys.exit(1)
    else:
      if options.props:
        for i in options.props:
          if not re.match("^[\w.]+=[/\w\.]+$",i):
            print "ERROR Options must be of the form KEY=VALUE.  Illegally formatted option: %s" % i
            parser.print_help()
            sys.exit(1)
      elif options.files:
        pass
    return (options,args)

  def cli_modify(self):
    (options,args) = self.get_parse_modify()
    self.modify(options.props)
    self.modify_from_file(options.files)

  def _modify(self, name, value):
    try:
      result = self.euca.connection.get_object('ModifyPropertyValue',
                                               {'Name' : name, 'Value' : value},
                                               Property, verb='POST')
      print result
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def modify(self,modify_list):
    if modify_list:
      for i in modify_list:
        new_prop = split(i,"=")
        if not len(new_prop) == 2:
          print "ERROR Options must be of the form KEY=VALUE.  Illegally formatted option: %s" % i
          sys.exit(1)
        self._modify(new_prop[0], new_prop[1])

  def modify_from_file(self,modify_list):
    if modify_list:
      for i in modify_list:
        new_prop = split(i,"=")
        if not len(new_prop) == 2:
          print "ERROR Options must be of the form KEY=VALUE.  Illegally formatted option: %s" % i
          sys.exit(1)
        file_path = new_prop[1]
        if file_path == '-':
          value = sys.stdin.read()
        else:
          file_path = os.path.expanduser(file_path)
          file_path = os.path.expandvars(file_path)
          if not os.path.isfile(file_path):
            print "ERROR File %s does not exist" % file_path
            sys.exit(1)
          fp = open(file_path)
          value = fp.read()
          fp.close()
        self._modify(new_prop[0], value)

