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

class RegisterStorageController(eucadmin.registerrequest.RegisterRequest):
    ServiceName = 'Storage Controller'
    Description = 'Register a StorageController service.'

    def main(self, **args):
        debug = self.args.get('debug', False) or args.get('debug')

        # Roboto weirdness results in self.args *always* containing a
        # 'partition' key, so we need to check its value separately.
        self.partition = self.args.get('partition') or args.get('partition')
        if self.partition is None:
            sys.exit('error: argument --partition is required')

        self.sc_preexists = self.check_for_extant_storage(self.partition,
                                                          debug)

        return eucadmin.registerrequest.RegisterRequest.main(self, **args)

    def check_for_extant_storage(self, partition, debug=False):
        obj = describeservices.DescribeServices()
        response = obj.main(filter_partition=partition, filter_type='storage',
                            debug=debug)
        statuses = (response.get('euca:DescribeServicesResponseType', {})
                            .get('euca:serviceStatuses', []))
        return len(statuses) > 0

    def cli_formatter(self, data):
        eucadmin.registerrequest.RegisterRequest.cli_formatter(self, data)
        if not self.sc_preexists:
            print >> sys.stderr, \
                    ('Registered the first storage controller in partition '
                     '\'{0}\'.  You must choose a storage back end with '
                     '``euca-modify-property -p {0}.storage.'
                     'blockstoragemanager=$BACKEND\'\'').format(self.partition)
