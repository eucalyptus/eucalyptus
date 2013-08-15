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

import boto
import ConfigParser
import json
from boto.ec2.elb import ELBConnection
from boto.ec2.regioninfo import RegionInfo

import eucaconsole
from .botojsonencoder import BotoJsonBalanceEncoder
from .balanceinterface import BalanceInterface

# This class provides an implmentation of the clcinterface using boto
class BotoBalanceInterface(BalanceInterface):
    conn = None
    saveclcdata = False

    def __init__(self, clc_host, access_id, secret_key, token):
        #boto.set_stream_logger('foo')
        path='/services/LoadBalancing'
        port=8773
        if clc_host[len(clc_host)-13:] == 'amazonaws.com':
            clc_host = clc_host.replace('ec2', 'elasticloadbalancing', 1)
            path = '/'
            reg = None
            port=443
        reg = RegionInfo(name='eucalyptus', endpoint=clc_host)
        self.conn = ELBConnection(access_id, secret_key, region=reg,
                                  port=port, path=path,
                                  is_secure=True, security_token=token, debug=0)
        self.conn.https_validate_certificates = False
        self.conn.http_connection_kwargs['timeout'] = 30

    def __save_json__(self, obj, name):
        f = open(name, 'w')
        json.dump(obj, f, cls=BotoJsonBalanceEncoder, indent=2)
        f.close()

    def create_load_balancer(self, name, zones, listeners, subnets=None,
                             security_groups=None, scheme='internet-facing'):
        return self.conn.create_load_balancer(name, zones, listeners, subnets, security_groups, scheme)
    
    def delete_load_balancer(self, name):
        return self.conn.delete_load_balancer(name)

    def get_all_load_balancers(self, load_balancer_names=None):
        params = {}
        if load_balancer_names:
            self.build_list_params(params, load_balancer_names,
                                   'LoadBalancerNames.member.%d')
        http_request = self.conn.build_base_http_request('GET', '/', None,
                                                    params, {}, '',
                                                    self.conn.server_name())
        http_request.params['Action'] = 'DescribeLoadBalancers'
        http_request.params['Version'] = self.conn.APIVersion
        response = self.conn._mexe(http_request, override_num_retries=2)
        body = response.read()
        boto.log.debug(body)
        if not body:
            boto.log.error('Null body %s' % body)
            raise self.conn.ResponseError(response.status, response.reason, body)
        elif response.status == 200:
            obj = boto.resultset.ResultSet([('member', boto.ec2.elb.loadbalancer.LoadBalancer)])
            h = boto.handler.XmlHandler(obj, self)
            import xml.sax; xml.sax.parseString(body, h)
            if self.saveclcdata:
                self.__save_json__(obj, "mockdata/ELB_Balancers.json")
            return obj
        else:
            boto.log.error('%s %s' % (response.status, response.reason))
            boto.log.error('%s' % body)
            raise self.conn.ResponseError(response.status, response.reason, body)
            

    def deregister_instances(self, load_balancer_name, instances):
        return self.conn.deregister_instances(load_balancer_name, instances)

    def register_instances(self, load_balancer_name, instances):
        return self.conn.register_instances(load_balancer_name, instances)

    def create_load_balancer_listeners(self, name, listeners):
        return self.conn.create_load_balancer_listeners(name, listeners)

    def delete_load_balancer_listeners(self, name, ports):
        return self.conn.delete_load_balancer_listeners(name, ports)

    def configure_health_check(self, name, health_check):
        return self.conn.configure_health_check(name, health_check)

    def describe_instance_health(self, load_balancer_name, instances=None):
        obj = self.conn.describe_instance_health(load_balancer_name, instances)
        if self.saveclcdata:
            self.__save_json__(obj, "mockdata/ELB_Instances.json")
        return obj

