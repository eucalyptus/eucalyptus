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
from . import describeservices
from . import EucadminRequest
import boto.exception
import eucadmin
import sys

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

class RegisterRequest(EucadminRequest):

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
                    doc='partition the component should join'),
              Param(name='Host',
                    short_name='H',
                    long_name='host',
                    ptype='string',
                    optional=False,
                    doc="new component's IP address"),
              Param(name='Port',
                    short_name='p',
                    long_name='port',
                    ptype='integer',
                    default=DefaultPort,
                    optional=True,
                    doc="new component's port number (default: %d)" % DefaultPort)
              ]
    Args = [Param(name='Name',
                  long_name='name',
                  ptype='string',
                  optional=False,
                  doc='component name (must be unique)')]

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
            port_param.doc = "new component's port number (default: %d)" % self.DefaultPort

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
            msg = getattr(data, 'euca:statusMessage', 'registration failed')
            raise RuntimeError('error: ' + msg)
        if len(self.new_partitions) == 1:
            print >> sys.stderr, 'Created new partition \'{0}\''.format(
                    iter(self.new_partitions).next())

        # Mostly copypasted from describeservices.py.  Make sure to refactor
        # this during the rewrite.
        fmt = 'SERVICE\t%-15.15s\t%-15s\t%-15s\t%-10s\t%-4s\t%-40s\t%s'
        if self.service_info:
            service_id = self.service_info['euca:serviceId']
            print fmt % (service_id.get('euca:type'),
                         service_id.get('euca:partition'),
                         service_id.get('euca:name'),
                         self.service_info.get('euca:localState'),
                         self.service_info.get('euca:localEpoch'),
                         service_id.get('euca:uri'),
                         service_id.get('euca:fullName'))
        else:
            print 'Registration ok; no service information is available yet'

    def main(self, **args):
        # We have to handle debug specially because AWSQueryRequest.send adds
        # another log stream handler every time its gets debug=2, resulting in
        # duplicate log messages' going to the console when debugging is
        # enabled.
        if 'debug' in args:
            debug = args.pop('debug')
            self.args.pop('debug', None)
        else:
            debug = self.args.pop('debug', 0)

        # Roboto weirdness results in self.args *always* containing a
        # 'partition' key, so we need to check its value separately.
        partition = self.args.get('partition') or args.get('partition')
        if partition is None:
            sys.exit('error: argument --partition is required')

        partitions_before = self.get_partitions(debug=debug)
        response = self.send(**args)
        partitions_after  = self.get_partitions()

        # Also save this info for later printing
        self.new_partitions = partitions_after - partitions_before
        name = self.args.get('name') or args.get('name')
        self.service_info = self.get_service_info(name)

        return response

    def get_service_info(self, name, debug=0):
        obj = describeservices.DescribeServices()
        response = obj.main(debug=debug)
        statuses = (response.get('euca:DescribeServicesResponseType', {})
                            .get('euca:serviceStatuses', []))
        for status in statuses:
            svcname = status.get('euca:serviceId', {}).get('euca:name')
            if svcname == name:
                return status

    def get_partitions(self, debug=0):
        obj = describeservices.DescribeServices()
        response = obj.main(debug=debug)
        statuses = (response.get('euca:DescribeServicesResponseType', {})
                            .get('euca:serviceStatuses', []))
        partitions = set()
        for status in statuses:
            partition = status.get('euca:serviceId', {}).get('euca:partition')
            partitions.add(partition)
        return partitions

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
