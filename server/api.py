import tornado.web
import server
import json

from botoclcinterface import BotoClcInterface
from mockclcinterface import MockClcInterface
from botojsonencoder import BotoJsonEncoder

class ComputeHandler(server.BaseHandler):
    @tornado.web.authenticated

    def handleKeypairs(self, action, clc):
        if action == 'DescribeKeyPairs':
          return clc.get_all_key_pairs()
        elif action == 'AddKeyPair':
          pass
        elif action == 'DeleteKeyPair':
          pass

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
        elif action == "DescribeVolumes":
          ret = clc.get_all_volumes()
        elif action == "DescribeSnapshots":
          ret = clc.get_all_snapshots()
        self.write(json.dumps(ret, cls=BotoJsonEncoder, indent=2))

