import tornado.web
import server
import json
import time
from boto.ec2.blockdevicemapping import BlockDeviceMapping

from boto.ec2.ec2object import EC2Object
from .botoclcinterface import BotoClcInterface
from .botojsonencoder import BotoJsonEncoder
from .cachingclcinterface import CachingClcInterface
from .mockclcinterface import MockClcInterface
from .response import Response

class ComputeHandler(server.BaseHandler):

    def get_argument_list(self, name, name_suffix=None):
        ret = []
        index = 1
        pattern = name+'.%d'
        if name_suffix:
            pattern = pattern+'.'+name_suffix
        val = self.get_argument(pattern % index, None)
        while val:
            ret.append(val)
            index = index + 1
            val = self.get_argument(pattern % index, None)
        return ret

    def handleImages(self, action, clc):
        if action == 'DescribeImages':
            return clc.get_all_images()
        elif action == 'DescribeImageAttribute':
            imageid = self.get_argument('ImageId')
            attribute = self.get_argument('Attribute')
            return clc.get_image_attribute(imageid, attribute)
        elif action == 'ModifyImageAttribute':
            imageid = self.get_argument('ImageId')
            attribute = self.get_argument('Attribute')
            operation = self.get_argument('OperationType')
            users = self.get_argument_list('UserId')
            groups = self.get_argument_list('UserGroup')
            return clc.modify_image_attribute(imageid, attribute, operation, users, groups)
        elif action == 'ResetImageAttribute':
            imageid = self.get_argument('ImageId')
            attribute = self.get_argument('Attribute')
            return clc.reset_image_attribute(imageid, attribute)

    def handleInstances(self, action, clc):
        if action == 'DescribeInstances':
            # apply transformation of data to normalize instances
            instances = clc.get_all_instances()
            ret = []
            for res in instances:
                if issubclass(res.__class__, EC2Object):
                    for inst in res.instances:
                        inst.reservation_id = res.id
                        inst.owner_id = res.owner_id
                        inst.groups = res.groups
                        inst.group_name = res.groups[0].id
                        ret.append(inst)
                else:
                    for inst in res['instances']:
                        inst['reservation_id'] = res['id']
                        inst['owner_id'] = res['owner_id']
                        inst['groups'] = res['groups']
                        inst['group_name'] = res['groups'][0]['id']
                        ret.append(inst)
            return ret
        elif action == 'RunInstances':
            image_id = self.get_argument('ImageId');
            min = self.get_argument('MinCount', '1');
            max = self.get_argument('MaxCount', '1');
            key = self.get_argument('KeyName', None);
            groups = self.get_argument_list('SecurityGroup')
            sec_group_ids = self.get_argument_list('SecurityGroupId')
            user_data = self.get_argument('UserData', None);
            addr_type = self.get_argument('AddressingType', None);
            vm_type = self.get_argument('InstanceType', None);
            placement = self.get_argument('Placement.AvailabilityZone', None);
            placement_group = self.get_argument('Placement.GroupName', None);
            tenancy = self.get_argument('Placement.Tenancy', None);
            kernel = self.get_argument('KernelId', None);
            ramdisk = self.get_argument('RamdiskId', None);
            monitoring=False
            if self.get_argument('Monitoring.Enabled', '') == 'true':
                monitoring=True
            subnet = self.get_argument('SubnetId', None);
            private_ip = self.get_argument('PrivateIpAddress', None);
            # get block device mappings
            bdm = []
            mapping = self.get_argument('BlockDeviceMapping.1.DeviceName', None)
            idx = 1
            while mapping:
                pre = 'BlockDeviceMapping.$d' % idx
                block_dev_mapping = BlockDeviceMapping()
                block_dev_mapping.dev_name = mapping
                block_dev_mapping.ephemeral_name = self.get_argument('%s.VirtualName' % pre, None)
                if not(block_dev_mapping.ephemeral_name):
                    block_dev_mapping.no_device = \
                            (self.get_argument('%s.NoDevice' % pre, '') == 'true')
                    block_dev_mapping.snapshot_id = \
                            self.get_argument('%s.Ebs.SnapshotId' % pre, None)
                    block_dev_mapping.size = \
                            self.get_argument('%s.Ebs.VolumeSize' % pre, None)
                    block_dev_mapping.delete_on_termination = \
                            (self.get_argument('%s.DeleteOnTermination' % pre, '') == 'true')
                bdm.append(block_dev_mapping)
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
                                tenancy=tenancy)
        elif action == 'TerminateInstances':
            instance_ids = self.get_argument_list('InstanceId')
            return clc.terminate_instances(instance_ids)
        elif action == 'StopInstances':
            instance_ids = self.get_argument_list('InstanceId')
            return clc.stop_instances(instance_ids)
        elif action == 'StartInstances':
            instance_ids = self.get_argument_list('InstanceId')
            return clc.start_instances(instance_ids)
        elif action == 'RebootInstances':
            instance_ids = self.get_argument_list('InstanceId')
            return clc.reboot_instances(instance_ids)
        elif action == 'GetConsoleOutput':
            instance_id = self.get_argument('InstanceId')
            return clc.get_console_output(instance_id)

    def handleKeypairs(self, action, clc):
        if action == 'DescribeKeyPairs':
            return clc.get_all_key_pairs()
        elif action == 'CreateKeyPair':
            name = self.get_argument('KeyName')
            return clc.create_key_pair(name)
        elif action == 'DeleteKeyPair':
            name = self.get_argument('KeyName')
            return clc.delete_key_pair(name)

    def handleGroups(self, action, clc):
        if action == 'DescribeSecurityGroups':
            return clc.get_all_security_groups()
        elif action == 'CreateSecurityGroup':
            name = self.get_argument('GroupName')
            desc = self.get_argument('GroupDescription')
            return clc.create_security_group(name, desc)
        elif action == 'DeleteSecurityGroup':
            name = self.get_argument('GroupName', None)
            group_id = self.get_argument('GroupId', None)
            return clc.delete_security_group(name, group_id)
        elif action == 'AuthorizeSecurityGroupIngress':
            name = self.get_argument('GroupName', None)
            group_id = self.get_argument('GroupId', None)
            ip_protocol = self.get_argument('IpPermissions.1.IpProtocol', None)
            from_port = self.get_argument('IpPermissions.1.FromPort', None)
            to_port = self.get_argument('IpPermissions.1.ToPort', None)
            src_security_group_name = self.get_argument('IpPermissions.1.Groups.1.GroupName', None)
            src_security_group_owner_id = self.get_argument('IpPermissions.1.Groups.1.UserId', None)
            src_security_group_group_id = self.get_argument('IpPermissions.1.Groups.1.GroupId', None)
            cidr_ip = self.get_argument_list('IpPermissions.1.IpRanges', 'CidrIp')
            return clc.authorize_security_group(name,
                                 src_security_group_name,
                                 src_security_group_owner_id,
                                 ip_protocol, from_port, to_port,
                                 cidr_ip, group_id,
                                 src_security_group_group_id)
        elif action == 'RevokeSecurityGroupIngress':
            name = self.get_argument('GroupName', None)
            group_id = self.get_argument('GroupId', None)
            ip_protocol = self.get_argument('IpPermissions.1.IpProtocol', None)
            from_port = self.get_argument('IpPermissions.1.FromPort', None)
            to_port = self.get_argument('IpPermissions.1.ToPort', None)
            src_security_group_name = self.get_argument('IpPermissions.1.Groups.1.GroupName', None)
            src_security_group_owner_id = self.get_argument('IpPermissions.1.Groups.1.UserId', None)
            src_security_group_group_id = self.get_argument('IpPermissions.1.Groups.1.GroupId', None)
            cidr_ip = self.get_argument_list('IpPermissions.1.IpRanges', 'CidrIp')
            return clc.revoke_security_group(name,
                                 src_security_group_name,
                                 src_security_group_owner_id,
                                 ip_protocol, from_port, to_port,
                                 cidr_ip, group_id,
                                 src_security_group_group_id)

    def handleAddresses(self, action, clc):
        if action == 'DescribeAddresses':
            return clc.get_all_addresses()
        elif action == 'AllocateAddress':
            return clc.allocate_address()
        elif action == 'ReleaseAddress':
            publicip = self.get_argument('PublicIp')
            return clc.release_address(publicip)
        elif action == 'AssociateAddress':
            publicip = self.get_argument('PublicIp')
            instanceid = self.get_argument('InstanceId')
            return clc.associate_address(publicip, instanceid)
        elif action == 'DisassociateAddress':
            publicip = self.get_argument('PublicIp')
            return clc.disassociate_address(publicip)

    def handleVolumes(self, action, clc):
        if action == 'DescribeVolumes':
            return clc.get_all_volumes()
        elif action == 'CreateVolume':
            size = self.get_argument('Size')
            zone = self.get_argument('AvailabilityZone')
            snapshotid = self.get_argument('SnapshotId', None)
            return clc.create_volume(size, zone, snapshotid)
        elif action == 'DeleteVolume':
            volumeid = self.get_argument('VolumeId')
            return clc.delete_volume(volumeid)
        elif action == 'AttachVolume':
            volumeid = self.get_argument('VolumeId')
            instanceid = self.get_argument('InstanceId')
            device = self.get_argument('Device')
            return clc.attach_volume(volumeid, instanceid, device)
        elif action == 'DetachVolume':
            volumeid = self.get_argument('VolumeId')
            force = self.get_argument('Force', False)
            return clc.detach_volume(volumeid, None, None, force)

    def handleSnapshots(self, action, clc):
        if action == "DescribeSnapshots":
            return clc.get_all_snapshots()
        elif action == 'CreateSnapshot':
            volumeid = self.get_argument('VolumeId')
            description = self.get_argument('Description', None)
            return clc.create_snapshot(volumeid, description)
        elif action == 'DeleteSnapshot':
            snapshotid = self.get_argument('SnapshotId')
            return clc.delete_snapshot(snapshotid)
        elif action == 'DescribeSnapshotAttribute':
            snapshotid = self.get_argument('SnapshotId')
            attribute = self.get_argument('Attribute')
            return clc.get_snapshot_attribute(snapshotid, attribute)
        elif action == 'ModifySnapshotAttribute':
            snapshotid = self.get_argument('SnapshotId')
            attribute = self.get_argument('Attribute')
            operation = self.get_argument('OperationType')
            users = self.get_argument_list('UserId')
            groups = self.get_argument_list('UsersGroup')
            return clc.modify_snapshot_attribute(snapshotid, attribute, operation, users, groups)
        elif action == 'ResetSnapshotAttribute':
            snapshotid = self.get_argument('SnapshotId')
            attribute = self.get_argument('Attribute')
            return clc.reset_snapshot_attribute(snapshotid, attribute)

    ##
    # This is the main entry point for API calls for EC2 from the browser
    # other calls are delegated to handler methods based on resource type
    #
    def get(self):
        if not self.authorized():
            raise tornado.web.HTTPError(401, "not authorized")
        if not(self.user_session.clc):
            if self.should_use_mock():
                self.user_session.clc = MockClcInterface()
            else:
                self.user_session.clc = BotoClcInterface(server.config.get('eui', 'clchost'),
                                                         self.user_session.access_key,
                                                         self.user_session.secret_key)
            # could make this conditional, but add caching always for now
            self.user_session.clc = CachingClcInterface(self.user_session.clc, server.config)

        ret = []
        action = self.get_argument("Action")
        if action == 'DescribeAvailabilityZones':
            ret = self.user_session.clc.get_all_zones()
        elif action.find('Image') > -1:
            ret = self.handleImages(action, self.user_session.clc)
        elif action.find('Instance') > -1 or action == 'GetConsoleOutput':
            ret = self.handleInstances(action, self.user_session.clc)
        elif action.find('Address') > -1:
            ret = self.handleAddresses(action, self.user_session.clc)
        elif action.find('KeyPair') > -1:
            ret = self.handleKeypairs(action, self.user_session.clc)
        elif action.find('SecurityGroup') > -1:
            ret = self.handleGroups(action, self.user_session.clc)
        elif action.find('Volume') > -1:
            ret = self.handleVolumes(action, self.user_session.clc)
        elif action.find('Snapshot') > -1:
            ret = self.handleSnapshots(action, self.user_session.clc)
        ret = Response(ret) # wrap all responses in an object for security purposes
        data = json.dumps(ret, cls=BotoJsonEncoder, indent=2)
        if(server.config.get('test','apidelay')):
            time.sleep(int(server.config.get('test','apidelay'))/1000.0);
        self.write(data)
