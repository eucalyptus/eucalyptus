package com.eucalyptus.cloudformation;

import com.eucalyptus.cloudformation.entity.*;
import com.eucalyptus.cloudformation.resources.Resource;
import com.eucalyptus.cloudformation.template.Template;
import com.eucalyptus.cloudformation.template.TemplateParser;

import java.util.UUID;

/**
 * Created by ethomas on 12/19/13.
 */
public class StackCreator extends Thread {

  private Stack stack;
  private String templateBody;
  private Template template;

  public StackCreator(Stack stack, String templateBody, Template template) {
    this.stack = stack;
    this.templateBody = templateBody;
    this.template = template;
  }
  @Override
  public void run() {
    try {
    for (Resource resource:template.getResourceList()) {
        new TemplateParser().reevaluateResources(template);
        StackEvent stackEvent = new StackEvent();
        stackEvent.setStackId(stack.getStackId());
        stackEvent.setStackName(stack.getStackName());
        stackEvent.setLogicalResourceId(resource.getLogicalResourceId());
        stackEvent.setPhysicalResourceId(resource.getPhysicalResourceId());
        stackEvent.setEventId(UUID.randomUUID().toString()); //TODO: get real event id
        stackEvent.setResourceProperties(resource.getPropertiesJSON().toString());
        stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_IN_PROGRESS.toString());
        stackEvent.setResourceStatusReason("Part of stack");
        StackEventEntityManager.addStackEvent(stackEvent);
        StackResource stackResource = new StackResource();
        stackResource.setResourceStatus(StackResourceEntity.Status.CREATE_IN_PROGRESS.toString());
        stackResource.setPhysicalResourceId(resource.getPhysicalResourceId());
        stackResource.setLogicalResourceId(resource.getLogicalResourceId());
        stackResource.setDescription(""); // deal later
        stackResource.setResourceStatusReason("Part of stack");
        stackResource.setResourceType(resource.getType());
        stackResource.setStackName(stack.getStackName());
        stackResource.setStackId(stack.getStackId());
        StackResourceEntityManager.addStackResource(stackResource, resource.getMetadataJSON());
        try {
          resource.create();
          StackResourceEntityManager.updatePhysicalResourceId(stack.getStackName(), resource.getLogicalResourceId(), resource.getPhysicalResourceId());
          StackResourceEntityManager.updateStatus(stack.getStackName(), resource.getLogicalResourceId(), StackResourceEntity.Status.CREATE_COMPLETE, "Complete!");
          stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_COMPLETE.toString());
          stackEvent.setResourceStatusReason("Complete!");
          stackEvent.setPhysicalResourceId(resource.getPhysicalResourceId());
          StackEventEntityManager.addStackEvent(stackEvent);
          template.getReferenceMap().get(resource.getPhysicalResourceId()).setReady(true);
          template.getReferenceMap().get(resource.getPhysicalResourceId()).setReferenceValue(resource.referenceValue());
        } catch (Exception ex) {
          StackResourceEntityManager.updateStatus(stack.getStackName(), resource.getLogicalResourceId(), StackResourceEntity.Status.CREATE_FAILED, ""+ex.getMessage());
          stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_FAILED.toString());
          stackEvent.setResourceStatusReason(""+ex.getMessage());
          stackEvent.setPhysicalResourceId(resource.getPhysicalResourceId());
          StackEventEntityManager.addStackEvent(stackEvent);
          throw ex;
        }
      }
      StackEntityManager.updateStatus(stack.getStackName(), StackEntity.Status.CREATE_COMPLETE, "Complete!");
    } catch (Exception ex2) {
      StackEntityManager.updateStatus(stack.getStackName(), StackEntity.Status.CREATE_FAILED, ex2.getMessage());
    }
  }
}
