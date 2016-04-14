# Copyright 2013-2014 Eucalyptus Systems, Inc.
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
from eucadmin.modifyservice import ModifyService

class MigrateInstances(AWSQueryRequest):
    ServiceClass = eucadmin.EC2
    ServicePath = '/services/compute'
    Description = ('Migrate one instance from its current host, or migrate '
                   'all instances off a specific host.  Either a single '
                   'instance or a single source host is required.')
    Params = [Param(name='SourceHost', long_name='source',
                    ptype='string', optional=True,
                    doc=('remove all instances from a specific host')),
              Param(name='InstanceId', short_name='i', long_name='instance',
                    ptype='string', optional=True,
                    doc='migrate a specific instance'),
              Param(name='dest', long_name='dest', ptype='string',
                    cardinality='*', request_param=False, doc=('migrate to '
                    'a specific host (may be used more than once)')),
              Param(name='exclude_dest', long_name='exclude-dest',
                    ptype='string', cardinality='*', request_param=False,
                    doc=('migrate to any host except this one (may be used '
                    'more than once)')),
              Param(name='stop_source', long_name='stop-source',
                    ptype='boolean', default=False, request_param=False,
                    doc=('also stop the source node controller to prevent new '
                         'instances from running on it (requires --source)'))]

    def cli_formatter(self, data):
        response = getattr(data, 'euca:_return')
        if response != 'true':
            # Failed responses still return HTTP 200, so we raise exceptions
            # manually.  See http://eucalyptus.atlassian.net/browse/EUCA-5269
            msg = getattr(data, 'euca:statusMessage',
                          'failed to start migration')
            raise RuntimeError('error: ' + msg)

    def main(self, **args):
        if self.args.get('source') and self.args.get('instance'):
            raise ValueError('error: argument -i/--instance: not allowed '
                             'with --source')
        if not self.args.get('source') and not self.args.get('instance'):
            raise ValueError('error: one of the arguments -i/--instance '
                             '--source is required')
        if self.args.get('dest'):
            if self.args.get('exclude_dest'):
                raise ValueError('error: argument --dest: not allowed '
                                 'with --exclude-dest')
            self.request_params['AllowHosts'] = 'true'
            for i, host in enumerate(self.args['dest'], 1):
                self.request_params['DestinationHost.{0}'.format(i)] = host
        elif self.args.get('exclude_dest'):
            self.request_params['AllowHosts'] = 'false'
            for i, host in enumerate(self.args['exclude_dest'], 1):
                self.request_params['DestinationHost.{0}'.format(i)] = host

        if self.args['stop_source']:
            source = self.args.get('source')
            if source is None:
                raise ValueError('error: argument --stop-source: only valid '
                                 'with --source, not -i/--instance')
            obj = ModifyService(debug=self.args.get('debug'))
            obj.main(name=self.args['source'], state='STOP')

        return self.send(**args)

    def main_cli(self):
        eucadmin.print_version_if_necessary()
        self.do_cli()
