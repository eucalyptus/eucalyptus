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

import eucadmin.registerrequest
import sys
from . import describeservices

class RegisterObjectStorageGateway(eucadmin.registerrequest.RegisterRequest):
    ServiceName = 'ObjectStorageGateway'
    Description = 'Register a new Object Storage Gateway.'

    def main(self, **args):
        debug = self.args.get('debug', False) or args.get('debug')

        self.osg_preexists = self.check_for_extant_osg(debug)

        return eucadmin.registerrequest.RegisterRequest.main(self, **args)

    def check_for_extant_osg(self, debug=False):
        obj = describeservices.DescribeServices()
        response = obj.main(filter_partition='objectstorage', filter_type='objectstorage',
                            debug=debug)
        statuses = (response.get('euca:DescribeServicesResponseType', {})
                            .get('euca:serviceStatuses', []))
        return len(statuses) > 0

    def cli_formatter(self, data):
        eucadmin.registerrequest.RegisterRequest.cli_formatter(self, data)
        if not self.osg_preexists:
            print >> sys.stderr, \
                    ('Registered the first Object Storage Gateway. '
                     'You must choose a object storage back end client with '
                     '``euca-modify-property -p objectstorage.providerclient=$PROVIDER '
                     '(e.g. walrus or s3)')
