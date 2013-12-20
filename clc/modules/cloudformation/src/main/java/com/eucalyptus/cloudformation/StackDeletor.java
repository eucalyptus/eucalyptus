package com.eucalyptus.cloudformation;

import com.eucalyptus.cloudformation.entity.StackEntityManager;
import com.eucalyptus.cloudformation.entity.StackEventEntityManager;
import com.eucalyptus.cloudformation.entity.StackResourceEntity;
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager;
import com.eucalyptus.cloudformation.resources.AWSEC2Instance;
import com.eucalyptus.cloudformation.resources.Resource;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Created by ethomas on 12/19/13.
 */
public class StackDeletor extends Thread {
  private Stack stack;
  private String userId;

  public StackDeletor(Stack stack, String userId) {
    this.stack = stack;
    this.userId = userId;
  }

  public void run() {
    for (StackResourceEntity stackResourceEntity: StackResourceEntityManager.getStackResources(stack.getStackName())) {
      Resource resource = null;
      if (stackResourceEntity.getResourceType().equals("AWS::EC2::Instance")) {
        AWSEC2Instance awsec2Instance = new AWSEC2Instance();
        awsec2Instance.setOwnerUserId(userId);
        awsec2Instance.setLogicalResourceId(stackResourceEntity.getLogicalResourceId());
        awsec2Instance.setType(stackResourceEntity.getResourceType());
        awsec2Instance.setPhysicalResourceId(awsec2Instance.getPhysicalResourceId());
        resource = awsec2Instance;
      }
      try {
        resource.delete();
      } catch (Exception ex) {
        // TODO: put in events, etc
      }
    }
    StackResourceEntityManager.deleteStackResources(stack.getStackName());
    StackEventEntityManager.deleteStackEvents(stack.getStackName());
    StackEntityManager.deleteStack(stack.getStackName());
  }
}
