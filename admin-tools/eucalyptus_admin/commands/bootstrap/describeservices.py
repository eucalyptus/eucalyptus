# Copyright (c) 2015-2016 Hewlett Packard Enterprise Development LP
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

import argparse
import sys

from requestbuilder import Arg, Filter, MutuallyExclusiveArgList
from requestbuilder.mixins.formatting import TableOutputMixin

from eucalyptus_admin.commands.bootstrap import BootstrapRequest


class _RenamingFilter(Filter):
    def __init__(self, name, server_name, **kwargs):
        Filter.__init__(self, name, **kwargs)
        self.__server_name = server_name

    def convert(self, argval):
        _, value = Filter.convert(self, argval)
        return (self.__server_name, value)


class DescribeServices(BootstrapRequest, TableOutputMixin):
    DESCRIPTION = "Show information about the cloud's services"
    ARGS = [Arg('ServiceName', metavar='SVCINSTANCE', nargs='*',
                help='limit results to specific instances of services'),
            Arg('-a', '--all', dest='ListAll', action='store_true',
                help='show all services regardless of type'),
            Arg('--show-events', dest='ShowEvents', action='store_true',
                help=argparse.SUPPRESS),
            Arg('--show-stack-traces', dest='ShowEventStacks',
                action='store_true', help=argparse.SUPPRESS),
            MutuallyExclusiveArgList(
                Arg('--group-by-type', action='store_true', route_to=None,
                    help='collate services by service type (default)'),
                Arg('--group-by-zone', action='store_true', route_to=None,
                    help='collate services by availability zone'),
                Arg('--group-by-host', action='store_true', route_to=None,
                    help='collate services by host'),
                Arg('--expert', action='store_true', route_to=None,
                    help=('show advanced information, including service '
                          'accounts')))]
    FILTERS = [_RenamingFilter('availability-zone', 'partition',
                               help="the service's availability zone"),
               Filter('host', help='the machine running the service'),
               Filter('internal', help='''whether the service is used
                      only internally (true or false)'''),
               Filter('public',
                      help='whether the service is public (true or false)'),
               Filter('service-group', help='''whether the service is a
                      member of a specific service group'''),
               Filter('service-group-member',
                      help='''whether the service is a member of any
                      service group (true or false)'''),
               Filter('service-type', help='the type of service'),
               Filter('state', help="the service's state")]
               # A "user-service" filter used to appear here, but it was
               # a mirror of an option with that name in the old admin
               # tool set that doesn't match what that term means today,
               # so we removed it at least for now.  See
               # https://eucalyptus.atlassian.net/browse/EUCA-11006
    LIST_TAGS = ['serviceAccounts', 'serviceStatuses', 'statusDetails', 'uris']

    def print_result(self, result):
        services = result.get('serviceStatuses') or []
        if self.args.get('expert'):
            table = self.get_table(('SERVICE', 'arn', 'state', 'epoch', 'uri', 'accounts'))
            table.sortby = 'arn'
            for service in services:
                svcid = service.get('serviceId') or {}
                accounts = []
                for account in service.get('serviceAccounts') or []:
                    accounts.append('{0}={1}:{2}'.format(
                        account.get('accountName'),
                        account.get('accountNumber'),
                        account.get('accountCanonicalId')))
                table.add_row((
                    'SERVICE', svcid.get('fullName'),
                    _colorize_state(service.get('localState').lower()),
                    service.get('localEpoch'), svcid.get('uri'),
                    ','.join(sorted(accounts))))
        elif self.args.get('group_by_host'):
            hosts = {}
            for service in services:
                svcid = service.get('serviceId') or {}
                hosts.setdefault(svcid.get('host'), [])
                hosts[svcid.get('host')].append(service)
            table = self.get_table(('SERVICE', 'host', 'type', 'name', 'state'))
            for host, services in sorted(hosts.iteritems()):
                host = _colorize(host, _get_service_list_color(services))
                for service in services:
                    svcid = service.get('serviceId') or {}
                    table.add_row((
                        'SERVICE', host, svcid.get('type'), svcid.get('name'),
                        _colorize_state(service.get('localState').lower())))
        elif self.args.get('group_by_zone'):
            by_zone = {}
            for service in services:
                svcid = service.get('serviceId') or {}
                by_zone.setdefault(svcid.get('partition'), {})
                by_zone[svcid.get('partition')].setdefault(
                    svcid.get('type'), [])
                by_zone[svcid.get('partition')][svcid.get('type')].append(
                    service)
            table = self.get_table(('SERVICE', 'zone', 'type', 'name', 'state'))
            for zone, svctypes in sorted(by_zone.iteritems()):
                if not zone:
                    continue
                zone_color = 'green'
                for svctype, services in sorted(svctypes.iteritems()):
                    svctype_color = _get_service_list_color(services)
                    if svctype_color == 'red':
                        zone_color = 'red'
                    elif svctype_color == 'brown' and zone_color == 'green':
                        zone_color = 'brown'
                zone = _colorize(zone, zone_color)
                for svctype, services in sorted(svctypes.iteritems()):
                    svctype = _colorize(svctype,
                                        _get_service_list_color(services))
                    for service in services:
                        svcid = service.get('serviceId') or {}
                        state = service.get('localState').lower()
                        table.add_row((
                            'SERVICE', zone, svctype, svcid.get('name'),
                            _colorize_state(state)))
        # TODO:  group_by_group (needs EUCA-10816)
        # https://eucalyptus.atlassian.net/browse/EUCA-10816
        else:  # group_by_type
            svctypes = {}
            for service in services:
                svcid = service.get('serviceId') or {}
                svctypes.setdefault(svcid.get('type'), [])
                svctypes[svcid.get('type')].append(service)
            table = self.get_table(('SERVICE', 'type', 'zone', 'name', 'state'))
            for svctype, services in sorted(svctypes.iteritems()):
                svctype = _colorize(svctype, _get_service_list_color(services))
                for service in services:
                    svcid = service.get('serviceId') or {}
                    table.add_row((
                        'SERVICE', svctype, svcid.get('partition'),
                        svcid.get('name'),
                        _colorize_state(service.get('localState').lower())))
        print table


def _colorize(str_, color):
    COLORS = dict(none=(0, 0), black=(0, 30), red=(0, 31), green=(0, 32),
                  brown=(0, 33), blue=(0, 34), purple=(0, 35), cyan=(0, 36),
                  lightgray=(0, 37))
    if sys.stdout.isatty() and str_:
        return '\033[{0};{1}m{2}\033[0m'.format(COLORS[color][0], COLORS[color][1], str_)
    else:
        return str_


def _colorize_state(state):
    if state.lower() == 'enabled':
        return _colorize(state, 'green')
    elif state.lower() == 'disabled':
        return _colorize(state, 'cyan')
    elif state.lower() == 'stopped':
        return _colorize(state, 'blue')
    elif state.lower() in ('broken', 'notready'):
        return _colorize(state, 'red')
    return state


def _get_service_list_color(services):
    if all(service.get('localState').lower() in
           ('enabled', 'disabled') for service in services):
        return 'green'
    elif all(service.get('localState').lower() in
             ('notready', 'broken', 'stopped') for service in services):
        return 'red'
    return 'brown'
