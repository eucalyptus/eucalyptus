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
#from poster.encode import multipart_encode
#from poster.streaminghttp import register_openers
import urllib
import urllib2
import json

# to talk to the UI proxy. It can make all of the REST calls as you
# This is a client to test the interface that the browser uses
# would from the browser GUI

class UIProxyClient(object):
    session_cookie = None
    xsrf = None

    def __init__(self):
        pass

    def login(self, host, port, account, username, password, is_secure=False):
        # make request, storing cookie
        self.host = host
        self.port = port
        self.protocol = 'https' if is_secure else 'http'
        req = urllib2.Request("%s://%s:%s/"%(self.protocol, host, port))
        encoded_auth = base64.encodestring("%s:%s:%s" % (account, username, password))[:-1]
        data = "action=login&remember=no&Authorization="+encoded_auth
        response = urllib2.urlopen(req, data)
        self.session_cookie = response.headers.get('Set-Cookie')
        print self.session_cookie
        idx = self.session_cookie.find('_xsrf=')+6
        self.xsrf = self.session_cookie[idx:idx+32]
        print "_xsrf="+self.xsrf
        print response.read()

    def logout(self):
        # forget cookie
        self.session_cookie = None

    def __check_logged_in__(self, request):
        if not(self.session_cookie):
            print "Need to login first!!"
        request.add_header('cookie', self.session_cookie)

    def __add_param_list__(self, params, name, list):
        for idx, val in enumerate(list):
            params["%s.%d" % (name, idx + 1)] = val

    # taken from boto/ec2/connection.py
    def __build_filter_params__(self, params, filters):
        i = 1
        for name in filters:
            aws_name = name
            if not aws_name.startswith('tag:'):
                aws_name = name.replace('_', '-')
            params['Filter.%d.Name' % i] = aws_name
            value = filters[name]
            if not isinstance(value, list):
                value = [value]
            j = 1
            for v in value:
                params['Filter.%d.Value.%d' % (i, j)] = v
                j += 1
            i += 1

    def __build_dimension_param__(self, dimension, params):
        prefix = 'Dimensions.member'
        i = 0
        for dim_name in dimension:
            dim_value = dimension[dim_name]
            if dim_value:
                if isinstance(dim_value, basestring):
                    dim_value = [dim_value]
                for value in dim_value:
                    params['%s.%d.Name' % (prefix, i+1)] = dim_name
                    params['%s.%d.Value' % (prefix, i+1)] = value
                    i += 1
            else:
                params['%s.%d.Name' % (prefix, i+1)] = dim_name
                i += 1

    def __build_list_params__(self, params, items, label):
        if isinstance(items, basestring):
            items = [items]
        for index, item in enumerate(items):
            i = index + 1
            if isinstance(item, dict):
                for k, v in item.iteritems():
                    params[label % (i, 'Name')] = k
                    if v is not None:
                        params[label % (i, 'Value')] = v
            else:
                params[label % i] = item

    def __make_request__(self, action, params, endpoint='ec2'):
        url = '%s://%s:%s/%s?'%(self.protocol, self.host, self.port, endpoint)
        for param in params.keys():
            if params[param]==None:
                del params[param]
        params['Action'] = action
        params['_xsrf'] = self.xsrf
        data = urllib.urlencode(params)
        print "request : "+data
        try:
            req = urllib2.Request(url)
            self.__check_logged_in__(req)
            response = urllib2.urlopen(req, data)
            return json.loads(response.read())
        except urllib2.URLError, err:
            print "Error! "+str(err.code)

    def __make_request_walrus__(self, action, params):
        return self.__make_request__(action, params, 's3')

    def __make_cw_request__(self, action, params):
        return self.__make_request__(action, params, 'monitor')

    def __make_scale_request__(self, action, params):
        return self.__make_request__(action, params, 'autoscaling')

    def __make_elb_request__(self, action, params):
        return self.__make_request__(action, params, 'elb')

    ##
    # Zone methods
    ##
    def get_zones(self, filters=None):
        params = {}
        if filters:
            self.__build_filter_params__(params, filters)
        return self.__make_request__('DescribeAvailabilityZones', params)

    ##
    # Image methods
    ##
    def get_images(self, filters=None):
        params = {}
        if filters:
            self.__build_filter_params__(params, filters)
        return self.__make_request__('DescribeImages', params)

    def get_image_attribute(self, image_id, attribute='launchPermission'):
        return self.__make_request__('DescribeImageAttribute', {'ImageId': image_id, 'Attribute': attribute})

    def modify_image_attribute(self, image_id, attribute='launchPermission', operation='add', user_ids=None, groups=None):
        params = {'ImageId': image_id, 'Attribute': attribute, 'OperationType': operation}
        if user_ids:
            self.__add_param_list__(params, 'UserId', user_ids);
        if groups:
            self.__add_param_list__(params, 'UserGroup', groups);
        return self.__make_request__('ModifyImageAttribute', params)

    def reset_image_attribute(self, image_id, attribute='launchPermission'):
        return self.__make_request__('ResetImageAttribute', {'ImageId': image_id, 'Attribute': attribute})

    ##
    # Instance methods
    ##
    def get_instances(self):
        return self.__make_request__('DescribeInstances', {})

    def run_instances(self, image_id, min_count=1, max_count=1,
                      key_name=None, security_groups=None,
                      user_data=None, addressing_type=None,
                      instance_type='m1.small', placement=None,
                      kernel_id=None, ramdisk_id=None,
                      monitoring_enabled=False, subnet_id=None,
                      block_device_map=None,
                      disable_api_termination=False,
                      instance_initiated_shutdown_behavior=None,
                      private_ip_address=None,
                      placement_group=None, client_token=None,
                      security_group_ids=None,
                      additional_info=None, instance_profile_name=None,
                      instance_profile_arn=None, tenancy=None):
        params = {'ImageId':image_id,
                  'MinCount':min_count,
                  'MaxCount':max_count}
        if key_name:
            params['KeyName'] = key_name
        if security_group_ids:
            self.__add_param_list__(params, 'SecurityGroupId', security_group_ids);
        if security_groups:
            self.__add_param_list__(params, 'SecurityGroup', security_groups);
        if user_data:
            params['UserData'] = base64.b64encode(user_data)
        if addressing_type:
            params['AddressingType'] = addressing_type
        if instance_type:
            params['InstanceType'] = instance_type
        if placement:
            params['Placement.AvailabilityZone'] = placement
        if placement_group:
            params['Placement.GroupName'] = placement_group
        if tenancy:
            params['Placement.Tenancy'] = tenancy
        if kernel_id:
            params['KernelId'] = kernel_id
        if ramdisk_id:
            params['RamdiskId'] = ramdisk_id
        if monitoring_enabled:
            params['Monitoring.Enabled'] = 'true'
        if subnet_id:
            params['SubnetId'] = subnet_id
        if private_ip_address:
            params['PrivateIpAddress'] = private_ip_address
        if block_device_map:
            block_device_map.__build_list_params__(params)
        if disable_api_termination:
            params['DisableApiTermination'] = 'true'
        if instance_initiated_shutdown_behavior:
            val = instance_initiated_shutdown_behavior
            params['InstanceInitiatedShutdownBehavior'] = val
        if client_token:
            params['ClientToken'] = client_token
        if additional_info:
            params['AdditionalInfo'] = additional_info
        return self.__make_request__('RunInstances', params)

    def terminate_instances(self, instanceids):
        params = {}
        self.__add_param_list__(params, 'InstanceId', instanceids)
        return self.__make_request__('TerminateInstances', params)

    def stop_instances(self, instanceids):
        params = {}
        self.__add_param_list__(params, 'InstanceId', instanceids)
        return self.__make_request__('StopInstances', params)

    def start_instances(self, instanceids):
        params = {}
        self.__add_param_list__(params, 'InstanceId', instanceids)
        return self.__make_request__('StartInstances', params)

    def reboot_instances(self, instanceids):
        params = {}
        self.__add_param_list__(params, 'InstanceId', instanceids)
        return self.__make_request__('RebootInstances', params)

    def get_console_output(self, instanceid):
        return self.__make_request__('GetConsoleOutput', {'InstanceId': instanceid})

    def get_password(self, instanceid, keypair_file):
