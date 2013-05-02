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
from eucadmin import EucAdmin
from boto.roboto.param import Param
import eucadmin

class DescribeVmTypes(AWSQueryRequest):
    ServicePath = '/services/Eucalyptus'
    APIVersion = 'eucalyptus'
    ServiceClass = EucAdmin
    Description = 'Describe the instance types which are available in the system.'
    Params = [
              Param(name='Verbose',
                    short_name='v',
                    long_name='verbose',
                    ptype='boolean',
                    default=False,
                    optional=True,
                    doc='Include extended information about the instance type definition.'),
              Param(name='Availability',
                    short_name='A',
                    long_name='availability',
                    ptype='boolean',
                    default=False,
                    optional=True,
                    doc='Include information about current instance type in the system.')
              ]
    
    Args = [Param(name='VmTypes',
              long_name='intance type name',
              ptype='string',
              cardinality=1,
              optional=True,
              doc='[[INSTANCETYPE]...]')]


    def __init__(self, **args):
      AWSQueryRequest.__init__(self, **args)
      self.list_markers = ['euca:vmTypeDetails']
      self.item_markers = ['euca:item']

    def get_connection(self, **args):
        if self.connection is None:
            args['path'] = self.ServicePath
            self.connection = self.ServiceClass(**args)
        return self.connection

    def cli_formatter(self, data):
#        print data
        vmtypes = getattr(data, 'euca:vmTypeDetails')
        fmt = 'TYPE\t%-20.20s%-10d%-10d%-10d'
        detail_fmt = '%s%06d / %06d %s'
        for vmtype in vmtypes:
            availability = vmtype.get('euca:availability')
            if availability:
              availability_item = availability.get('euca:item')
              print detail_fmt % ((fmt % (vmtype['euca:name'],
                                   int(vmtype['euca:cpu']),
                                   int(vmtype['euca:disk']),
                                   int(vmtype['euca:memory']))),
                                  int(availability_item['euca:available']),
                                  int(availability_item['euca:max']),
                                  availability_item['euca:zoneName'])
            else:
              print fmt % (vmtype['euca:name'],
                           int(vmtype['euca:cpu']),
                           int(vmtype['euca:disk']),
                           int(vmtype['euca:memory']))




    def main(self, **args):
        return self.send(**args)

    def main_cli(self):
        eucadmin.print_version_if_necessary()
        self.do_cli()
