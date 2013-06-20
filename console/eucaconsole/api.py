# Copyright 2012 Eucalyptus Systems, Inc.
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

import base64
import ConfigParser
from datetime import datetime
import functools
import logging
import json
import tornado.web
import eucaconsole
import socket
import sys
import traceback
import time
from xml.sax.saxutils import unescape
from M2Crypto import RSA
from boto.ec2.blockdevicemapping import BlockDeviceMapping, BlockDeviceType
from boto.ec2.autoscale.launchconfig import LaunchConfiguration
from boto.ec2.autoscale.policy import ScalingPolicy
from boto.ec2.autoscale.tag import Tag
from boto.ec2.autoscale.group import AutoScalingGroup
from boto.ec2.cloudwatch.alarm import MetricAlarm
from boto.ec2.elb.healthcheck import HealthCheck
from boto.ec2.elb.listener import Listener
from boto.exception import EC2ResponseError
from boto.exception import S3ResponseError
from boto.exception import BotoServerError
from eucaconsole.threads import Threads

from .botoclcinterface import BotoClcInterface
from .botobalanceinterface import BotoBalanceInterface
from .botowalrusinterface import BotoWalrusInterface
from .botowatchinterface import BotoWatchInterface
from .botoscaleinterface import BotoScaleInterface
from .botojsonencoder import BotoJsonEncoder
from .botojsonencoder import BotoJsonBalanceEncoder
from .botojsonencoder import BotoJsonWatchEncoder
from .botojsonencoder import BotoJsonScaleEncoder
from .botojsonencoder import BotoJsonStorageEncoder
from .cachingclcinterface import CachingClcInterface
from .cachingbalanceinterface import CachingBalanceInterface
from .cachingwalrusinterface import CachingWalrusInterface
from .cachingwatchinterface import CachingWatchInterface
from .cachingscaleinterface import CachingScaleInterface
from .mockclcinterface import MockClcInterface
from .mockbalanceinterface import MockBalanceInterface
from .mockwatchinterface import MockWatchInterface
from .mockscaleinterface import MockScaleInterface
from .mockwalrusinterface import MockWalrusInterface
from .response import ClcError
from .response import Response

class BaseAPIHandler(eucaconsole.BaseHandler):
    json_encoder = None

    def get_argument_list(self, name, name_suffix=None, another_suffix=None, size=None):
        ret = []
        index = 1
        index2 = 1
        pattern = name+'.%d'
        if name_suffix:
            pattern = pattern+'.'+name_suffix
        if another_suffix:
            pattern = pattern+'.%d.'+another_suffix
        val = ''
        if another_suffix:
            val = self.get_argument(pattern % (index, index2), None)
        else:
            val = self.get_argument(pattern % (index), None)
        while (index < (size+1)) if size else val:
            ret.append(val)
            index = index + 1
            if another_suffix:
                val = self.get_argument(pattern % (index, index2), None)
            else:
                val = self.get_argument(pattern % (index), None)
        return ret


    def extract_ids(self, data):
        if isinstance(data, list):
            ret = []
            for item in data:
                ret.append(item.id)
            return ret
        else:
            return data
                
    # async calls end up back here so we can check error status and reply appropriately
    def callback(self, response):
        if response.error:
            err = response.error
            ret = '[]'
            if isinstance(err, BotoServerError):
                ret = ClcError(err.status, err.reason, err.message)
                self.set_status(err.status);
            elif issubclass(err.__class__, Exception):
                if isinstance(err, socket.timeout):
                    ret = ClcError(504, 'Timed out', None)
                    self.set_status(504);
                else:
                    ret = ClcError(500, err.message, None)
                    self.set_status(500);
            self.set_header("Content-Type", "application/json;charset=UTF-8")
            self.set_header("Cache-control", "no-store")
            self.set_header("Pragma", "no-cache")
            self.write(json.dumps(ret, cls=self.json_encoder))
            self.finish()
            logging.exception(err)
        else:
            try:
                try:
                    if(eucaconsole.config.get('test','apidelay')):
                        time.sleep(int(eucaconsole.config.get('test','apidelay'))/1000.0);
                except ConfigParser.NoOptionError:
                    pass
                summary = False
                if summary:
                    ret = Response(extract_ids(response.data))
                else:
                    ret = Response(response.data) # wrap all responses in an object for security purposes
                data = json.dumps(ret, cls=self.json_encoder, indent=2)
                self.set_header("Content-Type", "application/json;charset=UTF-8")
                self.set_header("Cache-control", "no-store")
                self.set_header("Pragma", "no-cache")
                self.write(data)
                self.finish()
            except Exception, err:
                print err

