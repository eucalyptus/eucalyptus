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

import boto,sys

class BooleanResponse:
  def __init__(self, reply=False):
    self.reply = reply
    self.error = None
      
  def __repr__(self):
    if self.error:
      print 'RESPONSE %s' % self.error
      sys.exit(1)
    else:
      return 'RESPONSE %s' % self.reply 

  def startElement(self, name, attrs, connection):
    return None

  def endElement(self, name, value, connection):
    if name == 'euca:_return':
      self.reply  = value
    elif name == 'Message':
      self.error = value
    else:
      setattr(self, name, value)


class StringList(list):

  def __repr__(self):
    r = ""
    for i in self:
      r = "%s %s" % (r,i)
    return r
    
  def startElement(self, name, attrs, connection):
    pass

  def endElement(self, name, value, connection):
    if name == 'euca:entry':
      self.append(value)
      
