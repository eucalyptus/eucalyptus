import base64
import ConfigParser
import logging
import json
import tornado.web
import eucaconsole
import socket
import time
from xml.sax.saxutils import unescape
from M2Crypto import RSA
from boto.ec2.blockdevicemapping import BlockDeviceMapping, BlockDeviceType
from boto.exception import EC2ResponseError

from boto.ec2.ec2object import EC2Object
from .botoclcinterface import BotoClcInterface
from .botojsonencoder import BotoJsonEncoder
from .cachingclcinterface import CachingClcInterface
from .mockclcinterface import MockClcInterface
from .response import ClcError
from .response import Response

class ComputeHandler(eucaconsole.BaseHandler):

    def __normalize_instances__(self, instances):
        ret = []
        for res in instances:
            if issubclass(res.__class__, EC2Object):
                for inst in res.instances:
                    inst.reservation_id = res.id
                    inst.owner_id = res.owner_id
                    inst.groups = res.groups
                    if res.groups:
                        inst.group_name = res.groups[0].id
                    ret.append(inst)
            else:
                for inst in res['instances']:
                    inst['reservation_id'] = res['id']
                    inst['owner_id'] = res['owner_id']
                    inst['groups'] = res['groups']
                    if res['groups']:
                        inst['group_name'] = res['groups'][0]['id']
                    ret.append(inst)
        return ret

    # This method unescapes values that were escaped in the jsonbotoencoder.__sanitize_and_copy__ method
    def get_argument(self, name, default=tornado.web.RequestHandler._ARG_DEFAULT, strip=True):
        arg = super(ComputeHandler, self).get_argument(name, default, strip)
        if arg:
            return unescape(arg)
        else:
            return arg

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

    def handleRunInstances(self, action, clc, user_data_file):
        image_id = self.get_argument('ImageId')
        min = self.get_argument('MinCount', '1')
        max = self.get_argument('MaxCount', '1')
        key = self.get_argument('KeyName', None)
        groups = self.get_argument_list('SecurityGroup')
        sec_group_ids = self.get_argument_list('SecurityGroupId')
        if user_data_file:
            user_data = user_data_file
        else:
            user_data = self.get_argument('UserData', None)
            if user_data:
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
                        (self.get_argument('%s.DeleteOnTermination' % pre, '') == 'true')
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

        return self.__normalize_instances__([clc.run_instances(image_id, min_count=min, max_count=max,
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
                            tenancy=tenancy)])

    def handleImages(self, action, clc):
        if action == 'DescribeImages':
            owner = self.get_argument('Owner', None);
            if not owner:
                owners = None
            else:
                owners = [owner]
            return clc.get_all_images(owners)
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
        elif action == 'RegisterImage':
            image_location = self.get_argument('ImageLocation', None)
            name = self.get_argument('Name')
            description = self.get_argument('Description', None)
            description = base64.b64decode(description);
            architecture = self.get_argument('Architecture', None)
            kernel_id = self.get_argument('KernelId', None)
            ramdisk_id = self.get_argument('RamdiskId', None)
            root_dev_name = self.get_argument('RootDeviceName', None)
            snapshot_id = self.get_argument('SnapshotId', None)
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
                            (self.get_argument('%s.DeleteOnTermination' % pre, '') == 'true')
                bdm[dev_name] = block_dev_type
                idx += 1
                mapping = self.get_argument('BlockDeviceMapping.%d.DeviceName' % idx, None)
            if snapshot_id:
                rootbdm = BlockDeviceType()
                rootbdm.snapshot_id = snapshot_id
                bdm['/dev/sda1'] = rootbdm
            if len(bdm) == 0:
                bdm = None
            return clc.register_image(name, image_location, description, architecture, kernel_id, ramdisk_id, root_dev_name, bdm)

    def handleInstances(self, action, clc):
        if action == 'DescribeInstances':
            # apply transformation of data to normalize instances
            instances = clc.get_all_instances()
            return self.__normalize_instances__(instances)
        elif action == 'RunInstances':
            return self.handleRunInstances(action, clc, None)
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
            ret = clc.create_key_pair(name)
            self.user_session.keypair_cache[name] = ret.material
            return ret
        elif action == 'DeleteKeyPair':
            name = self.get_argument('KeyName')
            return clc.delete_key_pair(name)
        elif action == 'ImportKeyPair':
            name = self.get_argument('KeyName')
            material = base64.b64decode(self.get_argument('PublicKeyMaterial', None))
            return clc.import_key_pair(name, material)

    def handleGroups(self, action, clc):
        if action == 'DescribeSecurityGroups':
            return clc.get_all_security_groups()
        elif action == 'CreateSecurityGroup':
            name = self.get_argument('GroupName')
            desc = self.get_argument('GroupDescription')
            description = base64.b64decode(desc)
            return clc.create_security_group(name, description)
        elif action == 'DeleteSecurityGroup':
            name = self.get_argument('GroupName', None)
            group_id = self.get_argument('GroupId', None)
            return clc.delete_security_group(name, group_id)
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
            ret = False
            for i in range(len(ip_protocol)):
                ret = clc.authorize_security_group(name,
                                 src_security_group_name[i] if src_security_group_name else None,
                                 src_security_group_owner_id[i] if src_security_group_owner_id else None,
                                 ip_protocol[i], from_port[i], to_port[i],
                                 cidr_ip[i] if cidr_ip else None, group_id[i] if group_id else None,
                                 src_security_group_group_id[i] if src_security_group_group_id else None)
            return ret
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
            ret = False
            for i in range(len(ip_protocol)):
                ret = clc.revoke_security_group(name,
                                 src_security_group_name[i] if src_security_group_name else None,
                                 src_security_group_owner_id[i] if src_security_group_owner_id else None,
                                 ip_protocol[i], from_port[i], to_port[i],
                                 cidr_ip[i] if cidr_ip else None, group_id[i] if group_id else None,
                                 src_security_group_group_id[i] if src_security_group_group_id else None)
            return ret

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
            return clc.detach_volume(volumeid, force)

    def handleSnapshots(self, action, clc):
        if action == "DescribeSnapshots":
            return clc.get_all_snapshots()
        elif action == 'CreateSnapshot':
            volumeid = self.get_argument('VolumeId')
            description = self.get_argument('Description', None)
            description = base64.b64decode(description)
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
                self.user_session.clc = BotoClcInterface(eucaconsole.config.get('server', 'clchost'),
                                                         self.user_session.access_key,
                                                         self.user_session.secret_key,
                                                         self.user_session.session_token)
            # could make this conditional, but add caching always for now
            self.user_session.clc = CachingClcInterface(self.user_session.clc, eucaconsole.config)

        self.user_session.session_lifetime_requests += 1

        logging.warn(">>>> get being called in api.py. Fix this now! <<<<");

        ret = []
        try:
            action = self.get_argument("Action")
            # bump session counter if this was a user-initiated action
            if action.find('Describe') == -1:
                self.user_session.session_last_used = time.time()
                self.check_xsrf_cookie()
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
            try:
                if(eucaconsole.config.get('test','apidelay')):
                    time.sleep(int(eucaconsole.config.get('test','apidelay'))/1000.0);
            except ConfigParser.NoOptionError:
                pass
            self.set_header("Content-Type", "application/json;charset=UTF-8")
            self.write(data)
        except EC2ResponseError as err:
            ret = ClcError(err.status, err.reason, err.errors[0][1])
            self.set_status(err.status);
            self.set_header("Content-Type", "application/json;charset=UTF-8")
            self.write(json.dumps(ret, cls=BotoJsonEncoder))
        except Exception as ex:
            if isinstance(ex, socket.timeout):
                ret = ClcError(504, 'Timed out', '')
                self.set_status(504);
                self.set_header("Content-Type", "application/json;charset=UTF-8")
                self.write(json.dumps(ret, cls=BotoJsonEncoder))
            else:
                logging.error("Could not fullfil request, exception to follow")
                logging.exception(ex)
                ret = ClcError(500, ex.message, '')
                self.set_status(500);
                self.set_header("Content-Type", "application/json;charset=UTF-8")
                self.write(json.dumps(ret, cls=BotoJsonEncoder))

    def post(self):
        if not self.authorized():
            raise tornado.web.HTTPError(401, "not authorized")
        if not(self.user_session.clc):
            if self.should_use_mock():
                self.user_session.clc = MockClcInterface()
            else:
                self.user_session.clc = BotoClcInterface(eucaconsole.config.get('server', 'clchost'),
                                                         self.user_session.access_key,
                                                         self.user_session.secret_key,
                                                         self.user_session.session_token)
            # could make this conditional, but add caching always for now
            self.user_session.clc = CachingClcInterface(self.user_session.clc, eucaconsole.config)

        self.user_session.session_lifetime_requests += 1

        try:
            action = self.get_argument("Action")
            if action.find('Describe') == -1:
                self.user_session.session_last_used = time.time()
                self.check_xsrf_cookie()

            # this call returns a file vs. a json envelope, so it is self-contained
            if action == 'GetKeyPairFile':
                name = self.get_argument('KeyName')
                result = self.user_session.keypair_cache[name]
                self.set_header("Content-Type", "application/x-pem-file;charset=ISO-8859-1")
                self.set_header("Content-Disposition", "attachment; filename=\"" + name + '.pem"')
                self.write(result)
                del self.user_session.keypair_cache[name]
                return

            if action == 'RunInstances':
                user_data_file = []
                try:
                    user_data_file = self.request.files['user_data_file']
                except KeyError:
                    pass
                if len(user_data_file) > 0:
                    ret = self.handleRunInstances(action, self.user_session.clc, user_data_file[0].body)
                else:
                    ret = self.handleRunInstances(action, self.user_session.clc, None)
            elif action == 'DescribeAvailabilityZones':
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
            elif action == 'GetPassword':
                instanceid = self.get_argument('InstanceId')
                passwd_data = self.user_session.clc.get_password_data(instanceid)
                priv_key_file = self.request.files['priv_key']
                user_priv_key = RSA.load_key_string(priv_key_file[0].body)
                string_to_decrypt = base64.b64decode(passwd_data)
                ret = user_priv_key.private_decrypt(string_to_decrypt, RSA.pkcs1_padding)
                ret = {'instance':instanceid, 'password': ret} # wrap all responses in an object for security purposes
            ret = Response(ret) # wrap all responses in an object for security purposes
            data = json.dumps(ret, cls=BotoJsonEncoder, indent=2)
            try:
                if(eucaconsole.config.get('test','apidelay')):
                    time.sleep(int(eucaconsole.config.get('test','apidelay'))/1000.0);
            except ConfigParser.NoOptionError:
                pass
            self.set_header("Content-Type", "application/json;charset=UTF-8")
            self.write(data)
        except EC2ResponseError as err:
            ret = ClcError(err.status, err.reason, err.errors[0][1])
            self.set_status(err.status);
            self.set_header("Content-Type", "application/json;charset=UTF-8")
            self.write(json.dumps(ret, cls=BotoJsonEncoder))
        except Exception as ex:
            if isinstance(ex, socket.timeout):
                ret = ClcError(504, 'Timed out', None)
                self.set_status(504);
                self.set_header("Content-Type", "application/json;charset=UTF-8")
                self.write(json.dumps(ret, cls=BotoJsonEncoder))
            else:
                logging.error("Could not fullfil request, exception to follow")
                logging.exception(ex)
                ret = ClcError(500, ex.message, None)
                self.set_status(500);
                self.set_header("Content-Type", "application/json;charset=UTF-8")
                self.write(json.dumps(ret, cls=BotoJsonEncoder))