class ScaleHandler(BaseAPIHandler):
    json_encoder = BotoJsonScaleEncoder

    def get_tags(self):
        ret = []
        index = 1
        name_p = 'Tag.%d.Key'
        value_p = 'Tag.%d.Value'
        prop_p = 'Tag.%d.PropagateAtLaunch'
        id_p = 'Tag.%d.ResourceId'
        type_p = 'Tag.%d.ResourceType'
        done = False
        while not(done):
            name = self.get_argument(name_p % (index), None)
            if not(name):
                done = True
                break
            val = self.get_argument(value_p % (index), None)
            if val:
                prop = self.get_argument(prop_p % (index), 'false')
                prop = True if (prop == 'true') else False
                id = self.get_argument(id_p % (index), None)
                type = self.get_argument(type_p % (index), 'auto-scaling-group')
                ret.append(Tag(key=name, value=value, propagate_at_launch=prop, resource_id=id, resource_type=type))
            index += 1
            
        return ret

    ##
    # This is the main entry point for API calls for AutoScaling from the browser
    # other calls are delegated to handler methods based on resource type
    #
    @tornado.web.asynchronous
    def post(self):
        if not self.authorized():
            raise tornado.web.HTTPError(401, "not authorized")
        if not(self.user_session.scaling):
            if self.should_use_mock():
                self.user_session.scaling = MockScaleInterface()
            else:
                host = eucaconsole.config.get('server', 'clchost')
                if self.user_session.host_override:
                    host = self.user_session.host_override
                self.user_session.scaling = BotoScaleInterface(host,
                                                         self.user_session.access_key,
                                                         self.user_session.secret_key,
                                                         self.user_session.session_token)
            # could make this conditional, but add caching always for now
            self.user_session.scaling = CachingScaleInterface(self.user_session.scaling, eucaconsole.config)

        self.user_session.session_lifetime_requests += 1

        try:
            action = self.get_argument("Action")
            if action.find('Describe') == -1:
                self.user_session.session_last_used = time.time()
                self.check_xsrf_cookie()

            if action == 'CreateAutoScalingGroup':
                name = self.get_argument('AutoScalingGroupName')
                launch_config = self.get_argument('LaunchConfigurationName')
                azones = self.get_argument_list('AvailabilityZones.member')
                balancers = self.get_argument_list('LoadBalancerNames.member')
                def_cooldown = self.get_argument('DefaultCooldown', None)
                hc_type = self.get_argument('HealthCheckType', None)
                hc_period = self.get_argument('HealthCheckGracePeriod', None)
                desired_capacity = self.get_argument('DesiredCapacity', None)
                min_size = self.get_argument('MinSize', 0)
                max_size = self.get_argument('MaxSize', 0)
                tags = self.get_argument_list('Tags.member')
                termination_policy = self.get_argument_list('TerminationPolicies.member')
                as_group = AutoScalingGroup(name=name, launch_config=launch_config,
                                availability_zones=azones, load_balancers=balancers,
                                default_cooldown=def_cooldown, health_check_type=hc_type,
                                health_check_period=hc_period, desired_capacity=desired_capacity,
                                min_size=min_size, max_size=max_size, tags=tags,
                                termination_policy=termination_policy)
                self.user_session.scaling.create_auto_scaling_group(as_group, self.callback)
            elif action == 'DeleteAutoScalingGroup':
                name = self.get_argument('AutoScalingGroupName')
                force = self.get_argument('ForceDelete', '') == 'true'
                self.user_session.scaling.delete_auto_scaling_group(name, force, self.callback)
            elif action == 'DescribeAutoScalingGroups':
                names = self.get_argument_list('AutoScalingGroupNames.member')
                max_records = self.get_argument("MaxRecords", None)
                next_token = self.get_argument("NextToken", None)
                self.user_session.scaling.get_all_groups(names, max_records, next_token, self.callback)
            elif action == 'DescribeAutoScalingInstances':
                instance_ids = self.get_argument_list('InstanceIds.member')
                max_records = self.get_argument("MaxRecords", None)
                next_token = self.get_argument("NextToken", None)
                self.user_session.scaling.get_all_autoscaling_instances(instance_ids, max_records, next_token, self.callback)
            elif action == 'SetDesiredCapacity':
                name = self.get_argument('AutoScalingGroupName')
                desired_capacity = self.get_argument('DesiredCapacity')
                honor_cooldown = self.get_argument('HonorCooldown', '') == 'true'
                self.user_session.scaling.set_desired_capacity(name, desired_capacity, honor_cooldown, self.callback)
            elif action == 'SetInstanceHealth':
                instance_id = self.get_argument('InstanceId')
                health_status = self.get_argument('HealthStatus')
                respect_grace_period = self.get_argument('ShouldRespectGracePeriod', '') == 'true'
                self.user_session.scaling.set_instance_health(instance_id, health_status, respect_grace_period, self.callback)
            elif action == 'TerminateInstanceInAutoScalingGroup':
                instance_id = self.get_argument('InstanceId')
                decrement_capacity = self.get_argument('ShouldDecrementDesiredCapacity', '') == 'true'
                self.user_session.scaling.terminate_instance(instance_id, decrement_capacity, self.callback)
            elif action == 'UpdateAutoScalingGroup':
                name = self.get_argument('AutoScalingGroupName')
                azones = self.get_argument_list('AvailabilityZones.member', None)
                def_cooldown = self.get_argument('DefaultCooldown', None)
                desired_capacity = self.get_argument('DesiredCapacity', None)
                hc_period = self.get_argument('HealthCheckGracePeriod', None)
                hc_type = self.get_argument('HealthCheckType', None)
                launch_config = self.get_argument('LaunchConfigurationName', None)
                min_size = self.get_argument('MinSize', None)
                max_size = self.get_argument('MaxSize', None)
                termination_policy = self.get_argument_list('TerminationPolicies.member')
                group = AutoScalingGroup(name=name, launch_config=launch_config,
                                availability_zones=azones, default_cooldown=def_cooldown,
                                health_check_type=hc_type, health_check_period=hc_period,
                                desired_capacity=desired_capacity,
                                min_size=min_size, max_size=max_size,
                                termination_policies=termination_policy)
                self.user_session.scaling.update_autoscaling_group(group, self.callback)
            elif action == 'CreateLaunchConfiguration':
                image_id = self.get_argument('ImageId')
                name = self.get_argument('LaunchConfigurationName')
                instance_type = self.get_argument('InstanceType', 'm1.small')
                key_name = self.get_argument('KeyName', None)
                user_data = self.get_argument('UserData', None)
                kernel_id = self.get_argument('KernelId', None)
                ramdisk_id = self.get_argument('RamdiskId', None)
                groups = self.get_argument_list('SecurityGroups.member')
                # get block device mappings
                bdm = []
                mapping = self.get_argument('BlockDeviceMapping.1.DeviceName', None)
                idx = 1
                while mapping:
                    pre = 'BlockDeviceMapping.%d' % idx
                    dev_name = mapping
                    block_dev_type = boto.ec2.autoscale.launchconfig.BlockDeviceMapping()
                    block_dev_type.ephemeral_name = self.get_argument('%s.VirtualName' % pre, None)
                    if not(block_dev_type.ephemeral_name):
                        block_dev_type.snapshot_id = \
                                self.get_argument('%s.Ebs.SnapshotId' % pre, None)
                        block_dev_type.size = \
                                self.get_argument('%s.Ebs.VolumeSize' % pre, None)
                    bdm[dev_name] = block_dev_type
                    idx += 1
                    mapping = self.get_argument('BlockDeviceMapping.%d.DeviceName' % idx, None)
                if len(bdm) == 0:
                    bdm = None
                monitoring = self.get_argument('Instancemonitoring.Enabled', '') == 'true'
                spot_price = self.get_argument('SpotPrice', None)
                iam_instance_profile = self.get_argument('IamInstanceProfile', None)
                config = LaunchConfiguration(image_id=image_id, name=name,
                                instance_type=instance_type, key_name=key_name,
                                user_data=user_data, kernel_id=kernel_id,
                                ramdisk_id=ramdisk_id, instance_monitoring=monitoring,
                                spot_price=spot_price,
                                instance_profile_name=iam_instance_profile,
                                block_device_mappings=bdm,
                                security_groups=groups)
                self.user_session.scaling.create_launch_configuration(config, self.callback)
            elif action == 'DeleteLaunchConfiguration':
                config_name = self.get_argument('LaunchConfigurationName')
                self.user_session.scaling.delete_launch_configuration(config_name, self.callback)
            elif action == 'DescribeLaunchConfigurations':
                config_names = self.get_argument_list('LaunchConfigurationNames.member')
                max_records = self.get_argument("MaxRecords", None)
                next_token = self.get_argument("NextToken", None)
                self.user_session.scaling.get_all_launch_configurations(config_names, max_records, next_token, self.callback)
            elif action == 'DeletePolicy':
                policy_name = self.get_argument("PolicyName")
                as_group = self.get_argument("AutoScalingGroupName", None)
                self.user_session.scaling.delete_policy(policy_name, as_group, self.callback)
            elif action == 'DescribePolicies':
                as_group = self.get_argument("AutoScalingGroupName", None)
                policy_names = self.get_argument_list('PolicyNames.member')
                max_records = self.get_argument("MaxRecords", None)
                next_token = self.get_argument("NextToken", None)
                self.user_session.scaling.get_all_policies(as_group, policy_names, max_records, next_token, self.callback)
            elif action == 'ExecutePolicy':
                policy_name = self.get_argument("PolicyName")
                as_group = self.get_argument("AutoScalingGroupName", None)
                honor_cooldown = self.get_argument("HonorCooldown", None)
                self.user_session.scaling.execute_policy(policy_name, as_group, honor_cooldown, self.callback)
            elif action == 'PutScalingPolicy':
                policy_name = self.get_argument("PolicyName")
                adjustment_type = self.get_argument("AdjustmentType")
                as_group = self.get_argument("AutoScalingGroupName")
                scaling_adjustment = self.get_argument("ScalingAdjustment")
                cooldown = self.get_argument("Cooldown", None)
                min_adjustment_step = self.get_argument("MinAdjustmentStep", None)
                policy = ScalingPolicy(name=policy_name, adjustment_type=adjustment_type, as_name=as_group, scaling_adjustment=scaling_adjustment, cooldown=cooldown, min_adjustment_step=min_adjustment_step)
                self.user_session.scaling.create_scaling_policy(policy, self.callback)
            elif action == 'DescribeAdjustmentTypes':
                self.user_session.scaling.get_all_adjustment_types(self.callback)
            elif action == 'DeleteTags':
                tags = self.get_tags()
                self.user_session.scaling.delete_tags(tags, self.callback)
            elif action == 'DescribeTags':
                filters = self.get_argument_list("Filters.member")
                max_records = self.get_argument("MaxRecords", None)
                next_token = self.get_argument("NextToken", None)
                self.user_session.scaling.get_all_tags(filters, max_records, next_token, self.callback)
            elif action == 'CreateOrUpdateTags':
                tags = self.get_tags()
                self.user_session.scaling.create_or_update_tags(tags, self.callback)

        except Exception as ex:
            logging.error("Could not fulfill request, exception to follow")
            logging.error("Since we got here, client likely not notified either!")
            logging.exception(ex)

