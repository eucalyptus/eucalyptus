import boto,sys,euca_admin
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin.generic import StringList
from boto.resultset import ResultSet
from euca_admin import EucaAdmin
from optparse import OptionParser

SERVICE_PATH = '/services/Accounts'
class Group():
  
  
  def __init__(self, groupName=None):
    self.group_groupName = groupName
    self.group_users = StringList()
    self.group_auths = StringList()
    self.euca = EucaAdmin(path=SERVICE_PATH)
          
  def __repr__(self):
    r = 'GROUP      \t%s\t' % (self.group_groupName)
    r = '%s\nUSERS\t%s\t%s' % (r,self.group_groupName,self.group_users)
    r = '%s\nAUTH\t%s\t%s' % (r,self.group_groupName,self.group_auths)
    return r
      
  def startElement(self, name, attrs, connection):
    if name == 'euca:users':
      return self.group_users
    if name == 'euca:authorizations':
      return self.group_auths
    else:
      return None

  def endElement(self, name, value, connection):
    if name == 'euca:groupName':
      self.group_groupName = value
    else:
      setattr(self, name, value)
          
  def get_describe_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    return parser
  
  def cli_describe(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    (options, args) = parser.parse_args()
    self.describe(args)
    
  def describe(self,groups=None):
    params = {}
    if groups:
      self.euca.connection.build_list_params(params,groups,'GroupNames')
    try:
      list = self.euca.connection.get_list('DescribeGroups', params, [('euca:item', Group)])
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

  def get_membership_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    parser.add_option("-n","--name",dest="groupName",help="Name of the Group.")
    parser.add_option("-u","--user",dest="userName",help="Name of the User.")
    (options, args) = parser.parse_args()
    return options

  def add_membership(self):
    options = self.get_membership_parser()
    try:
      reply = self.euca.connection.get_object('AddGroupMember', {'GroupName':options.groupName,'UserName':options.userName},BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def remove_membership(self):
    try:
      reply = self.euca.connection.get_object('AddGroupMember', {'GroupName':options.groupName,'UserName':options.userName},BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)


  def get_grant_authorize_parser(self):
    parser = OptionParser("usage: %prog [options] GROUP",version="Eucalyptus %prog VERSION")
    parser.add_option("-n","--name",dest="groupName",help="Name of the Group.")
    parser.add_option("-z","--zone",dest="zoneName",help="Name of the availability zone.")
    (options, args) = parser.parse_args()
    return options
            
  def grant_authorize(self):
    options = self.get_grant_authorize_parser()
    try:
      reply = self.euca.connection.get_object('GrantGroupAuthorization', {'GroupName':options.groupName,'ZoneName':options.zoneName},BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_revoke_authorize_parser(self):
    parser = OptionParser("usage: %prog [options] GROUP",version="Eucalyptus %prog VERSION")
    parser.add_option("-n","--name",dest="groupName",help="Name of the Group.")
    parser.add_option("-z","--zone",dest="zoneName",help="Name of the availability zone.")
    (options, args) = parser.parse_args()
    return options
            
  def revoke_authorize(self):
    options = self.get_revoke_authorize_parser()
    try:
      reply = self.euca.connection.get_object('RevokeGroupAuthorization', {'GroupName':options.groupName,'ZoneName':options.zoneName},BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)
  