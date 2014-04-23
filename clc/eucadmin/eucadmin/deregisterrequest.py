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

from boto.roboto.awsqueryrequest import AWSQueryRequest, RequiredParamError
from boto.roboto.param import Param
import boto.exception
import eucadmin

class DeregisterRequest(AWSQueryRequest):
    ServiceName = ''
    ServicePath = '/services/Empyrean'
    ServiceClass = eucadmin.EucAdmin

    @property
    def Description(self):
        return 'De-register a ' + self.ServiceName

    Params = [Param(name='Partition',
                    short_name='P',
                    long_name='partition',
                    ptype='string',
                    optional=True,
                    doc='partition in which the component participates')]
    Args = [Param(name='Name',
                  long_name='name',
                  ptype='string',
                  optional=False,
                  doc='component name')]

    def get_connection(self, **args):
        if self.connection is None:
            args['path'] = self.ServicePath
            self.connection = self.ServiceClass(**args)
        return self.connection

    def cli_formatter(self, data):
        response = getattr(data, 'euca:_return')
        if response != 'true':
            # Failed responses still return HTTP 200, so we raise exceptions
            # manually.  See https://eucalyptus.atlassian.net/browse/EUCA-3670.
            msg = getattr(data, 'euca:statusMessage', 'de-registration failed')
            raise RuntimeError('error: ' + msg)

    def main(self, **args):
        return self.send(**args)

    def main_cli(self):
        eucadmin.print_version_if_necessary()

        # EVIL HACK: When you fail to supply a name for the component,
        # roboto's error message instructs you to use the --name
        # option, which doesn't exist.  We can't simply call
        # self.do_cli and catch an exception either, because do_cli
        # will call sys.exit before we regain control.  As a last
        # resort, we monkey patch the RequiredParamError that it
        # would throw to special-case this error message.
        #
        # Don't worry too much about how horrible this is; we're
        # going to phase roboto out completely soon.
        RequiredParamError.__init__ = _required_param_error_init

        self.do_cli()

def _required_param_error_init(self, required):
    self.required = required
    s = 'Required parameters are missing: %s' % self.required.replace('--name', 'name')
    boto.exception.BotoClientError.__init__(self, s)
