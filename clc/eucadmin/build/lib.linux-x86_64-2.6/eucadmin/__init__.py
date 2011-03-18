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

import boto
import os
import sys
import re
import urlparse
from boto.ec2.regioninfo import RegionInfo

__version__ = '1.0a'

class EucAdmin:
    
  def __init__(self, path='/services/Configuration'):
      if not 'EC2_ACCESS_KEY' in os.environ:
          print 'Environment variable EC2_ACCESS_KEY is unset.'
          sys.exit(1)
      if not 'EC2_SECRET_KEY' in os.environ:
          print 'Environment variable EC2_SECRET_KEY is unset.'
          sys.exit(1)
      if not 'EC2_URL' in os.environ:
          print 'Environment variable EC2_URL is unset.'
          sys.exit(1)
      self.path = path
      self.region='eucalyptus'
      self.access_key = os.getenv('EC2_ACCESS_KEY')
      self.secret_key = os.getenv('EC2_SECRET_KEY')
      self.url = os.getenv('EC2_URL')
      self.parsed = urlparse.urlparse(self.url)
      self.connection = boto.connect_euca(self.parsed.hostname,
                                          self.access_key,
                                          self.secret_key,
                                          path=self.path)
      self.connection.APIVersion = 'eucalyptus'
    
  def get_connection(self):
      return self.conn

  def handle_error(self,ex):
      s = ""
      if not hasattr(ex,"errors"):
          s = 'ERROR %s' % (ex)
      else:
          if ex.errors.__len__() != 0:
              for i in ex.errors:
                  s = '%sERROR %s %s %s: %s\n' % (s,ex.status,
                                                  ex.reason,
                                                  i[0], i[1])
          else:
              s = 'ERROR %s %s %s' % (ex.status, ex.reason, ex)
          while s.count("\n") != 3:
              s = re.sub(".*Exception.*\n", ": ", s)
      print s.replace("\n","")
      sys.exit(1)
