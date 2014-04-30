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
import re

from boto.roboto.awsqueryrequest import AWSQueryRequest
from . import EucadminRequest, EucaFormat
from boto.roboto.param import Param
import sys
import eucadmin
import os

class DescribeServices(EucadminRequest):

    ServicePath = '/services/Empyrean'
    ServiceClass = eucadmin.EucAdmin
    Description = 'Get services'
    Params = [
              Param(name='ListAll',
                    short_name='A',
                    long_name='all',
                    ptype='boolean',
                    default=False,
                    optional=True,
                    doc='Include all public service information.  Reported state information is determined by the view available to the target host, which should be treated as advisory (See documentation for guidance on interpreting this information).'),
              Param(name='ListInternal',
                    short_name=None,
                    long_name='system-internal',
                    ptype='boolean',
                    default=False,
                    optional=True,
                    doc='Include internal services information (note: this information is only for the target host).'),
              Param(name='ListUserServices',
                    short_name=None,
                    long_name='user-services',
                    ptype='boolean',
                    default=False,
                    optional=True,
                    doc='Include services which are user facing and are co-located with some other top-level service (note: this information is only for the target host).'),
              Param(name='ByServiceType',
                    short_name='T',
                    long_name='filter-type',
                    ptype='string',
                    optional=True,
                    doc='Filter services by component type.'),
              Param(name='ByHost',
                    short_name='H',
                    long_name='filter-host',
                    ptype='string',
                    optional=True,
                    doc='Filter services by host.'),
              Param(name='ByState',
                    short_name='F',
                    long_name='filter-state',
                    ptype='string',
                    optional=True,
                    doc='Filter services by state.'),
              Param(name='ByPartition',
                    short_name='P',
                    long_name='filter-partition',
                    ptype='string',
                    optional=True,
                    doc='Filter services by partition.'),
              Param(name='ShowEvents',
                    short_name='E',
                    long_name='events',
                    ptype='boolean',
                    default=False,
                    optional=True,
                    doc='Show service event details.'),
              Param(name='ShowEventStacks',
                    long_name='events-verbose',
                    ptype='boolean',
                    default=False,
                    optional=True,
                    doc='Show verbose service event details.')
              ]

    Args = [Param(name='service_names',
              long_name='service-name',
              ptype='string',
              cardinality='*',
              optional=True,
              doc='[SERVICE-NAME]*')]

    def __init__(self, **args):
      AWSQueryRequest.__init__(self, **args)
      self.list_markers = ['euca:serviceStatuses', 'euca:statusDetails']
      self.item_markers = ['euca:item', 'euca:item']

    def get_connection(self, **args):
        if self.connection is None:
            args['path'] = self.ServicePath
            self.connection = self.ServiceClass(**args)
        if 'service_names' in self.request_params:
          for i,type in enumerate(self.request_params.pop('service_names')):
            self.request_params['ServiceName.%s'%(i+1)]=type
        return self.connection

    def cli_formatter(self, data):
        services = getattr(data, 'euca:serviceStatuses')
        fmt = '\t'.join(EucaFormat.PREFIX_LINE + EucaFormat.STATE + ['%-4s'] + EucaFormat.URI + EucaFormat.FULLNAME)

        detail_fmt = 'SERVICEEVENT\t%-36.36s\t%s'
        for s in services:
            service_id = s['euca:serviceId']
            print fmt % ('SERVICE',
                         service_id.get('euca:type'),
                         service_id.get('euca:partition'),
                         service_id.get('euca:name'),
                         s.get('euca:localState'),
                         s.get('euca:localEpoch'),
                         service_id.get('euca:uri'),
                         service_id.get('euca:fullName'))
            for detail_item in s.get('euca:statusDetails', []):
                fullname = detail_item.get('euca:serviceFullName').replace('/', '')
                [arn, ns, parent, partition, servicetype, servicename] = re.split(':', fullname)
                dfmt = '\t'.join(EucaFormat.PREFIX_LINE + ['%s'])
                print >> sys.stderr, dfmt % (detail_item.get('euca:severity'),
                                            servicetype,
                                            partition if (len(partition) > 1) else service_id.get('euca:partition'),
                                            servicename,
                                            detail_item.get('euca:message', ''))
                # GRZE: Would be nice to get rid of this output
                print detail_fmt % (detail_item.get('euca:uuid'),
                                    detail_item.get('euca:serviceFullName'))
                print detail_fmt % (detail_item.get('euca:uuid'),
                                    detail_item.get('euca:severity'))
                print detail_fmt % (detail_item.get('euca:uuid'),
                                    detail_item.get('euca:timestamp'))
                print detail_fmt % (detail_item.get('euca:uuid'),
                                    detail_item.get('euca:message'))
                if detail_item.get('euca:stackTrace'):
                    print detail_item['euca:stackTrace']

    def main(self, **args):
        return self.send(**args)

    def main_cli(self):
        eucadmin.print_version_if_necessary()
        self.do_cli()