class BalanceHandler(BaseAPIHandler):
    json_encoder = BotoJsonBalanceEncoder

    def get_listener_args(self):
        ret = []
        index = 1
        key_p = 'Listeners.member.%d.%s'
        done = False
        while not(done):
            balancer_port = self.get_argument(key_p % (index, 'LoadBalancerPort'), None)
            instance_port = self.get_argument(key_p % (index, 'InstancePort'), 0)
            protocol = self.get_argument(key_p % (index, 'Protocol'), '')
            upper_proto = protocol.upper()
            ssl_cert_id = None
            if upper_proto == 'HTTPS' or upper_proto == 'SSL':
                ssl_cert_id = self.get_argument(key_p % (index, 'SSLCertificateId'), None)

            if not(balancer_port):
                done = True
                break
            l = balancer_port, instance_port, protocol, ssl_cert_id
            ret.append(l)
            index += 1

        return ret


    ##
    # This is the main entry point for API calls for AutoScaling from the browser
    # other calls are delegated to handler methods based on resource type
    #
    @tornado.web.asynchronous
    def post(self):
        if not self.authorized():
            raise tornado.web.HTTPError(401, "not authorized")
        if not(self.user_session.elb):
            if self.should_use_mock():
                self.user_session.elb = MockBalanceInterface()
            else:
                host = eucaconsole.config.get('server', 'clchost')
                if self.user_session.host_override:
                    host = self.user_session.host_override
                self.user_session.elb = BotoBalanceInterface(host,
                                                         self.user_session.access_key,
                                                         self.user_session.secret_key,
                                                         self.user_session.session_token)
            # could make this conditional, but add caching always for now
            self.user_session.elb = CachingBalanceInterface(self.user_session.elb, eucaconsole.config)

        self.user_session.session_lifetime_requests += 1

        try:
            action = self.get_argument("Action")
            if action.find('Describe') == -1:
                self.user_session.session_last_used = time.time()
                self.check_xsrf_cookie()

            if action == 'CreateLoadBalancer':
                azones = self.get_argument_list('AvailabilityZones.member')
                listeners = self.get_listener_args()
                name = self.get_argument('LoadBalancerName')
                scheme = self.get_argument('Scheme', 'internet-facing')
                groups = self.get_argument_list('SecurityGroups.member')
                subnets = self.get_argument_list('Subnets.member')
                self.user_session.elb.create_load_balancer(name, azones, listeners, subnets, groups, scheme, self.callback)
            elif action == 'DeleteLoadBalancer':
                name = self.get_argument('LoadBalancerName')
                self.user_session.elb.delete_load_balancer(name, self.callback)
            elif action == 'DescribeLoadBalancers':
                names = self.get_argument_list('LoadBalancerNames.member')
                self.user_session.elb.get_all_load_balancers(names, self.callback)
            elif action == 'DeregisterInstancesFromLoadBalancer':
                name = self.get_argument('LoadBalancerName')
                instances = self.get_argument_list('Instances.member')
                self.user_session.elb.deregister_instances(name, instances, self.callback)
            elif action == 'RegisterInstancesWithLoadBalancer':
                name = self.get_argument('LoadBalancerName')
                instances = self.get_argument_list('Instances.member')
                self.user_session.elb.register_instances(name, instances, self.callback)
            elif action == 'CreateLoadBalancerListeners':
                name = self.get_argument('LoadBalancerName')
                listeners = self.get_listener_args()
                self.user_session.elb.create_load_balancer_listeners(name, listeners, self.callback)
            elif action == 'DeleteLoadBalancerListeners':
                name = self.get_argument('LoadBalancerName')
                ports = self.get_argument_list('LoadBalancerPorts.member')
                self.user_session.elb.delete_load_balancer_listeners(name, ports, self.callback)
            elif action == 'ConfigureHealthCheck':
                name = self.get_argument('LoadBalancerName')
                timeout = self.get_argument('HealthCheck.Timeout')
                target = self.get_argument('HealthCheck.Target')
                interval = self.get_argument('HealthCheck.Interval')
                unhealthy = self.get_argument('HealthCheck.UnhealthyThreshold')
                healthy = self.get_argument('HealthCheck.HealthyThreshold')
                hc = HealthCheck(timeout=timeout, target=target, interval=interval,
                                 unhealthy_threshold=unhealthy, healthy_threshold=healthy)
                self.user_session.elb.configure_health_check(name, hc, self.callback)
            elif action == 'DescribeInstanceHealth':
                name = self.get_argument('LoadBalancerName')
                instances = self.get_argument_list('Instances.member')
                self.user_session.elb.describe_instance_health(name, instances, self.callback)

        except Exception as ex:
            logging.error("Could not fulfill request, exception to follow")
            logging.error("Since we got here, client likely not notified either!")
            logging.exception(ex)

