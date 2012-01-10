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

class FixPortMetaClass(type):
    """
    I generally avoid metaclasses.  They can be confusing.
    However, I wanted to find a clean way to allow the
    DefaultPort class attribute to be "overridden" and
    have the new value make it's way into Port parameter
    in a seamless way.  This is the best I could come up with.
    """

    def __new__(cls, name, bases, attrs):
        if 'DefaultPort' in attrs:
            for base in bases:
                if hasattr(base, 'Params'):
                    for param in getattr(base, 'Params'):
                        if param.name == 'Port':
                            port = attrs['DefaultPort']
                            param.default = port
                            param.doc = 'Port for service (default=%d)' % port
        return type.__new__(cls, name, bases, attrs)

class RegisterRequest(AWSQueryRequest):

    __metaclass__ = FixPortMetaClass

    ServiceName = ''
    ServicePath = '/services/Configuration'
    ServiceClass = eucadmin.EucAdmin
    DefaultPort = 8773

    Params = [
              Param(name='Partition',
                    short_name='P',
                    long_name='partition',
                    ptype='string',
                    optional=False,
                    doc='Partition name for the service'),
              Param(name='Host',
                    short_name='H',
                    long_name='host',
                    ptype='string',
                    optional=False,
                    doc='Hostname of the service'),
              Param(name='Port',
                    short_name='p',
                    long_name='port',
                    ptype='integer',
                    default=DefaultPort,
                    optional=True,
                    doc='Port for the service (Default: %d)' % DefaultPort)
              ]
    Args = [Param(name='Name',
                  long_name='name',
                  ptype='string',
                  optional=False,
                  doc='The name of the service')]

    def __init__(self, **args):
        self._update_port()
        AWSQueryRequest.__init__(self, **args)

    def _update_port(self):
        port_param = None
        for param in self.Params:
            if param.name == 'Port':
                port_param = param
                break
        if port_param:
            port_param.default = self.DefaultPort
            port_param.doc = 'Port for the service (default=%d)' % self.DefaultPort

    def get_connection(self, **args):
        if self.connection is None:
            args['path'] = self.ServicePath
            self.connection = self.ServiceClass(**args)
        return self.connection

    def cli_formatter(self, data):
        response = getattr(data, 'euca:_return')
        print 'RESPONSE %s' % response

    def main(self, **args):
        return self.send(**args)

    def main_cli(self):
        eucadmin.print_version_if_necessary()
        self.do_cli()
