# Copyright (c) 2011, Eucalyptus Systems, Inc.
# All rights reserved.
#
# Redistribution and use of this software in source and binary forms, with or
# without modification, are permitted provided that the following conditions
# are met:
#
#   Redistributions of source code must retain the above
#   copyright notice, this list of conditions and the
#   following disclaimer.
#
#   Redistributions in binary form must reproduce the above
#   copyright notice, this list of conditions and the
#   following disclaimer in the documentation and/or other
#   materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
# Author: Mitch Garnaat mgarnaat@eucalyptus.com

import eucadmin.describerequest

class DescribeNodes(eucadmin.describerequest.DescribeRequest):

    ServiceName = 'Node'
    Description = 'List Node controllers.'

    def __init__(self, **kwargs):
        eucadmin.describerequest.DescribeRequest.__init__(self, **kwargs)
        self.list_markers.append('euca:instances')

    def cli_formatter(self, data):
        nodes = getattr(data, 'euca:registered')
        for node in nodes:
            instances = node.get('euca:instances')
            if isinstance(instances, list):
                instance_ids = [instance.get('euca:entry') for instance in
                                node.get('euca:instances', [])]
            else:
                # If boto's XML parsing bug hasn't been fixed then instances
                # will be a dict with only one or zero instances per node.
                instance_ids = []
            fmt = ['NODE', node.get('euca:name'), node.get('euca:clusterName')]
            fmt.extend(instance_ids)
            print '\t'.join(map(str, fmt))

