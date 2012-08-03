import tornado.web
import server
import json

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
            print "cidr = "+cidr_ip[0]
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
            instanceid = self.get_argument('InstanceId')
            device = self.get_argument('Device')
            force = self.get_argument('Force', False)
            return clc.detach_volume(volumeid, instanceid, device, force)

    def handleSnapshots(self, action, clc):
        if action == "DescribeSnapshots":
            return clc.get_all_snapshots()
        elif action == 'CreateSnapshot':
            volumeid = self.get_argument('VolumeId')
            description = self.get_argument('Description')
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
            self.user_session.clc = CachingClcInterface(self.user_session.clc, int(server.config.get('eui', 'pollfreq')))

        ret = []
        action = self.get_argument("Action")
        if action == 'DescribeAvailabilityZones':
            ret = self.user_session.clc.get_all_zones()
        if action == 'DescribeImages':
            ret = self.user_session.clc.get_all_images()
        elif action == 'DescribeInstances':
            ret = self.user_session.clc.get_all_instances()
        elif action == 'DescribeAddresses':
            ret = self.user_session.clc.get_all_addresses()
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
        self.write(data)
