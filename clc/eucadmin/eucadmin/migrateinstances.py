# Copyright 2011-2012 Eucalyptus Systems, Inc.
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the following
# conditions are met:
#
#   Redistributions of source code must retain the above copyright notice,
#   this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer in the
#   documentation and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

from boto.roboto.awsqueryrequest import AWSQueryRequest
from boto.roboto.param import Param
import eucadmin

class MigrateInstances(AWSQueryRequest):
    ServicePath = '/services/Eucalyptus'
    ServiceClass = eucadmin.EucAdmin
    Description = 'Migrate instances from a node'
    Params = [
              Param(name='SourceHost',
                    short_name='H',
                    long_name='sourceHost',
                    ptype='string',
                    optional=False,
                    doc='Hostname or IP of the ')
              ]

    def get_connection(self, **args):
        if self.connection is None:
            args['path'] = self.ServicePath
            self.connection = self.ServiceClass()
        return self.connection

    def cli_formatter(self, data):
        response = getattr(data, 'euca:_return')
        print 'RESPONSE %s' % response

    def main(self, **args):
        return self.send(**args)

    def main_cli(self):
        eucadmin.print_version_if_necessary()
        self.do_cli()
