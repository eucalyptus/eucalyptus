# Copyright 2011-2014 Eucalyptus Systems, Inc.
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
from eucadmin import EucAdmin
from eucadmin.registerautoscaling import RegisterAutoScaling
from eucadmin.registercloudwatch import RegisterCloudWatch
from eucadmin.registercompute import RegisterCompute
from eucadmin.registereuare import RegisterEuare
from eucadmin.registerloadbalancing import RegisterLoadBalancing
from eucadmin.registerobjectstoragegateway import RegisterObjectStorageGateway
from eucadmin.registertokens import RegisterTokens

class RegisterUserServices(AWSQueryRequest):
    ServiceClass = EucAdmin
    Description = ("Register all user facing services.")
    DefaultPort = 8773

    Params = [
              Param(name='Host',
                    short_name='H',
                    long_name='host',
                    ptype='string',
                    optional=False,
                    doc="IP address for the components"),
              Param(name='Port',
                    short_name='p',
                    long_name='port',
                    ptype='integer',
                    default=DefaultPort,
                    optional=True,
                    doc="Port number for the components (default: %d)" % DefaultPort)
              ]
    Args = [Param(name='NameSuffix',
                  long_name='name-suffix',
                  ptype='string',
                  optional=False,
                  doc='Component name suffix (must be unique)')]

    def register_services(self):
        self.suffix = self.args.pop('name_suffix')
        self.register( 'AS',  'autoscaling',   RegisterAutoScaling() )
        self.register( 'CW',  'cloudwatch',    RegisterCloudWatch() )
        self.register( 'COM', 'compute',       RegisterCompute() )
        self.register( 'EUA', 'euare',         RegisterEuare() )
        self.register( 'LB',  'loadbalancing', RegisterLoadBalancing() )
        self.register( 'TOK', 'tokens',        RegisterTokens() )
        self.register( 'OSG', 'objectstorage', RegisterObjectStorageGateway() ) # Last as it may output a warning

    def register(self, prefix, partition, register_request):
        self.args['name'] = '%s_%s' % (prefix, self.suffix)
        self.args['partition'] = partition
        data = register_request.main(**self.args)
        register_request.cli_formatter(data)

    def main(self, **args):
        self.args.update(args)
        self.process_args()
        self.register_services()

    def main_cli(self):
        eucadmin.print_version_if_necessary()
        self.do_cli()