# Copyright 2015 Eucalyptus Systems, Inc.
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

from requestbuilder import Arg, Filter

from eucalyptus_admin.commands.empyrean import EmpyreanRequest
from eucalyptus_admin.commands.mixins import TableOutputMixin


class DescribeServices(EmpyreanRequest, TableOutputMixin):
    DESCRIPTION = "List the cloud's services"
    ARGS = [Arg('ServiceName', metavar='SVCINSTANCE', nargs='*',
                help='limit results to specific instances of services'),
            Arg('-a', '--all', dest='ListAll', action='store_true',
                help='show all services regardless of type'),
            Arg('--show-events', action='store_true', dest='ShowEvents'),
            Arg('--expert', action='store_true', route_to=None,
                help='show advanced information')]
    # XXX:  Old options not implemented in this tool include:
    #  * ByHost (to be replaced by filters)
    #  * ByPartition (to be replaced by filters)
    #  * ByServiceType (to be replaced by filters)
    #  * ByState (to be replaced by filters)
    #  * ListInternal (to be replaced by filters)
    #  * ListUserServices (to be replaced by filters)
    #  * ShowEvents (events to be split into different actions)
    #  * ShowEventStacks (events to be split into different actions)
    # TODO:  These filters currently don't work (See EUCA-10639)
    FILTERS = [Filter('availability-zone', help='''availability zone in which
                      the instance of the service participates'''),
               Filter('is-service-group', help='''whether the instance of
                      the service is a service group'''),
               Filter('name', help='the name of the service instance'),
               Filter('service-group-member', help='''whether the instance of
                      the service is a member of a specific service group'''),
               Filter('service-type'),
               Filter('state')]
    LIST_TAGS = ['serviceStatuses', 'uris']

    def print_result(self, result):
        services = result.get('serviceStatuses') or []
        if self.args.get('expert'):
            table = self.get_table(('SERVICE', 'arn', 'state', 'epoch', 'uri'))
            table.sortby = 'arn'
            for service in services:
                svcid = service.get('serviceId') or {}
                table.add_row(('SERVICE', svcid.get('fullName'),
                               service.get('localState').lower(),
                               service.get('localEpoch'), svcid.get('uri')))
        else:
            table = self.get_table(('SERVICE', 'type', 'zone', 'name', 'state'))
            table.sortby = 'type'
            for service in services:
                svcid = service.get('serviceId') or {}
                table.add_row(('SERVICE', svcid.get('type'),
                               svcid.get('partition'), svcid.get('name'),
                               service.get('localState').lower()))
        print table
