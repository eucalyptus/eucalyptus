import boto,sys,euca_admin
from boto.exception import EC2ResponseError
from euca_admin.generic import BooleanResponse
from euca_admin import EucaAdmin
from optparse import OptionParser

SERVICE_PATH = '/services/Accounts'
class User():
  
  
  def __init__(self, userName=None, email="N/A", certificateCode=None, confirmationCode=None, accessKey=None, secretKey=None, confirmed=False, admin=False, enabled=False, distinguishedName=None, certificateSerial=None):
    self.user_userName = userName
    self.user_email = email
    self.user_distinguishedName = distinguishedName
    self.user_certificateSerial = certificateSerial
    self.user_certificateCode = certificateCode
    self.user_confirmationCode = confirmationCode
    self.user_accessKey = accessKey
    self.user_secretKey = secretKey
    self.user_confirmed = confirmed
    self.user_admin = admin
    self.user_enabled = enabled
    self.user_groups = []
    self.user_revoked = []
    self.user_list = self.user_groups
    self.euca = EucaAdmin(path=SERVICE_PATH)
          
  def __repr__(self):
    r = 'USER\t\t%s\t%s%s\t%s' % (self.user_userName,self.user_email,'\tADMIN' if self.user_admin == 'true' else ' ', 'ENABLED' if self.user_enabled == 'true' else 'DISABLED' )
    for s in self.user_groups:
      r = '%s\nUSER-GROUP\t%s\t%s' % (r,self.user_userName,s)
    r = '%s\nUSER-CERT\t%s\t%s\t%s' % (r,self.user_userName,self.user_distinguishedName,self.user_certificateSerial)
    for s in self.user_revoked:
      r = '%s\nUSER-REVOKED\t%s\t%s' % (r,self.user_userName,s)
    r = '%s\nUSER-KEYS\t%s\t%s\t%s' % (r,self.user_userName,self.user_accessKey,self.user_secretKey)
    r = '%s\nUSER-CODE\t%s\t%s' % (r,self.user_userName,self.user_certificateCode)
    r = '%s\nUSER-WEB \t%s\t%s' % (r,self.user_userName,self.user_confirmationCode)
    return r
      
  def startElement(self, name, attrs, connection):
      return None

  def endElement(self, name, value, connection):
    if name == 'euca:userName':
      self.user_userName = value
    elif name == 'euca:email':
      self.user_email = value
    elif name == 'euca:admin':
      self.user_admin = value
    elif name == 'euca:confirmed':
      self.user_confirmed = value
    elif name == 'euca:enabled':
      self.user_enabled = value
    elif name == 'euca:distinguishedName':
      self.user_distinguishedName = value
    elif name == 'euca:certificateSerial':
      self.user_certificateSerial = value
    elif name == 'euca:certificateCode':
      self.user_certificateCode = value
    elif name == 'euca:confirmationCode':
      self.user_confirmationCode = value
    elif name == 'euca:accessKey':
      self.user_accessKey = value
    elif name == 'euca:secretKey':
      self.user_secretKey = value
    elif name == 'euca:groups':
      self.user_list = self.user_groups
    elif name == 'euca:revoked':
      self.user_list = self.user_revoked
    elif name == 'euca:entry':
      self.user_list.append(value)
    else:
      setattr(self, name, value)
          
  def get_describe_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    return parser
  
  def describe(self):
    try:
      list = self.euca.connection.get_list('DescribeUsers', {}, [('euca:item', User)])
      for i in list:
        print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)


  def get_add_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    parser.add_option("-n","--name",dest="userName",help="Name of the User.")
    parser.add_option("-e","--email",dest="email",default="N/A", help="Email of the User.")
    parser.add_option("-a","--admin",dest="admin",default=False,action="store_true",help="Mark user as admin.")
    return parser


  def add(self, user_userName, user_email, user_admin):
    try:
      reply = self.euca.connection.get_object('AddUser', {'UserName':user_userName,'Email':user_email,'Admin':user_admin}, BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_delete_parser(self):
    parser = OptionParser("usage: %prog [options]",version="Eucalyptus %prog VERSION")
    parser.add_option("-n","--name",dest="userName",help="Name of the User.")
    return parser
            
  def delete(self, user_name):
    try:
      reply = self.euca.connection.get_object('DeleteUser', {'UserName':user_name},BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)
        