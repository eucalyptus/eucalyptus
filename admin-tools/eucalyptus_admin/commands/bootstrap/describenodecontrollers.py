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

from eucalyptus_admin.commands.ec2.describeinstances import DescribeInstances
from eucalyptus_admin.commands.bootstrap import BootstrapRequest
from eucalyptus_admin.commands.bootstrap.describeservices import \
    DescribeServices


class DescribeNodeControllers(BootstrapRequest, TableOutputMixin):
    DESCRIPTION = "List the cloud's node controllers and their instances"
    ARGS = [Arg('--ec2-url', metavar='URL', route_to=None,
                help='compute service endpoint URL')]

    def configure(self):
        BootstrapRequest.configure(self)
        if not self.args.get('ec2_service'):
            self.args['ec2_service'] = \
                DescribeInstances.SERVICE_CLASS.from_other(
                    self.service, url=self.args.get('ec2_url'))

    def main(self):
        req = DescribeServices.from_other(self, ListAll=True)
        services = req.main().get('serviceStatuses') or []
        nodes = {}
        for service in services:
            svcid = service['serviceId']
            if svcid.get('type') == 'node':
                nodes[svcid.get('name')] = {
                    'zone': svcid.get('partition'),
                    'host': svcid.get('host'),
                    'state': service.get('localState'),
                    'details': service.get('details') or {},
                    'instances': {}}
        req = DescribeInstances.from_other(self, service=self.args['ec2_service'],
                                           InstanceId=['verbose'])
        reservations = req.main().get('reservationSet')
        for reservation in reservations:
            for instance in reservation.get('instancesSet') or []:
                inst_info = {}
                for tag in instance.get('tagSet') or []:
                    if tag['key'] == 'euca:node' and tag['value'] in nodes:
                        node = nodes[tag['value']]
                        node['instances'][instance['instanceId']] = inst_info
                    elif tag['key'] == 'euca:node:migration:destination':
                        inst_info['migration_dest'] = tag['value']
                    # 'euca:node:migration:source' also exists
        return nodes

    def print_result(self, nodes):
        for _, node in sorted(nodes.items()):
            table = self.get_table(('NODE', 'zone', 'host', 'state',
                                    'details'))
            table.add_row(('NODE', node['zone'], node['host'],
                           node['state'].lower(),
                           ', '.join('='.join((key, val)) for key, val in
                           node['details'].iteritems())))
            print table
            table = self.get_table(('INSTANCE', 'ID', 'MIGRATING-TO', 'dest'))
            for instance_id, instance in node['instances'].iteritems():
                if 'migration_dest' in instance:
                    table.add_row(('INSTANCE', instance_id, 'MIGRATING-TO',
                                   instance['migration_dest']))
                else:
                    table.add_row(('INSTANCE', instance_id, '', ''))
            print table
