import boto,sys,euca_admin
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin import EucaAdmin
from optparse import OptionParser

SERVICE_PATH = '/services/Accounts'
class Group():
  
  
  def __init__(self, groupName=None):
    self.group_groupName = groupName
    self.group_users = []
    self.euca = EucaAdmin(path=SERVICE_PATH)
          
  def __repr__(self):
    r = ''
    for s in self.group_users:
      r = '%s\t%s' % (r,s)
    r = 'GROUP\t%s\t%s' % (self.group_groupName,r)
    return r
      
  def startElement(self, name, attrs, connection):
      return None

  def endElement(self, name, value, connection):
    if name == 'euca:groupName':
      self.group_groupName = value
    elif name == 'euca:entry':
      self.user_groups.append(value)
    else:
      setattr(self, name, value)
          
  def get_describe_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    return parser
  
  def describe(self):
    try:
      list = self.euca.connection.get_list('DescribeGroups', {}, [('euca:item', Group)])
      for i in list:
        print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)


  def get_add_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    parser.add_option("-n","--name",dest="groupName",help="Name of the group.")
    return parser


  def add(self, groupName):
    try:
      reply = self.euca.connection.get_object('AddGroup', {'GroupName':groupName}, BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_delete_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    parser.add_option("-n","--name",dest="groupName",help="Name of the Group.")
    return parser
            
  def delete(self, groupName):
    try:
      reply = self.euca.connection.get_object('DeleteGroup', {'GroupName':groupName},BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)
        