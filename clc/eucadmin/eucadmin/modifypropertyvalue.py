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

from boto.roboto.awsqueryrequest import AWSQueryRequest
from boto.roboto.param import Param
import eucadmin

def encode_prop(param, dict, value):
    t = value.split('=')
    if len(t) != 2:
        print "Options must be of the form KEY=VALUE: %s" % value
        sys.exit(1)
    dict['Name'] = t[0]
    dict['Value'] = t[1]
    
class ModifyPropertyValue(AWSQueryRequest):
  
    ServicePath = '/services/Properties'
    ServiceClass = eucadmin.EucAdmin
    Description = 'Modify property'
    
    Params = [Param(name='property',
                    short_name='p',
                    long_name='property',
                    ptype='string',
                    optional=True,
                    encoder=encode_prop,
                    doc='Modify property (KEY=VALUE)'),
              Param(name='property_from_file',
                    short_name='f',
                    long_name='property-from-file',
                    ptype='file',
                    optional=True,
                    encoder=encode_prop,
                    doc='Modify property with content of file')]
          
    def get_connection(self, **args):
        if self.connection is None:
            args['path'] = self.ServicePath
            self.connection = self.ServiceClass(**args)
        return self.connection
      
    def cli_formatter(self, data):
        prop = data['euca:ModifyPropertyValueResponseType']
        print 'PROPERTY\t%s\t%s was %s' % (prop['euca:name'],
                                           prop['euca:value'],
                                           prop['euca:oldValue'])
        
def main(**args):
    req = ModifyPropertyValue(**args)
    return req.send()


def main_cli():
    req = ModifyPropertyValue()
    req.do_cli()
