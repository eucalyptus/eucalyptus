import tornado.web
import server
import json

from botoclcinterface import BotoClcInterface
from mockclcinterface import MockClcInterface
from botojsonencoder import BotoJsonEncoder

class ComputeHandler(server.BaseHandler):
    @tornado.web.authenticated

    def get_argument_list(self, name):
        ret = []
        index = 1
        val = self.get_argument('%s.%d' % (name, index))
        while val:
          ret.append(val)
          index = index + 1
          val = self.get_argument('%s.%d' % (name, index))
        return ret

    def handleKeypairs(self, action, clc):
        if action == 'DescribeKeyPairs':
          return clc.get_all_key_pairs()
        elif action == 'AddKeyPair':
          name = self.get_argument('KeyName')
          clc.create_key_pair(name);
        elif action == 'DeleteKeyPair':
          name = self.get_argument('KeyName')
          clc.delete_key_pair(name);

    def handleVolumes(self, action, clc):
        if action == 'DescribeVolumes':
          ret = clc.get_all_volumes()
        elif action == 'CreateVolume':
          size = self.get_argument('Size')
          zone = self.get_argument('AvailabilityZone')
          snapshotid = self.get_argument('SnapshotId')
          clc.create_volume(size, zone, snapshotid);
        elif action == 'DeleteVolume':
          volumeid = self.get_argument('VolumeId')
          clc.delete_volume(volumeid);
        elif action == 'AttachVolume':
          volumeid = self.get_argument('VolumeId')
          instanceid = self.get_argument('InstanceId')
          device = self.get_argument('Device')
          clc.attach_volume(volumeid, instanceid, device);
        elif action == 'DetachVolume':
          volumeid = self.get_argument('VolumeId')
          instanceid = self.get_argument('InstanceId')
          device = self.get_argument('Device')
          force = self.get_argument('Force')
          clc.detach_volume(volumeid, instanceid, device, force);

    def handleSnapshots(self, action, clc):
        if action == "DescribeSnapshots":
          ret = clc.get_all_snapshots()
        elif action == 'CreateSnapshot':
          volumeid = self.get_argument('VolumeId')
          description = self.get_argument('Description')
          clc.create_snapshot(volumeid, description);
        elif action == 'DeleteSnapshot':
          snapshotid = self.get_argument('SnapshotId')
          clc.delete_snapshot(snapshotid);
        elif action == 'DescribeSnapshotAttribute':
          snapshotid = self.get_argument('SnapshotId')
          attribute = self.get_argument('Attribute')
          clc.get_snapshot_attribute(snapshotid, attribute);
        elif action == 'ModifySnapshotAttribute':
          snapshotid = self.get_argument('SnapshotId')
          attribute = self.get_argument('Attribute')
          operation = self.get_argument('OperationType')
          users = self.get_argument_list('UserId')
          groups = self.get_argument_list('UsersGroup')
          clc.modify_snapshot_attribute(snapshotid, attribute, operation, users, groups);
        elif action == 'ResetSnapshotAttribute':
          snapshotid = self.get_argument('SnapshotId')
          attribute = self.get_argument('Attribute')
          clc.reset_snapshot_attribute(snapshotid, attribute);

    ##
    # This is the main entry point for API calls for EC2 from the browser
    # other calls are delegated to handler methods based on resource type
    #
    def get(self):
        self.validate_session()
        if self.should_use_mock():
          clc = MockClcInterface()
        else:
          clc = BotoClcInterface(server.config.get('eui', 'clchost'),
                  self.session.access_id, self.session.secret_key)
        ret = []
        action = self.get_argument("Action")
        if action == "DescribeImages":
          ret = clc.get_all_images()
        elif action == "DescribeInstances":
          ret = clc.get_all_instances()
        elif action == "DescribeAddresses":
          ret = clc.get_all_addresses()
        elif action.find('KeyPair') > -1:
          ret = self.handleKeypairs(action, clc)
        elif action == "DescribeSecurityGroups":
          ret = clc.get_all_security_groups()
        elif action.find('Volume') > -1:
          ret = self.handleVolumes(action, clc)
        elif action.find('Snapshot') > -1:
          ret = self.handleSnapshots(action, clc)
        self.write(json.dumps(ret, cls=BotoJsonEncoder, indent=2))

