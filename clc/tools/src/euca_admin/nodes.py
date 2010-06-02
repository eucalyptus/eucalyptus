import boto,sys,euca_admin
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin import EucaAdmin
from optparse import OptionParser

SERVICE_PATH = '/services/Configuration'
class Node():
  
  
  def __init__(self, node_name=None, cluster_name=None, port=None):
    self.node_name = node_name
    self.cluster_name = cluster_name
    self.instances = []
    self.euca = EucaAdmin(path=SERVICE_PATH)

          
  def __repr__(self):
      s = 'NODE\t%s\t%s' % (self.node_name, self.cluster_name)
      for i in self.instances:
        s = '%s\t%s' % (s,i)
      return s

  def startElement(self, name, attrs, connection):
      return None

  def endElement(self, name, value, connection):
    if name == 'euca:clusterName':
      self.cluster_name = value
    elif name == 'euca:name':
      self.node_name = value
    elif name == 'euca:entry':
      self.instances.append(value)
    else:
      setattr(self, name, value)
  
  def get_describe_parser(self):
    parser = OptionParser("usage: %prog [NODES...]",version="Eucalyptus %prog VERSION")
    return parser.parse_args()
  
  def cli_describe(self):
    (options, args) = self.get_describe_parser()
    self.node_describe(args)
    
  def node_describe(self,nodes=None):
    params = {}
    if nodes:
      self.euca.connection.build_list_params(params,nodes,'Name')
    try:
      list = self.euca.connection.get_list('DescribeNodes', params, [('euca:item', Node)])
      for i in list:
        print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

