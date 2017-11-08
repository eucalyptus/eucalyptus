# Copyright 2015 Ent. Services Development Corporation LP
#
# Redistribution and use of this software in source and binary forms,
# with or without modification, are permitted provided that the
# following conditions are met:
#
#   Redistributions of source code must retain the above copyright
#   notice, this list of conditions and the following disclaimer.
#
#   Redistributions in binary form must reproduce the above copyright
#   notice, this list of conditions and the following disclaimer
#   in the documentation and/or other materials provided with the
#   distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

from requestbuilder import Arg
from requestbuilder.mixins.formatting import TableOutputMixin

from eucalyptus_admin.commands.bootstrap import BootstrapRequest


class DescribeAvailableServiceTypes(BootstrapRequest, TableOutputMixin):
    DESCRIPTION = 'List available service types'
    ARGS = [Arg('-a', '--all', dest='show_all', action='store_true',
                route_to=None,
                help='show all service types regardless of their properties')]
    LIST_TAGS = ['available', 'serviceGroups', 'serviceGroupMembers']

    def print_result(self, result):
        svctypes = result.get('available') or []
        table = self.get_table(('SVCTYPE', 'type', 'groups', 'description'))
        table.sortby = 'type'
        for svctype in svctypes:
            if (svctype.get('registerable', '').lower() != 'false' or
                    self.args.get('all')):
                parent_groups = [item['entry'] for item in
                                 svctype.get('serviceGroups') or {}]
                table.add_row(('SVCTYPE', svctype.get('componentName'),
                               ','.join(sorted(parent_groups)),
                               svctype.get('description')))
        print table