class WatchHandler(BaseAPIHandler):
    ISO_FORMAT = "%Y-%m-%dT%H:%M:%S.%f"
    json_encoder = BotoJsonWatchEncoder


    ##
    # This is the main entry point for API calls for CloudWatch from the browser
    # other calls are delegated to handler methods based on resource type
    #
    @tornado.web.asynchronous
    def post(self):
        if not self.authorized():
            raise tornado.web.HTTPError(401, "not authorized")
        if not(self.user_session.cw):
            if self.should_use_mock():
                self.user_session.cw = MockWatchInterface()
            else:
                host = eucaconsole.config.get('server', 'clchost')
                if self.user_session.host_override:
                    host = self.user_session.host_override
                self.user_session.cw = BotoWatchInterface(host,
                                                         self.user_session.access_key,
                                                         self.user_session.secret_key,
                                                         self.user_session.session_token)
            # could make this conditional, but add caching always for now
            self.user_session.cw = CachingWatchInterface(self.user_session.cw, eucaconsole.config)

        self.user_session.session_lifetime_requests += 1

        try:
            action = self.get_argument("Action")
            if action.find('Describe') == -1:
                self.user_session.session_last_used = time.time()
                self.check_xsrf_cookie()

            if action == 'GetMetricStatistics':
                period = self.get_argument('Period')
                start_time = datetime.strptime(self.get_argument('StartTime'), self.ISO_FORMAT)
                end_time = datetime.strptime(self.get_argument('EndTime'), self.ISO_FORMAT)
                metric_name = self.get_argument('MetricName')
                namespace = self.get_argument('Namespace')
                statistics = self.get_argument_list('Statistics.member')
                dimensions = self.get_argument_list('Dimensions.member')
                unit = self.get_argument('Unit')
                self.user_session.cw.get_metric_statistics(period, start_time, end_time, metric_name, namespace, statistics, dimensions, unit, self.callback)
            elif action == 'ListMetrics':
                dimensions = self.get_argument_list('Dimensions.member')
                metric_name = self.get_argument('MetricName', None)
                namespace = self.get_argument('Namespace', None)
                next_token = self.get_argument('NextToken', None)
                self.user_session.cw.list_metrics(next_token, dimensions, metric_name, namespace, self.callback)
            elif action == 'PutMetricData':
                namespace = self.get_argument('Namespace')
                data = []
                metric_name = self.get_argument('MetricData.member.1.MetricName')
                idx = 1
                while metric_name:
                    value = self.get_argument("MetricData.member.%d.Value" % idx, None)
                    timestamp = self.get_argument("MetricData.member.%d.Timestamp" % idx, None)
                    unit = self.get_argument("MetricData.member.%d.Unit" % idx, None)
                    dimensions = self.get_argument("MetricData.member.%d.Dimensions" % idx, None)
                    statistics = self.get_argument("MetricData.member.%d.StatisticValues" % idx, None)
                    data.append({'name':name, 'value':value, 'timestamp':timestamp, 'unit':unit, 'dimensions':dimensions, 'statistics':statistics})
                    idx += 1
                    metric_name = self.get_argument("MetricData.member.%d.MetricName" % idx, None)
                self.user_session.cw.put_metric_data(namespace, data, self.callback)
            elif action == 'DescribeAlarms':
                action_prefix = self.get_argument('ActionPrefix', None)
                alarm_name_prefix = self.get_argument('AlarmNamePrefix', None)
                alarm_names = self.get_argument_list('AlarmNames.member')
                max_records = self.get_argument('MaxRecords', None)
                state_value = self.get_argument('StateValue', None)
                next_token = self.get_argument('NextToken', None)
                self.user_session.cw.describe_alarms(action_prefix, alarm_name_prefix, alarm_names, max_records, state_value, next_token, self.callback)
            elif action == 'DeleteAlarms':
                alarm_names = self.get_argument_list('AlarmNames.member')
                self.user_session.cw.delete_alarms(alarm_names, self.callback)
            elif action == 'EnableAlarmActions':
                alarm_names = self.get_argument_list('AlarmNames.member')
                self.user_session.cw.enable_alarm_actions(alarm_names, self.callback)
            elif action == 'DisableAlarmActions':
                alarm_names = self.get_argument_list('AlarmNames.member')
                self.user_session.cw.disable_alarm_actions(alarm_names, self.callback)
            elif action == 'PutMetricAlarm':
                actions_enabled = self.get_argument('ActionsEnabled', None)
                alarm_actions = self.get_argument_list('AlarmActions.member')
                alarm_desc = self.get_argument('AlarmDescription', None)
                alarm_name = self.get_argument('AlarmName')
                comparison_op = self.get_argument('ComparisonOperator')
                dimensions = self.get_argument_list('Dimensions.member')
                eval_periods = self.get_argument('EvaluationPeriods')
                insufficient_data_actions = self.get_argument_list('InsufficientDataActions.member')
                metric_name = self.get_argument('MetricName')
                namespace = self.get_argument('Namespace')
                ok_actions = self.get_argument_list('OKActions.member')
                period = self.get_argument('Period')
                statistic = self.get_argument('Statistic')
                threshold = self.get_argument('Threshold')
                unit = self.get_argument('Unit', None)
                if comparison_op == "GreaterThanOrEqualToThreshold":
                  comparison_op = ">="
                elif comparison_op == "GreaterThanThreshold":
                  comparison_op = ">"
                elif comparison_op == "LessThanOrEqualToThreshold":
                  comparison_op = "<="
                elif comparison_op == "LessThanThreshold":
                  comparison_op = "<"
                alarm = MetricAlarm(name=alarm_name, metric=metric_name, namespace=namespace, statistic=statistic, comparison=comparison_op,
                                    threshold=threshold, period=period, evaluation_periods=eval_periods, unit=unit, description=alarm_desc,
                                    dimensions=dimensions, alarm_actions=alarm_actions, insufficient_data_actions=insufficient_data_actions,
                                    ok_actions=ok_actions)
                self.user_session.cw.put_metric_alarm(alarm, self.callback)

        except Exception as ex:
            logging.error("Could not fulfill request, exception to follow")
            logging.error("Since we got here, client likely not notified either!")
            logging.exception(ex)