#        register_openers()
#        datagen, headers = multipart_encode({
#                                'Action': 'GetPassword',
#                                'InstanceId': instanceid,
#                                '_xsrf': self.xsrf,
#                                'priv_key': open(keypair_file)
#                                })
#
#        url = 'http://%s:%s/ec2?'%(self.host, self.port)
#        req = urllib2.Request(url, datagen, headers)
#        self.__check_logged_in__(req)
#        response = urllib2.urlopen(req)
#        return json.loads(response.read())
        pass

    ##
    # Keypair methods
    ##
    def get_keypairs(self, filters=None):
        params = {}
        if filters:
            self.__build_filter_params__(params, filters)
        return self.__make_request__('DescribeKeyPairs', params)

    def create_keypair(self, name):
        return self.__make_request__('CreateKeyPair', {'KeyName': name})

    def delete_keypair(self, name):
        return self.__make_request__('DeleteKeyPair', {'KeyName': name})

    ##
    # Security Group methods
    ##
    def get_security_groups(self, filters=None):
        params = {}
        if filters:
            self.__build_filter_params__(params, filters)
        return self.__make_request__('DescribeSecurityGroups', params)

    # returns True if successful
    def create_security_group(self, name, description):
        return self.__make_request__('CreateSecurityGroup', {'GroupName': name, 'GroupDescription': base64.encodestring(description)})

    # returns True if successful
    def delete_security_group(self, name=None, group_id=None):
        return self.__make_request__('DeleteSecurityGroup', {'GroupName': name, 'GroupId': group_id})

    # returns True if successful
    def authorize_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        params = {'GroupName': name, 'GroupId': group_id}
        for i in range(1, len(ip_protocol)+1):
            if src_security_group_name:
                params['IpPermissions.%d.Groups.1.GroupName'%i] = src_security_group_name[i-1]
            if src_security_group_owner_id:
                params['IpPermissions.%d.Groups.1.UserId'%i] = src_security_group_owner_id[i-1]
            if src_security_group_group_id:
                params['IpPermissions.%d.Groups.1.GroupId'%i] = src_security_group_group_id[i-1]
            params['IpPermissions.%d.IpProtocol'%i] = ip_protocol[i-1]
            params['IpPermissions.%d.FromPort'%i] = from_port[i-1]
            params['IpPermissions.%d.ToPort'%i] = to_port[i-1]
            if cidr_ip:
                params['IpPermissions.%d.IpRanges.1.CidrIp' % i] = cidr_ip[i-1]
        return self.__make_request__('AuthorizeSecurityGroupIngress', params)

    # returns True if successful
    def revoke_security_group(self, name=None,
                                 src_security_group_name=None,
                                 src_security_group_owner_id=None,
                                 ip_protocol=None, from_port=None, to_port=None,
                                 cidr_ip=None, group_id=None,
                                 src_security_group_group_id=None):
        params = {'GroupName': name, 'GroupId': group_id,
                  'IpPermissions.1.Groups.1.GroupName': src_security_group_name,
                  'IpPermissions.1.Groups.1.UserId': src_security_group_owner_id,
                  'IpPermissions.1.Groups.1.GroupId': src_security_group_group_id,
                  'IpPermissions.1.IpProtocol': ip_protocol,
                  'IpPermissions.1.FromPort': from_port,
                  'IpPermissions.1.ToPort': to_port}
        if cidr_ip:
            if not isinstance(cidr_ip, list):
                cidr_ip = [cidr_ip]
            for i, single_cidr_ip in enumerate(cidr_ip):
                params['IpPermissions.1.IpRanges.%d.CidrIp' % (i+1)] = \
                    single_cidr_ip
        return self.__make_request__('RevokeSecurityGroupIngress', params)

    ##
    # Addresss methods
    ##
    def get_addresses(self, filters=None):
        params = {}
        if filters:
            self.__build_filter_params__(params, filters)
        return self.__make_request__('DescribeAddresses', params)

    def allocate_address(self):
        return self.__make_request__('AllocateAddress', {})

    def release_address(self, publicip):
        return self.__make_request__('ReleaseAddress', {'PublicIp': publicip})

    def associate_address(self, publicip, instanceid):
        return self.__make_request__('AssociateAddress', {'PublicIp': publicip, 'InstanceId': instanceid})

    def disassociate_address(self, publicip):
        return self.__make_request__('DisassociateAddress', {'PublicIp': publicip})

    ##
    # Volume methods
    ##
    def get_volumes(self, filters=None):
        params = {}
        if filters:
            self.__build_filter_params__(params, filters)
        return self.__make_request__('DescribeVolumes', params)

    def create_volume(self, size, zone, snapshot_id):
        params = {'Size': size, 'AvailabilityZone': zone}
        if snapshot_id:
            params['SnapshotId'] = snapshot_id
        return self.__make_request__('CreateVolume', params)

    def delete_volume(self, volume_id):
        return self.__make_request__('DeleteVolume', {'VolumeId': volume_id})

    def attach_volume(self, volume_id, instance_id, device):
        return self.__make_request__('AttachVolume',
                    {'VolumeId': volume_id, 'InstanceId': instance_id, 'Device': device})

    def detach_volume(self, volume_id, force=False):
        return self.__make_request__('DetachVolume',
                    {'VolumeId': volume_id, 'Force': str(force)})

    ##
    # Snapshot methods
    ##
    def get_snapshots(self, filters=None):
        params = {}
        if filters:
            self.__build_filter_params__(params, filters)
        return self.__make_request__('DescribeSnapshots', params)

    def create_snapshot(self, volume_id, description=None):
        params = {'VolumeId': volume_id}
        if description:
            params['Description'] = base64.b64encode(description)
        return self.__make_request__('CreateSnapshot', params)

    def delete_snapshot(self, snapshot_id):
        return self.__make_request__('DeleteSnapshot', {'SnapshotId': snapshot_id})

    def get_snapshot_attribute(self, snapshot_id, attribute='createVolumePermission'):
        return self.__make_request__('DescribeSnapshotAttribute', {'SnapshotId': snapshot_id, 'Attribute': attribute})

    def modify_snapshot_attribute(self, snapshot_id, attribute='createVolumePermission', operation='add', users=None, groups=None):
        params = {'SnapshotId': snapshot_id, 'Attribute': attribute, 'OperationType': operation}
        if users:
            self.__add_param_list__(params, 'UserId', users);
        if groups:
            self.__add_param_list__(params, 'UserGroup', groups);
        return self.__make_request__('ModifySnapshotAttribute', params)

    def reset_snapshot_attribute(self, snapshot_id, attribute='createVolumePermission'):
        return self.__make_request__('ResetSnapshotAttribute', {'SnapshotId': snapshot_id, 'Attribute': attribute})

    ##
    # Register/deregister image
    ##
    def register_snapshot_as_image(self, snapshot_id, name):
        return self.__make_request__('RegisterImage', {'SnapshotId': snapshot_id, 'Name': name})
    def deregister_image(self, image_id):
        return self.__make_request__('DeregisterImage', {'ImageId': image_id})

    ##
    # Tag methods
    ##
    def get_tags(self, filters=None):
        params = {}
        if filters:
            self.__build_filter_params__(params, filters)
        return self.__make_request__('DescribeTags', params)

    def create_tags(self, resource_ids, tags):
        params = {}
        self.__add_param_list__(params, 'ResourceId', resource_ids)
        i = 1
        for key in tags.keys():
            value = tags[key]
            params['Tag.%d.Key'%i] = key
            if value is not None:
                params['Tag.%d.Value'%(i)] = value
            i += 1
        return self.__make_request__('CreateTags', params)

    def delete_tags(self, resource_ids, tags):
        params = {}
        self.__add_param_list__(params, 'ResourceId', resource_ids)
        i = 1
        for key in tags.keys():
            value = tags[key]
            params['Tag.%d.Key'%i] = key
            if value is not None:
                params['Tag.%d.Value'%(i)] = value
            i += 1
        return self.__make_request__('DeleteTags', params)

    ##
    # Optimize methods
    ##
    def get_dash_summary(self):
        return self.__make_request__('GetDashSummary', {})

    def set_data_interest(self, resources):
        params = {}
        self.__build_list_params__(params, resources, 'Resources.member.%d')
        return self.__make_request__('SetDataInterest', params)

    ##
    # Walrus methods
    ##
    def get_buckets(self):
        return self.__make_request_walrus__('DescribeBuckets', {})

    def get_objects(self, bucket):
        params = {'Bucket': bucket}
        return self.__make_request_walrus__('DescribeObjects', params)

    ##
    # CloudWatch methods
    ##
    def get_metric_statistics(self, period, start_time, end_time, metric_name,
                              namespace, statistics, dimensions=None,
                              unit=None):
        params = {'Period': period,
                  'MetricName': metric_name,
                  'Namespace': namespace,
                  'StartTime': start_time.isoformat(),
                  'EndTime': end_time.isoformat()}
        self.__build_list_params__(params, statistics, 'Statistics.member.%d')
        if dimensions:
            self.__build_dimension_param__(dimensions, params)
        if unit:
            params['Unit'] = unit
        
        return self.__make_cw_request__('GetMetricStatistics', params)

    def list_metrics(self, next_token=None, dimensions=None,
                     metric_name=None, namespace=None):
        params = {}
        if next_token:
            params['NextToken'] = next_token
        if dimensions:
            self.__build_dimension_param__(dimensions, params)
        if metric_name:
            params['MetricName'] = metric_name
        if namespace:
            params['Namespace'] = namespace

        return self.__make_cw_request__('ListMetrics', params)
    
    def put_metric_data(self, namespace, name, value=None, timestamp=None,
                        unit=None, dimensions=None, statistics=None):
        params = {'Namespace': namespace,
                  'Name': name}
        if value:
            params['Value'] = value
        if timestamp:
            params['Timestamp'] = timestamp.isoformat()
        if unit:
            params['Unit'] = unit
        if dimensions:
            self.__build_dimension_param__(dimensions, params)
        # TODO: how to format stats? API doc isn't clear
        if statistics:
            pass

        self.build_put_params(params, name, value=value, timestamp=timestamp,
            unit=unit, dimensions=dimensions, statistics=statistics)

        return self.__make_cw_request__('PutMetricData', params) 

    def describe_alarms(self, action_prefix=None, alarm_name_prefix=None, alarm_names=None, max_records=None, state_value=None, next_token=None):
        params = {}
        if action_prefix:
            params['ActionPrefix'] = action_prefix
        if alarm_name_prefix:
            params['AlarmNamePrefix'] = alarm_name_prefix
        if alarm_names:
            self.__build_list_params__(params, alarm_names, 'AlarmNames.member.%d')
        if max_records:
            params['MaxRecords'] = max_records
        if next_token:
            params['NextToken'] = next_token
        if state_value:
            params['StateValue'] = state_value
        return self.__make_cw_request__('DescribeAlarms', params)

    def delete_alarms(self, alarm_names):
        params = {}
        self.__build_list_params__(params, alarm_names, 'AlarmNames.member.%d')
        return self.__make_cw_request__('DeleteAlarms', params)

    def enable_alarm_actions(self, alarm_names):
        params = {}
        self.__build_list_params__(params, alarm_names, 'AlarmNames.member.%d')
        return self.__make_cw_request__('EnableAlarmActions', params)

    def disable_alarm_actions(self, alarm_names):
        params = {}
        self.__build_list_params__(params, alarm_names, 'AlarmNames.member.%d')
        return self.__make_cw_request__('DisableAlarmActions', params)
    
    def put_metric_alarm(self, alarm_name, metric_name, namespace, period, threshold, comparison_op, eval_periods, statistic,
                         actions_enabled=None, alarm_actions=[], alarm_desc=None, dimensions=[], insufficient_data_actions=[], ok_actions=[], unit=None):
        params = {'AlarmName': alarm_name, 'MetricName': metric_name, 'Namespace': namespace, 'Period': period, 'Threshold': threshold, 'ComparisonOperator': comparison_op, 'EvaluationPeriods': eval_periods, 'Statistic': statistic}
        if actions_enabled:
            params['ActionsEnabled'] = actions_enabled
        if alarm_actions:
            self.__build_list_params__(params, alarm_actions, 'AlarmActions.member.%d')
        if alarm_desc:
            params['AlarmDescription'] = alarm_desc
        if dimensions:
            self.__build_list_params__(params, dimensions, 'Dimensions.member.%d')
        if insufficient_data_actions:
            self.__build_list_params__(params, insufficient_data_actions, 'InsufficientDataActions.member.%d')
        if ok_actions:
            self.__build_list_params__(params, ok_actions, 'OKActions.member.%d')
        if unit:
            params['Unit'] = unit
        return self.__make_cw_request__('PutMetricAlarm', params)

    ##
    # Auto Scaling methods
    ##
    def create_auto_scaling_group(self, name, launch_config, zones=None, load_balancers=None,
                                  default_cooldown=None, hc_type=None, hc_period=None,
                                  desired_capacity=None, min_size=0, max_size=0,
                                  tags=None, termination_policies=None):
        params = {'AutoScalingGroupName':name,
                  'LaunchConfigurationName':launch_config,
                  'MinSize':min_size,
                  'MaxSize':max_size}
        if zones != None:
            self.__build_list_params__(params, zones, 'AvailabilityZones.member.%d')
        if load_balancers != None:
            self.__build_list_params__(params, load_balancers, 'LoadBalancerNames.member.%d')
        if default_cooldown != None:
            params['DefaultCooldown'] = default_cooldown
        if hc_type != None:
            params['HealthCheckType'] = hc_type
        if hc_period != None:
            params['HealthCheckGracePeriod'] = hc_period
        if desired_capacity != None:
            params['DesiredCapacity'] = desired_capacity
        if hc_period != None:
            params['HealthCheckGracePeriod'] = hc_period
        if tags != None:
            self.__build_list_params__(params, tags, 'Tags.member.%d')
        if termination_policies != None:
            self.__build_list_params__(params, termination_policies, 'TerminationPolicies.member.%d')

        return self.__make_scale_request__('CreateAutoScalingGroup', params) 

    def delete_auto_scaling_group(self, name, force_delete=False):
        params = {'AutoScalingGroupName':name}
        if force_delete:
            params['ForceDelete'] = 'true'

        return self.__make_scale_request__('DeleteAutoScalingGroup', params) 

    def get_all_groups(self, names=None, max_records=None, next_token=None):
        params = {}
        if names:
            self.__build_list_params__(params, names, 'Names.member.%d')
        if max_records:
            params['MaxRecords'] = max_records
        if next_token:
            params['NextToken'] = next_token

        return self.__make_scale_request__('DescribeAutoScalingGroups', params) 
    
    def get_all_autoscaling_instances(self, instance_ids=None,
                                      max_records=None, next_token=None):
        params = {}
        if instance_ids:
            self.__build_list_params__(params, instance_ids, 'InstanceIds.member.%d')
        if max_records:
            params['MaxRecords'] = max_records
        if next_token:
            params['NextToken'] = next_token

        return self.__make_scale_request__('DescribeAutoScalingInstances', params) 

    def set_desired_capacity(self, group_name, desired_capacity, honor_cooldown=False):
        params = {'AutoScalingGroupName':group_name,
                  'DesiredCapacity':desired_capacity}
        if honor_cooldown:
            params['HonorCooldown'] = 'true'

        return self.__make_scale_request__('SetDesiredCapacity', params) 

    def set_instance_health(self, instance_id, health_status, should_respect_grace_period=False):
        params = {'InstanceId':instance_id,
                  'HealthStatus':health_status}
        if should_respect_grace_period:
            params['ShouldRespectGracePeriod'] = 'true'

        return self.__make_scale_request__('SetInstanceHealth', params) 

    def terminate_instance(self, instance_id, decrement_capacity=False):
        params = {'InstanceId':instance_id}
        if decrement_capacity:
            params['ShouldDecrementDesiredCapacity'] = 'true'

        return self.__make_scale_request__('TerminateInstanceInAutoScalingGroup', params) 

    def create_launch_configuration(self, name, image_id, key_name=None, security_groups=None,
                                    user_data=None, instance_type=None, kernel_id=None,
                                    ramdisk_id=None, block_device_mappings=None,
                                    instance_monitoring=None, spot_price=None,
                                    instance_profile_name=None):
        params = {'LaunchConfigurationName':name,
                  'ImageId':image_id}
        if key_name != None:
            params['KeyName'] = key_name
        if security_groups != None:
            self.__build_list_params__(params, security_groups, 'SecurityGroups.member.%d')
        if user_data != None:
            params['UserData'] = user_data
        if instance_type != None:
            params['InstanceType'] = instance_type
        if kernel_id != None:
            params['KernelId'] = kernel_id
        if ramdisk_id != None:
            params['RamdiskId'] = ramdisk_id
        if block_device_mappings != None:
            self.__build_list_params__(params, block_device_mapings, 'BlockDeviceMappings.member.%d')
        if instance_monitoring != None:
            params['InstanceMonitoring'] = instance_monitoring
        if spot_price != None:
            params['SpotPrice'] = spot_price
        if instance_profile_name != None:
            params['IamInstanceProfile'] = instance_profile_name

        return self.__make_scale_request__('CreateLaunchConfiguration', params) 

    def delete_launch_configuration(self, launch_config_name):
        params = {'LaunchConfigurationName':launch_config_name}

        return self.__make_scale_request__('DeleteLaunchConfiguration', params) 

    def get_all_launch_configurations(self, configuration_names=None,
                           max_records=None, next_token=None):
        params = {}
        if configuration_names:
            self.__build_list_params__(params, configuration_names,
                                       'LaunchConfigurationNames.member.%d')
        if max_records:
            params['MaxRecords'] = max_records
        if next_token:
            params['NextToken'] = next_token

        return self.__make_scale_request__('DescribeLaunchConfigurations', params) 
    
    
    ##
    # elb methods
    ##
    def create_load_balancer(self, name, zones, listeners, subnets=None,
                             security_groups=None, scheme='internet-facing', callback=None):
        params = {'LoadBalancerName': name,
                  'Scheme': scheme}
        for index, listener in enumerate(listeners):
            i = index + 1
            protocol = listener[2].upper()
            params['Listeners.member.%d.LoadBalancerPort' % i] = listener[0]
            params['Listeners.member.%d.InstancePort' % i] = listener[1]
            params['Listeners.member.%d.Protocol' % i] = listener[2]
            if protocol == 'HTTPS' or protocol == 'SSL':
                params['Listeners.member.%d.SSLCertificateId' % i] = listener[3]
        if zones:
            self.__build_list_params__(params, zones, 'AvailabilityZones.member.%d')

        if subnets:
            self.__build_list_params__(params, subnets, 'Subnets.member.%d')

        if security_groups:
            self.__build_list_params__(params, security_groups,
                                    'SecurityGroups.member.%d')

        return self.__make_elb_request__('CreateLoadBalancer', params) 
    
    def delete_load_balancer(self, name, callback=None):
        params = {'LoadBalancerName': name}
        return self.__make_elb_request__('DeleteLoadBalancer', params) 

    def get_all_load_balancers(self, load_balancer_names=None, callback=None):
        params = {}
        if load_balancer_names:
            self.__build_list_params__(params, load_balancer_names,
                                   'LoadBalancerNames.member.%d')
        return self.__make_elb_request__('DescribeLoadBalancers', params) 

    def deregister_instances(self, load_balancer_name, instances, callback=None):
        params = {'LoadBalancerName': load_balancer_name}
        self.__build_list_params__(params, instances, 'Instances.member.%d')
        return self.__make_elb_request__('DeregisterInstancesFromLoadBalancer', params) 

    def register_instances(self, load_balancer_name, instances, callback=None):
        params = {'LoadBalancerName': load_balancer_name}
        self.__build_list_params__(params, instances, 'Instances.member.%d')
        return self.__make_elb_request__('RegisterInstancesWithLoadBalancer', params) 

    def create_load_balancer_listeners(self, name, listeners, callback=None):
        params = {'LoadBalancerName': name}
        for index, listener in enumerate(listeners):
            i = index + 1
            protocol = listener[2].upper()
            params['Listeners.member.%d.LoadBalancerPort' % i] = listener[0]
            params['Listeners.member.%d.InstancePort' % i] = listener[1]
            params['Listeners.member.%d.Protocol' % i] = listener[2]
            if protocol == 'HTTPS' or protocol == 'SSL':
                params['Listeners.member.%d.SSLCertificateId' % i] = listener[3]

        return self.__make_elb_request__('CreateLoadBalancerListeners', params) 
    
    def delete_load_balancer_listeners(self, name, ports, callback=None):
        params = {'LoadBalancerName': name}
        for index, port in enumerate(ports):
            params['LoadBalancerPorts.member.%d' % (index + 1)] = port
        return self.__make_elb_request__('DeleteLoadBalancerListeners', params) 

    def configure_health_check(self, name, health_check, callback=None):
        params = {'LoadBalancerName': name,
                  'HealthCheck.Timeout': health_check.timeout,
                  'HealthCheck.Target': health_check.target,
                  'HealthCheck.Interval': health_check.interval,
                  'HealthCheck.UnhealthyThreshold': health_check.unhealthy_threshold,
                  'HealthCheck.HealthyThreshold': health_check.healthy_threshold}
        return self.__make_elb_request__('ConfigureHealthCheck', params) 

    def describe_instance_health(self, load_balancer_name, instances=None, callback=None):
        params = {'LoadBalancerName': load_balancer_name}
        if instances:
            self.__build_list_params__(params, instances,
                                   'Instances.member.%d.InstanceId')
        return self.__make_elb_request__('DescribeInstanceHealth', params) 
