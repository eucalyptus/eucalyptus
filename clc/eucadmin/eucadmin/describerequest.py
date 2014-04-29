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
import eucadmin


class DescribeRequest(AWSQueryRequest):
    ServiceName = ''
    ServicePath = '/services/Empyrean'
    ServiceClass = eucadmin.EucAdmin

    def __init__(self, **args):
        AWSQueryRequest.__init__(self, **args)
        self.list_markers = ['euca:registered']
        self.item_markers = ['euca:item']

    def get_connection(self, **args):
        if self.connection is None:
            args['path'] = self.ServicePath
            self.connection = self.ServiceClass(**args)
        return self.connection

    def cli_formatter(self, data):
        services = getattr(data, 'euca:registered')
        fields = {'euca:partition' : 15,
                  'euca:name' : 15,
                  'euca:hostName' : 25}
        for s in services:
            if s.get('euca:hostName', None) != 'detail':
                for field_name in fields:
                    if len(s.get(field_name)) > fields[field_name]:
                        fields[field_name] = len(s.get(field_name))
        fmt = '%%s\t%%-%s.%ss\t%%-%d.%ds\t%%-%ds\t%%s\t%%s' % (fields['euca:partition'],
                                                               fields['euca:partition'],
                                                               fields['euca:name'],
                                                               fields['euca:name'],
                                                               fields['euca:hostName'])
        for s in services:
            if s.get('euca:hostName', None) != 'detail':
                print fmt % (
                    self.ServiceName.upper(),
                    s.get('euca:partition', None),
                    s.get('euca:name', None),
                    s.get('euca:hostName', None),
                    s.get('euca:state', None),
                    s.get('euca:detail', None))

    def main(self, **args):
        return self.send(**args)

    def main_cli(self):
        eucadmin.print_version_if_necessary()
        self.do_cli()
