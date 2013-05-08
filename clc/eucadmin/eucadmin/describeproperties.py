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

import eucadmin.describerequest

from boto.roboto.param import Param

class DescribeProperties(eucadmin.describerequest.DescribeRequest):
    ServiceName = 'Property'
    Description = "Show the cloud's properties or settings"

    Args = [Param(name='properties',
              long_name='property prefix',
              ptype='string',
              cardinality='+',
              optional=True,
              doc='[PROPERTY-PREFIX]*')]

    def __init__(self, **args):
        eucadmin.describerequest.DescribeRequest.__init__(self, **args)
        self.list_markers = ['euca:properties']
        self.item_markers = ['euca:item']

    def get_connection(self, **args):
        if self.connection is None:
            args['path'] = self.ServicePath
            self.connection = self.ServiceClass(**args)
        for i, value in enumerate(self.request_params.pop('properties'), []):
            self.request_params['Property.%s' % (i + 1)] = value
        return self.connection

    def cli_formatter(self, data):
        props = getattr(data, 'euca:properties')
        for prop in props:
            print 'PROPERTY\t%s\t%s' % (prop['euca:name'], prop['euca:value'])
