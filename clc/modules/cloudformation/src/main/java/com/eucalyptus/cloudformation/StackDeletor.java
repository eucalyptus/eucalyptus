package com.eucalyptus.cloudformation;

import com.eucalyptus.cloudformation.entity.StackEntityManager;
import com.eucalyptus.cloudformation.entity.StackEventEntityManager;
import com.eucalyptus.cloudformation.entity.StackResourceEntity;
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager;
import com.eucalyptus.cloudformation.resources.AWSEC2Instance;
import com.eucalyptus.cloudformation.resources.Resource;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 12/19/13.
 */
public class StackDeletor extends Thread {
  private static final Logger LOG = Logger.getLogger(StackDeletor.class);
  private Stack stack;
  private String userId;

  public StackDeletor(Stack stack, String userId) {
    this.stack = stack;
    this.userId = userId;
  }
  @Override
  public void run() {
    try {
      LOG.info("stackName=" + stack.getStackName());
      for (StackResourceEntity stackResourceEntity: StackResourceEntityManager.getStackResources(stack.getStackName())) {
        Resource resource = null;
        LOG.info("resourceType="+stackResourceEntity.getResourceType());
        LOG.info("physicalResourceId="+stackResourceEntity.getPhysicalResourceId());
        if (stackResourceEntity.getResourceType().equals("AWS::EC2::Instance")) {
          LOG.info("It's an instance!");
          AWSEC2Instance awsec2Instance = new AWSEC2Instance();
          awsec2Instance.setOwnerUserId(userId);
          awsec2Instance.setLogicalResourceId(stackResourceEntity.getLogicalResourceId());
          awsec2Instance.setType(stackResourceEntity.getResourceType());
          awsec2Instance.setPhysicalResourceId(stackResourceEntity.getPhysicalResourceId());
          resource = awsec2Instance;
        }
        try {
          resource.delete();
        } catch (Throwable ex) {
          LOG.error(ex, ex);
        }
      }
      StackResourceEntityManager.deleteStackResources(stack.getStackName());
      StackEventEntityManager.deleteStackEvents(stack.getStackName());
      StackEntityManager.deleteStack(stack.getStackName());
    } catch (Throwable ex) {
      LOG.error(ex, ex);
    }
  }
}
