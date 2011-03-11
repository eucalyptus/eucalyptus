#!/usr/bin/env python
# Copyright (c) 2011, Eucalyptus Systems, Inc.
# All rights reserved.
#
# Redistribution and use of this software in source and binary forms, with or
# without modification, are permitted provided that the following conditions
# are met:
#
#   Redistributions of source code must retain the above
#   copyright notice, this list of conditions and the
#   following disclaimer.
#
#   Redistributions in binary form must reproduce the above
#   copyright notice, this list of conditions and the
#   following disclaimer in the documentation and/or other
#   materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
# Author: Mitch Garnaat mgarnaat@eucalyptus.com

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
    parser = OptionParser("usage: %prog [GROUPS...]",version="Eucalyptus %prog VERSION")
    return parser.parse_args()
  
  def cli_describe(self):
    (options, args) = self.get_describe_parser()
    self.group_describe(args)
    
  def group_describe(self,groups=None):
    params = {}
    if groups:
      self.euca.connection.build_list_params(params,groups,'GroupNames')
    try:
      list = self.euca.connection.get_list('DescribeGroups', params, [('euca:item', Group)])
      for i in list:
        print i
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def get_single_parser(self):
    parser = OptionParser("usage: %prog GROUPNAME",version="Eucalyptus %prog VERSION")
    (options,args) = parser.parse_args()    
    if len(args) != 1:
      print "ERROR  Required argument GROUPNAME is missing or malformed."
      parser.print_help()
      sys.exit(1)
    else:
      return (options,args)  

  def cli_add(self):
    (options, args) = self.get_single_parser()
    self.group_add(args[0])

  def group_add(self, groupName):
    try:
      reply = self.euca.connection.get_object('AddGroup', {'GroupName':groupName}, BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)

  def cli_delete(self):
    (options, args) = self.get_single_parser()
    self.group_delete(args[0])
            
  def group_delete(self, groupName):
    try:
      reply = self.euca.connection.get_object('DeleteGroup', {'GroupName':groupName},BooleanResponse)
      print reply
    except EC2ResponseError, ex:
      self.euca.handle_error(ex)


