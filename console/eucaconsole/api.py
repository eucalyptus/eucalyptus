import base64
import ConfigParser
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
from boto.exception import EC2ResponseError
from eucaconsole.threads import Threads

from .botoclcinterface import BotoClcInterface
from .botojsonencoder import BotoJsonEncoder
from .cachingclcinterface import CachingClcInterface
from .mockclcinterface import MockClcInterface
from .response import ClcError
from .response import Response

class ComputeHandler(eucaconsole.BaseHandler):

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
            return clc.get_all_images(owners, callback)
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
                            (self.get_argument('%s.Ebs.DeleteOnTermination' % pre, '') == 'true')
                bdm[dev_name] = block_dev_type
                idx += 1
                mapping = self.get_argument('BlockDeviceMapping.%d.DeviceName' % idx, None)
            if snapshot_id:
                rootbdm = BlockDeviceType()
                rootbdm.snapshot_id = snapshot_id
                bdm['/dev/sda1'] = rootbdm
            if len(bdm) == 0:
                bdm = None
            return clc.register_image(name, image_location, description, architecture, kernel_id, ramdisk_id, root_dev_name, bdm, callback)

    def handleInstances(self, action, clc, callback=None):
        if action == 'DescribeInstances':
            # apply transformation of data to normalize instances
            return clc.get_all_instances(callback)
        elif action == 'RunInstances':
            return self.handleRunInstances(action, clc, None, callback)
        elif action == 'TerminateInstances':
            instance_ids = self.get_argument_list('InstanceId')
            return clc.terminate_instances(instance_ids, callback)
        elif action == 'StopInstances':
            instance_ids = self.get_argument_list('InstanceId')
            return clc.stop_instances(instance_ids, callback)
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
            return clc.get_all_key_pairs(callback)
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
            return clc.get_all_security_groups(callback)
        elif action == 'CreateSecurityGroup':
            name = self.get_argument('GroupName')
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
            return clc.get_all_addresses(callback)
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
            return clc.get_all_volumes(callback)
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
            return clc.get_all_snapshots(callback)
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

    def handleGetPassword(self, clc, callback):
        instanceid = self.get_argument('InstanceId')
        Threads.instance().runThread(self.__get_password_cb__, ({'instanceid':instanceid}, callback))

    def __get_password_cb__(self, kwargs, callback):
        try:
            passwd_data = self.user_session.clc.get_password_data(kwargs['instanceid'])
            print "got password data"+passwd_data
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
                try:
                    host = eucaconsole.config.get('test', 'ec2.endpoint')
                except ConfigParser.Error:
                    pass
                self.user_session.clc = BotoClcInterface(host,
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
                self.finish()
                del self.user_session.keypair_cache[name]
                return

            if action == 'RunInstances':
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
                self.user_session.clc.get_all_zones(self.callback)
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
            elif action == 'GetPassword':
                self.handleGetPassword(self.user_session.clc, self.callback)

        except Exception as ex:
            logging.error("Could not fullfil request, exception to follow")
            logging.error("Since we got here, client likelly not notified either!")
            logging.exception(ex)

    def keycache_callback(self, response, name, callback):
        # respond to the client
        callback(response)
        # now, cache the response
        if not(response.error):
            self.user_session.keypair_cache[name] = response.data.material

    # async calls end up back here so we can check error status and reply appropriately
    def callback(self, response):
        if response.error:
            err = response.error
            ret = '[]'
            if isinstance(err, EC2ResponseError):
                ret = ClcError(err.status, err.reason, err.errors[0][1])
                self.set_status(err.status);
            elif issubclass(err.__class__, Exception):
                if isinstance(err, socket.timeout):
                    ret = ClcError(504, 'Timed out', None)
                    self.set_status(504);
                else:
                    ret = ClcError(500, err.message, None)
                    self.set_status(500);
            self.set_header("Content-Type", "application/json;charset=UTF-8")
            self.write(json.dumps(ret, cls=BotoJsonEncoder))
            self.finish()
            logging.exception(err)
        else:
            try:
                try:
                    if(eucaconsole.config.get('test','apidelay')):
                        time.sleep(int(eucaconsole.config.get('test','apidelay'))/1000.0);
                except ConfigParser.NoOptionError:
                    pass
                ret = Response(response.data) # wrap all responses in an object for security purposes
                data = json.dumps(ret, cls=BotoJsonEncoder, indent=2)
                self.set_header("Content-Type", "application/json;charset=UTF-8")
                self.write(data)
                self.finish()
            except Exception, err:
                print err