class StorageHandler(BaseAPIHandler):
    json_encoder = BotoJsonStorageEncoder

    ##
    # This is the main entry point for API calls for S3(Walrus) from the browser
    # other calls are delegated to handler methods based on resource type
    #
    @tornado.web.asynchronous
    def post(self):
        if not self.authorized():
            raise tornado.web.HTTPError(401, "not authorized")
        if not(self.user_session.walrus):
            if self.should_use_mock():
                self.user_session.walrus = MockWalrusInterface()
            else:
                host = eucaconsole.config.get('server', 'clchost')
                if self.user_session.host_override:
                    host = self.user_session.host_override
                self.user_session.walrus = BotoWalrusInterface(host,
                                                         self.user_session.access_key,
                                                         self.user_session.secret_key,
                                                         self.user_session.session_token)
            # could make this conditional, but add caching always for now
            self.user_session.walrus = CachingWalrusInterface(self.user_session.walrus, eucaconsole.config)

        self.user_session.session_lifetime_requests += 1

        try:
            action = self.get_argument("Action")
            if action.find('Describe') == -1:
                self.user_session.session_last_used = time.time()
                self.check_xsrf_cookie()

            if action == 'DescribeBuckets':
                self.user_session.walrus.get_all_buckets(self.callback)
            elif action == 'DescribeObjects':
                bucket = self.get_argument('Bucket')
                self.user_session.walrus.get_all_objects(bucket, self.callback)

        except Exception as ex:
            logging.error("Could not fulfill request, exception to follow")
            logging.error("Since we got here, client likely not notified either!")
            logging.exception(ex)

