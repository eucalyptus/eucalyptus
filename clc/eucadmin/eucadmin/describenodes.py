# Copyright 2013 Eucalyptus Systems, Inc.
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

import eucadmin
from eucadmin.describeinstances import DescribeInstances
from eucadmin.describeservices  import DescribeServices


class DescribeNodes(eucadmin.EucadminRequest):
    Description = 'List node controllers and their instances'
    ServiceClass = eucadmin.EucAdmin

    def main(self, **args):
        obj = DescribeServices(debug=self.args.get('debug'))
        services = obj.main(all=True, filter_type='node', **args)
        service_statuses = services['euca:DescribeServicesResponseType'] \
            ['euca:serviceStatuses']
        nodes = {}
        for service in service_statuses:
            sid = service['euca:serviceId']
            hostname = sid.get('euca:hostName')
            nodes[sid['euca:name']] = {'partition': sid['euca:partition'],
                                       'name': sid['euca:name'],
                                       'hostname': hostname,
                                       'state': service['euca:localState'],
                                       'details': service['euca:details'],
                                       'instances': {}}

        obj = DescribeInstances(InstanceId=['verbose'],
                                debug=self.args.get('debug'))
        inst_response = obj.main(**args)
        reservations = inst_response['DescribeInstancesResponse'] \
            ['reservationSet']
        for reservation in reservations:
            for instance in reservation.get('instancesSet', []):
                i_info = {}
                for tag in instance.get('tagSet', []):
                     if tag['key'] == 'euca:node' and tag['value'] in nodes:
                         node = nodes[tag['value']]
                         node['instances'][instance['instanceId']] = i_info
                     elif tag['key'] == 'euca:node:migration:destination':
                         i_info['migration_dest'] = tag['value']
                     # 'euca:node:migration-source'
        return nodes

    def cli_formatter(self, nodes):
        for node_name, node in nodes.iteritems():
            print '\t'.join(('NODE', node['partition'],
                             node['hostname'] or '', node_name,
                             node['state'],
                             ', '.join('%s=%s' % (key, val) for key, val in
                                       node.get('details', {}).iteritems())))
            for inst_id, instance in node['instances'].iteritems():
                if 'migration_dest' in instance:
                    print '\t'.join(('INSTANCE', inst_id, 'MIGRATING-TO',
                                     instance['migration_dest']))
                else:
                    print '\t'.join(('INSTANCE', inst_id))

    def main_cli(self):
        eucadmin.print_version_if_necessary()
        self.do_cli()
