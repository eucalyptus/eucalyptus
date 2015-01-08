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

import boto.exception
from boto.jsonresponse import Element
from boto.roboto.awsqueryrequest import AWSQueryRequest, RequiredParamError
from boto.roboto.param import Param
import boto.exception
import sys

import eucadmin
from eucadmin.describeservices import DescribeServices
from eucadmin import EucaFormat

class DeregisterService(AWSQueryRequest):
    ServiceName = ''
    ServicePath = '/services/Empyrean'
    ServiceClass = eucadmin.EucAdmin

    @property
    def Description(self):
        return 'De-register a ' + self.ServiceName

    Params = [Param(name='Type',
                    short_name='T',
                    long_name='type',
                    ptype='string',
                    optional=True,
                    doc='Type of service to be deregistered.')]
    Args = [Param(name='Name',
                  long_name='name',
                  ptype='string',
                  optional=False,
                  doc='Name of the service to deregister.')]

    def __init__(self, **args):
        AWSQueryRequest.__init__(self, **args)
        self.list_markers = ['euca:deregisteredServices', 'euca:statusMessages']
        self.item_markers = ['euca:item', 'euca:item']


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
            msg = 'deregistration failed'
            msg = '\nERROR\t'.join([msg] + [e.get('euca:entry') for e in getattr(data, 'euca:statusMessages', {})])
            raise RuntimeError('error: ' + msg)
        services = getattr(data, 'euca:deregisteredServices')
        fmt = '\t'.join(EucaFormat.PREFIX_LINE + EucaFormat.FULLNAME)
        for service_id in services:
            print fmt % ('SERVICE',
                         service_id.get('euca:type'),
                         service_id.get('euca:partition'),
                         service_id.get('euca:name'),
                         service_id.get('euca:fullName'))

    def main(self, **args):
        if 'partition' in args:
            args.pop('partition')
        if 'partition' in self.args:
            self.args.pop('partition')
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


class RegisterService(AWSQueryRequest):
    __metaclass__ = FixPortMetaClass

    ServiceName = ''
    ServicePath = '/services/Empyrean'
    ServiceClass = eucadmin.EucAdmin
    DefaultPort = 8773

    Params = [
        Param(name='Type',
              short_name='T',
              long_name='type',
              ptype='string',
              optional=False,
              doc='The type of the service to register.'),
        Param(name='Partition',
              short_name='P',
              long_name='partition',
              ptype='string',
              optional=False,
              doc='The partition where the service should be registered.'),
        Param(name='Host',
              short_name='H',
              long_name='host',
              ptype='string',
              optional=False,
              doc="The IP address of the host on which to register the service"),
        Param(name='Port',
              short_name='p',
              long_name='port',
              ptype='integer',
              default=DefaultPort,
              optional=True,
              doc="The new services's port number (default: %d)" % DefaultPort)
    ]
    Args = [Param(name='Name',
                  long_name='name',
                  ptype='string',
                  optional=False,
                  doc="service's name (must be unique)")]

    def __init__(self, **args):
        self._update_port()
        AWSQueryRequest.__init__(self, **args)
        self.list_markers = ['euca:registeredServices', 'euca:statusMessages', 'euca:serviceGroups',
                             'euca:serviceGroupMembers']
        self.item_markers = ['euca:item', 'euca:item', 'euca:item', 'euca:item']


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
            msg = 'registration failed'
            msg = '\nERROR\t'.join([msg] + [e.get('euca:entry') for e in getattr(data, 'euca:statusMessages', {})])
            raise RuntimeError('error: ' + msg)
        if len(self.new_partitions) == 1:
            print >> sys.stderr, 'Created new partition \'{0}\''.format(
                iter(self.new_partitions).next())
        services = getattr(data, 'euca:registeredServices')
        fmt = '\t'.join(EucaFormat.PREFIX_LINE + EucaFormat.URI + EucaFormat.FULLNAME)
        for service_id in services:
            print fmt % ('SERVICE',
                         service_id.get('euca:type'),
                         service_id.get('euca:partition'),
                         service_id.get('euca:name'),
                         service_id.get('euca:uri'),
                         service_id.get('euca:fullName'))

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
        partitions_after = self.get_partitions()

        # Also save this info for later printing
        self.new_partitions = partitions_after - partitions_before
        name = self.args.get('name') or args.get('name')
        #GRZE:RESTORE? self.service_info = self.get_service_info(name)

        return response

    def get_service_info(self, name, debug=0):
        obj = DescribeServices()
        response = obj.main(debug=debug)
        statuses = (response.get('euca:DescribeServicesResponseType', {})
                    .get('euca:serviceStatuses', []))
        for status in statuses:
            svcname = status.get('euca:serviceId', {}).get('euca:name')
            if svcname == name:
                return status

    def get_partitions(self, debug=0):
        obj = DescribeServices()
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


class DescribeAvailableServiceTypes(AWSQueryRequest):
    ServiceName = ''
    ServicePath = '/services/Empyrean'
    ServiceClass = eucadmin.EucAdmin
    Description = 'Get available component types and their characteristics'
    Params = [
        Param(name='Verbose',
              short_name='v',
              long_name='verbose',
              ptype='boolean',
              default=False,
              optional=True,
              doc='Be verbose.')
    ]

    def __init__(self, **args):
        AWSQueryRequest.__init__(self, **args)
        self.list_markers = ['euca:available', 'euca:serviceGroups', 'euca:serviceGroupMembers']
        self.item_markers = ['euca:item', 'euca:item', 'euca:item']

    def get_connection(self, **args):
        if self.connection is None:
            args['path'] = self.ServicePath
            self.connection = self.ServiceClass(**args)
        return self.connection

    def cli_formatter(self, data):
        components = getattr(data, 'euca:available')
        for c in components:
            reg = c.get('euca:registerable', False) == "true"
            partitioned = c.get('euca:partitioned', False) == "true"
            print "SERVICE\t%-21.21s\t%s\t%s" % (
                c.get('euca:componentName', None),
                ",".join(
                    filter(lambda x: (len(x) > 1),
                           ["register" if reg else "",
                            "partitioned" if partitioned else "",
                            "modifiable" if reg else "",
                            "internal" if not reg or not partitioned else ""
                           ]),
                ),
                c.get('euca:description', None),
            )
        for c in components:
            sgMembers = ",".join(str(entry['euca:entry']) for entry in c.get('euca:serviceGroupMembers'))
            if len(sgMembers) > 0:
                print "SERVICEGROUP\t%-21.21s\t%s" % (c.get('euca:componentName', None), sgMembers)
        for c in components:
            sgs = ",".join(str(entry['euca:entry']) for entry in c.get('euca:serviceGroups'))
            if len(sgs) > 0:
                print "GROUPMEMBER\t%-21.21s\t%s" % (c.get('euca:componentName', None), sgs)

    def main(self, **args):
        return self.send(**args)

    def main_cli(self):
        eucadmin.print_version_if_necessary()
        self.do_cli()