class ComputeHandler(BaseAPIHandler):
    json_encoder = BotoJsonEncoder

    # This method unescapes values that were escaped in the jsonbotoencoder.__sanitize_and_copy__ method
    # TODO: this should not be needed when we stop escaping on the proxy and only escape in the browser as needed
    #def get_argument(self, name, default=tornado.web.RequestHandler._ARG_DEFAULT, strip=True):
    #    arg = super(ComputeHandler, self).get_argument(name, default, strip)
    #    if arg:
    #        return unescape(arg)
    #    else:
    #        return arg

    def get_filter_args(self):
        ret = {}
        index = 1
        index2 = 1
        name_p = 'Filter.%d.Name'
        value_p = 'Filter.%d.Value.%d'
        vals = []
        done = False
        while not(done):
            name = self.get_argument(name_p % (index), None)
            if not(name):
                done = True
                break
            val = self.get_argument(value_p % (index, index2), None)
            while (val):
                vals.append(val)
                index2 += 1
                val = self.get_argument(value_p % (index, index2), None)
            if index2 > 1: # values found
                ret[name] = vals
            index += 1
            index2 = 1
            
        return ret

    def get_tags(self):
        ret = {}
        index = 1
        name_p = 'Tag.%d.Key'
        value_p = 'Tag.%d.Value'
        done = False
        while not(done):
            name = self.get_argument(name_p % (index), None)
            if not(name):
                done = True
                break
            val = self.get_argument(value_p % (index), None)
            if val:
                ret[name] = val
            index += 1
            
        return ret

    def handleRunInstances(self, action, clc, user_data_file, callback):
        image_id = self.get_argument('ImageId')
        min = self.get_argument('MinCount', '1')
        max = self.get_argument('MaxCount', '1')
        key = self.get_argument('KeyName', None)
        groups = self.get_argument_list('SecurityGroup')
        sec_group_ids = self.get_argument_list('SecurityGroupId')
        if user_data_file:
            user_data = user_data_file
        else:
            user_data = self.get_argument('UserData', "")
            user_data = base64.b64decode(user_data)
        addr_type = self.get_argument('AddressingType', None)
        vm_type = self.get_argument('InstanceType', None)
        placement = self.get_argument('Placement.AvailabilityZone', None)
        placement_group = self.get_argument('Placement.GroupName', None)
        tenancy = self.get_argument('Placement.Tenancy', None)
        kernel = self.get_argument('KernelId', None)
        ramdisk = self.get_argument('RamdiskId', None)
        monitoring=False
        if self.get_argument('Monitoring.Enabled', '') == 'true':
            monitoring=True
        subnet = self.get_argument('SubnetId', None);
        private_ip = self.get_argument('PrivateIpAddress', None);
        # get block device mappings
        bdm = BlockDeviceMapping()
        mapping = self.get_argument('BlockDeviceMapping.1.DeviceName', None)
        idx = 1
        while mapping:
            pre = 'BlockDeviceMapping.%d' % idx
            dev_name = mapping
            block_dev_type = BlockDeviceType()
            block_dev_type.ephemeral_name = self.get_argument('%s.VirtualName' % pre, None)
            if not(block_dev_type.ephemeral_name):
                block_dev_type.no_device = \
                    (self.get_argument('%s.NoDevice' % pre, '') == 'true')
                block_dev_type.snapshot_id = \
                        self.get_argument('%s.Ebs.SnapshotId' % pre, None)
                block_dev_type.size = \
                        self.get_argument('%s.Ebs.VolumeSize' % pre, None)
                block_dev_type.delete_on_termination = \
                        (self.get_argument('%s.Ebs.DeleteOnTermination' % pre, '') == 'true')
            bdm[dev_name] = block_dev_type
            idx += 1
            mapping = self.get_argument('BlockDeviceMapping.%d.DeviceName' % idx, None)
        if len(bdm) == 0:
            bdm = None
            
        api_termination=False
        if self.get_argument('DisableApiTermination', '') == 'true':
            api_termination=True
        instance_shutdown=False
        if self.get_argument('InstanceInitiatedShutdownBehavior', '') == 'true':
            instance_shutdown=True
        token = self.get_argument('ClientToken', None);
        addition_info = self.get_argument('AdditionInfo', None);
        instance_profile_name = self.get_argument('IamInstanceProfile.Name', None);
        instance_profile_arn = self.get_argument('IamInstanceProfile.Arn', None);

        return clc.run_instances(image_id, min_count=min, max_count=max,
                            key_name=key, security_groups=groups,
                            user_data=user_data, addressing_type=addr_type,
                            instance_type=vm_type, placement=placement,
                            kernel_id=kernel, ramdisk_id=ramdisk,
                            monitoring_enabled=monitoring, subnet_id=subnet,
                            block_device_map=bdm,
                            disable_api_termination=api_termination,
                            instance_initiated_shutdown_behavior=instance_shutdown,
                            private_ip_address=private_ip,
                            placement_group=placement_group, client_token=token,
                            security_group_ids=sec_group_ids,
                            additional_info=addition_info,
                            instance_profile_name=instance_profile_name,
                            instance_profile_arn=instance_profile_arn,
                            tenancy=tenancy, callback=callback)

    def handleImages(self, action, clc, callback=None):
        if action == 'DescribeImages':
            owner = self.get_argument('Owner', None);
            if not owner:
                owners = None
            else:
                owners = [owner]
            filters = self.get_filter_args()
            return clc.get_all_images(owners, filters, callback)
        elif action == 'DescribeImageAttribute':
            imageid = self.get_argument('ImageId')
            attribute = self.get_argument('Attribute')
            return clc.get_image_attribute(imageid, attribute, callback)
        elif action == 'ModifyImageAttribute':
            imageid = self.get_argument('ImageId')
            attribute = self.get_argument('Attribute')
            operation = self.get_argument('OperationType')
            users = self.get_argument_list('UserId')
            groups = self.get_argument_list('UserGroup')
            return clc.modify_image_attribute(imageid, attribute, operation, users, groups, callback)
        elif action == 'ResetImageAttribute':
            imageid = self.get_argument('ImageId')
            attribute = self.get_argument('Attribute')
            return clc.reset_image_attribute(imageid, attribute, callback)
        elif action == 'DeregisterImage':
            image_id = self.get_argument('ImageId')
            return clc.deregister_image(image_id, callback)
        elif action == 'RegisterImage':
            image_location = self.get_argument('ImageLocation', None)
            name = self.get_argument('Name')
            description = self.get_argument('Description', None)
            if description != None:
              description = base64.b64decode(description);
            architecture = self.get_argument('Architecture', None)
            kernel_id = self.get_argument('KernelId', None)
            ramdisk_id = self.get_argument('RamdiskId', None)
            root_dev_name = self.get_argument('RootDeviceName', None)
            # get block device mappings
            bdm = BlockDeviceMapping()
            mapping = self.get_argument('BlockDeviceMapping.1.DeviceName', None)
            idx = 1
            while mapping:
                pre = 'BlockDeviceMapping.%d' % idx
                dev_name = mapping
                block_dev_type = BlockDeviceType()
                block_dev_type.ephemeral_name = self.get_argument('%s.VirtualName' % pre, None)
                if not(block_dev_type.ephemeral_name):
                    block_dev_type.no_device = \
                        (self.get_argument('%s.NoDevice' % pre, '') == 'true')
                    block_dev_type.snapshot_id = \
                            self.get_argument('%s.Ebs.SnapshotId' % pre, None)
                    block_dev_type.size = \
                            self.get_argument('%s.Ebs.VolumeSize' % pre, None)
                    block_dev_type.delete_on_termination = \
                            (self.get_argument('%s.Ebs.DeleteOnTermination' % pre, '') == 'true')
                bdm[dev_name] = block_dev_type
                idx += 1
                mapping = self.get_argument('BlockDeviceMapping.%d.DeviceName' % idx, None)
            if len(bdm) == 0:
                bdm = None
            return clc.register_image(name, image_location, description, architecture, kernel_id, ramdisk_id, root_dev_name, bdm, callback)

    def handleInstances(self, action, clc, callback=None):
        if action == 'DescribeInstances':
            filters = self.get_filter_args()
            return clc.get_all_instances(filters, callback)
        elif action == 'RunInstances':
            return self.handleRunInstances(action, clc, None, callback)
        elif action == 'TerminateInstances':
            instance_ids = self.get_argument_list('InstanceId')
            return clc.terminate_instances(instance_ids, callback)
        elif action == 'StopInstances':
            instance_ids = self.get_argument_list('InstanceId')
            return clc.stop_instances(instance_ids, False, callback)
        elif action == 'StartInstances':
            instance_ids = self.get_argument_list('InstanceId')
            return clc.start_instances(instance_ids, callback)
        elif action == 'RebootInstances':
            instance_ids = self.get_argument_list('InstanceId')
            return clc.reboot_instances(instance_ids, callback)
        elif action == 'GetConsoleOutput':
            instance_id = self.get_argument('InstanceId')
            return clc.get_console_output(instance_id, callback)

    def handleKeypairs(self, action, clc, callback=None):
        if action == 'DescribeKeyPairs':
            filters = self.get_filter_args()
            return clc.get_all_key_pairs(filters, callback)
        elif action == 'CreateKeyPair':
            name = self.get_argument('KeyName')
            ret = clc.create_key_pair(name, functools.partial(self.keycache_callback, name=name, callback=callback))
            return ret
        elif action == 'DeleteKeyPair':
            name = self.get_argument('KeyName')
            return clc.delete_key_pair(name, callback)
        elif action == 'ImportKeyPair':
            name = self.get_argument('KeyName')
            material = base64.b64decode(self.get_argument('PublicKeyMaterial', None))
            return clc.import_key_pair(name, material, callback)

    def handleGroups(self, action, clc, callback=None):
        if action == 'DescribeSecurityGroups':
            filters = self.get_filter_args()
            return clc.get_all_security_groups(filters, callback)
        elif action == 'CreateSecurityGroup':
            name = self.get_argument('GroupName')
            name = base64.b64decode(name)
            desc = self.get_argument('GroupDescription')
            desc = base64.b64decode(desc)
            return clc.create_security_group(name, desc, callback)
        elif action == 'DeleteSecurityGroup':
            name = self.get_argument('GroupName', None)
            group_id = self.get_argument('GroupId', None)
            return clc.delete_security_group(name, group_id, callback)
        elif action == 'AuthorizeSecurityGroupIngress':
            name = self.get_argument('GroupName', None)
            group_id = self.get_argument('GroupId', None)
            ip_protocol = self.get_argument_list('IpPermissions', 'IpProtocol')
            numRules = len(ip_protocol)
            from_port = self.get_argument_list('IpPermissions', 'FromPort')
            to_port = self.get_argument_list('IpPermissions', 'ToPort')
            src_security_group_name = self.get_argument_list('IpPermissions', 'Groups', 'GroupName', numRules)
            src_security_group_owner_id = self.get_argument_list('IpPermissions', 'Groups', 'UserId', numRules)
            src_security_group_group_id = self.get_argument_list('IpPermissions', 'Groups', 'GroupId', numRules)
            cidr_ip = self.get_argument_list('IpPermissions', 'IpRanges', 'CidrIp', numRules)
            clc.authorize_security_group(name, src_security_group_name,
                                 src_security_group_owner_id, ip_protocol, from_port, to_port,
                                 cidr_ip, group_id, src_security_group_group_id,
                                 callback)
            return
        elif action == 'RevokeSecurityGroupIngress':
            name = self.get_argument('GroupName', None)
            group_id = self.get_argument('GroupId', None)
            ip_protocol = self.get_argument_list('IpPermissions', 'IpProtocol')
            numRules = len(ip_protocol)
            from_port = self.get_argument_list('IpPermissions', 'FromPort')
            to_port = self.get_argument_list('IpPermissions', 'ToPort')
            src_security_group_name = self.get_argument_list('IpPermissions', 'Groups', 'GroupName', numRules)
            src_security_group_owner_id = self.get_argument_list('IpPermissions', 'Groups', 'UserId', numRules)
            src_security_group_group_id = self.get_argument_list('IpPermissions', 'Groups', 'GroupId', numRules)
            cidr_ip = self.get_argument_list('IpPermissions', 'IpRanges', 'CidrIp', numRules)
            clc.revoke_security_group(name, src_security_group_name,
                                 src_security_group_owner_id, ip_protocol, from_port, to_port,
                                 cidr_ip, group_id, src_security_group_group_id,
                                 callback)
            return

    def handleAddresses(self, action, clc, callback=None):
        if action == 'DescribeAddresses':
            filters = self.get_filter_args()
            return clc.get_all_addresses(filters, callback)
        elif action == 'AllocateAddress':
            return clc.allocate_address(callback)
        elif action == 'ReleaseAddress':
            publicip = self.get_argument('PublicIp')
            return clc.release_address(publicip, callback)
        elif action == 'AssociateAddress':
            publicip = self.get_argument('PublicIp')
            instanceid = self.get_argument('InstanceId')
            return clc.associate_address(publicip, instanceid, callback)
        elif action == 'DisassociateAddress':
            publicip = self.get_argument('PublicIp')
            return clc.disassociate_address(publicip, callback)

    def handleVolumes(self, action, clc, callback=None):
        if action == 'DescribeVolumes':
            filters = self.get_filter_args()
            return clc.get_all_volumes(filters, callback)
        elif action == 'CreateVolume':
            size = self.get_argument('Size')
            zone = self.get_argument('AvailabilityZone')
            snapshotid = self.get_argument('SnapshotId', None)
            return clc.create_volume(size, zone, snapshotid, callback)
        elif action == 'DeleteVolume':
            volumeid = self.get_argument('VolumeId')
            return clc.delete_volume(volumeid, callback)
        elif action == 'AttachVolume':
            volumeid = self.get_argument('VolumeId')
            instanceid = self.get_argument('InstanceId')
            device = self.get_argument('Device')
            return clc.attach_volume(volumeid, instanceid, device, callback)
        elif action == 'DetachVolume':
            volumeid = self.get_argument('VolumeId')
            force = self.get_argument('Force', False)
            return clc.detach_volume(volumeid, force, callback)

    def handleSnapshots(self, action, clc, callback=None):
        if action == "DescribeSnapshots":
            filters = self.get_filter_args()
            return clc.get_all_snapshots(filters, callback)
        elif action == 'CreateSnapshot':
            volumeid = self.get_argument('VolumeId')
            description = self.get_argument('Description', None)
            if description:
                description = base64.b64decode(description)
            return clc.create_snapshot(volumeid, description, callback)
        elif action == 'DeleteSnapshot':
            snapshotid = self.get_argument('SnapshotId')
            return clc.delete_snapshot(snapshotid, callback)
        elif action == 'DescribeSnapshotAttribute':
            snapshotid = self.get_argument('SnapshotId')
            attribute = self.get_argument('Attribute')
            return clc.get_snapshot_attribute(snapshotid, attribute, callback)
        elif action == 'ModifySnapshotAttribute':
            snapshotid = self.get_argument('SnapshotId')
            attribute = self.get_argument('Attribute')
            operation = self.get_argument('OperationType')
            users = self.get_argument_list('UserId')
            groups = self.get_argument_list('UsersGroup')
            return clc.modify_snapshot_attribute(snapshotid, attribute, operation, users, groups, callback)
        elif action == 'ResetSnapshotAttribute':
            snapshotid = self.get_argument('SnapshotId')
            attribute = self.get_argument('Attribute')
            return clc.reset_snapshot_attribute(snapshotid, attribute, callback)

    def handleTags(self, action, clc, callback=None):
        if action == "DescribeTags":
            filters = self.get_filter_args()
            return clc.get_all_tags(filters, callback)
        elif action == 'CreateTags':
            resourceIds = self.get_argument_list('ResourceId')
            tags = self.get_tags()
            return clc.create_tags(resourceIds, tags, callback)
        elif action == 'DeleteTags':
            resourceIds = self.get_argument_list('ResourceId')
            tags = self.get_tags()
            return clc.delete_tags(resourceIds, tags, callback)

    def handleGetPassword(self, clc, callback):
        instanceid = self.get_argument('InstanceId')
        Threads.instance().runThread(self.__get_password_cb__, ({'instanceid':instanceid}, callback))

    def __get_password_cb__(self, kwargs, callback):
        try:
            passwd_data = self.user_session.clc.get_password_data(kwargs['instanceid'])
            priv_key_file = self.request.files['priv_key']
            user_priv_key = RSA.load_key_string(priv_key_file[0].body)
            string_to_decrypt = base64.b64decode(passwd_data)
            ret = user_priv_key.private_decrypt(string_to_decrypt, RSA.pkcs1_padding)
            ret = {'instance':kwargs['instanceid'], 'password': ret}
            Threads.instance().invokeCallback(callback, eucaconsole.cachingclcinterface.Response(data=ret))
        except Exception as ex:
            traceback.print_exc(file=sys.stdout)
            Threads.instance().invokeCallback(callback, eucaconsole.cachingclcinterface.Response(error=ex))

    ##
    # This is the main entry point for API calls for EC2 from the browser
    # other calls are delegated to handler methods based on resource type
    #
    @tornado.web.asynchronous
    def post(self):
        if not self.authorized():
            raise tornado.web.HTTPError(401, "not authorized")
        if not(self.user_session.clc):
            if self.should_use_mock():
                self.user_session.clc = MockClcInterface()
            else:
                host = eucaconsole.config.get('server', 'clchost')
                if self.user_session.host_override:
                    host = self.user_session.host_override
                self.user_session.clc = BotoClcInterface(host,
                                                         self.user_session.access_key,
                                                         self.user_session.secret_key,
                                                         self.user_session.session_token)
            # could make this conditional, but add caching always for now
            self.user_session.clc = CachingClcInterface(self.user_session.clc, eucaconsole.config)

        self.user_session.session_lifetime_requests += 1

        try:
            action = self.get_argument("Action")
            if action.find('Describe') == -1 and action.find('GetDashSummary') == -1:
                self.user_session.session_last_used = time.time()
                self.check_xsrf_cookie()

            # this call returns a file vs. a json envelope, so it is self-contained
            if action == 'GetKeyPairFile':
                name = self.get_argument('KeyName')
                result = self.user_session.keypair_cache[name]
                self.set_header("Content-Type", "application/x-pem-file;charset=ISO-8859-1")
                self.set_header("Content-Disposition", "attachment; filename=\"" + name + '.pem"')
                self.write(result)
                self.finish()
                del self.user_session.keypair_cache[name]
                return

            if action == 'GetDashSummary':
                ret = ""
                zone = self.get_argument('AvailabilityZone', 'all')
                if isinstance(self.user_session.clc, CachingClcInterface):
                    ret = self.user_session.clc.get_cache_summary(zone)
                self.callback(eucaconsole.cachingclcinterface.Response(data=ret))
            elif action == 'SetDataInterest':
                resources = self.get_argument_list('Resources.member')
                if isinstance(self.user_session.clc, CachingClcInterface):
                    ret = self.user_session.clc.set_data_interest(resources)
                self.callback(eucaconsole.cachingclcinterface.Response(data=ret))
            elif action == 'RunInstances':
                user_data_file = []
                try:
                    user_data_file = self.request.files['user_data_file']
                except KeyError:
                    pass
                if len(user_data_file) > 0:
                    self.handleRunInstances(action, self.user_session.clc, user_data_file[0].body, self.callback)
                else:
                    self.handleRunInstances(action, self.user_session.clc, None, self.callback)
            elif action == 'DescribeAvailabilityZones':
                filters = self.get_filter_args()
                self.user_session.clc.get_all_zones(filters, self.callback)
            elif action.find('Image') > -1:
                self.handleImages(action, self.user_session.clc, self.callback)
            elif action.find('Instance') > -1 or action == 'GetConsoleOutput':
                self.handleInstances(action, self.user_session.clc, self.callback)
            elif action.find('Address') > -1:
                self.handleAddresses(action, self.user_session.clc, self.callback)
            elif action.find('KeyPair') > -1:
                self.handleKeypairs(action, self.user_session.clc, self.callback)
            elif action.find('SecurityGroup') > -1:
                self.handleGroups(action, self.user_session.clc, self.callback)
            elif action.find('Volume') > -1:
                self.handleVolumes(action, self.user_session.clc, self.callback)
            elif action.find('Snapshot') > -1:
                self.handleSnapshots(action, self.user_session.clc, self.callback)
            elif action.find('Tags') > -1:
                self.handleTags(action, self.user_session.clc, self.callback)
            elif action == 'GetPassword':
                self.handleGetPassword(self.user_session.clc, self.callback)

        except Exception as ex:
            logging.error("Could not fulfill request, exception to follow")
            logging.error("Since we got here, client likely not notified either!")
            logging.exception(ex)

    def keycache_callback(self, response, name, callback):
        # respond to the client
        callback(response)
        # now, cache the response
        if not(response.error):
            self.user_session.keypair_cache[name] = response.data.material

