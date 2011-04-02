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

class DeregisterStorageController(AWSQueryRequest):
  
    ServicePath = '/services/Configuration'
    ServiceClass = eucadmin.EucAdmin
    Description = 'Deregister storage controller'
    Params = [Param(name='Partition',
                    short_name='P',
                    long_name='partition',
                    ptype='string',
                    optional=True,
                    doc='Partition for the storage controller')]
    Args = [Param(name='Name',
                  long_name='name',
                  ptype='string',
                  optional=False,
                  doc='The storage controller name')]

    def get_connection(self, **args):
        if self.connection is None:
            args['path'] = self.ServicePath
            self.connection = self.ServiceClass(**args)
        return self.connection
      
    def cli_formatter(self, data):
        data = data['euca:DeregisterStorageControllerResponseType']
        data = data['euca:DeregisterComponentResponseType']
        data = data['euca:ConfigurationMessage']
        print 'RESPONSE %s' % data['euca:return']

def main(**args):
    req = DeregisterStorageController(**args)
    return req.send()


def main_cli():
    req = DeregisterStorageController()
    req.do_cli()
    
