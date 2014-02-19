/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudformation;

import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.entity.StackEntityManager;
import com.eucalyptus.cloudformation.entity.StackEventEntityManager;
import com.eucalyptus.cloudformation.entity.StackResourceEntity;
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourcePropertyResolver;
import com.eucalyptus.cloudformation.resources.ResourceResolver;
import com.eucalyptus.cloudformation.resources.ResourceResolverManager;
import com.eucalyptus.cloudformation.template.FunctionEvaluation;
import com.eucalyptus.cloudformation.template.IntrinsicFunctions;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.template.Template;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by ethomas on 12/19/13.
 */
public class StackCreator extends Thread {
  private static final Logger LOG = Logger.getLogger(StackCreator.class);

  private Stack stack;
  private String templateBody;

  private Template template;
  private String accountId;

  public StackCreator(Stack stack, String templateBody, Template template, String accountId) {
    this.stack = stack;
    this.templateBody = templateBody;
    this.template = template;
    this.accountId = accountId;
  }
  @Override
  public void run() {
    try {
      ResourceResolverManager resourceResolverManager = new ResourceResolverManager();
      for (String resourceName: template.getResourceDependencyManager().dependencyList()) {
        ResourceInfo resourceInfo = template.getResourceMap().get(resourceName);
        if (!resourceInfo.getAllowedByCondition()) continue;
        // Finally evaluate all properties
        if (resourceInfo.getPropertiesJson() != null) {
          JsonNode propertiesJsonNode = JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson());
          List<String> propertyKeys = Lists.newArrayList(propertiesJsonNode.fieldNames());
          for (String propertyKey: propertyKeys) {
            JsonNode evaluatedPropertyNode = FunctionEvaluation.evaluateFunctions(propertiesJsonNode.get(propertyKey), template);
            if (IntrinsicFunctions.NO_VALUE.evaluateMatch(evaluatedPropertyNode).isMatch()) {
              ((ObjectNode) propertiesJsonNode).remove(propertyKey);
            } else {
              ((ObjectNode) propertiesJsonNode).put(propertyKey, evaluatedPropertyNode);
            }
          }
          resourceInfo.setPropertiesJson(JsonHelper.getStringFromJsonNode(propertiesJsonNode));
        }
        ResourceAction resourceAction = resourceResolverManager.resolveResourceAction(resourceInfo.getType());
        resourceAction.setResourceInfo(resourceInfo);
        ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
        StackEvent stackEvent = new StackEvent();
        stackEvent.setStackId(stack.getStackId());
        stackEvent.setStackName(stack.getStackName());
        stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
        stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
        stackEvent.setEventId(UUID.randomUUID().toString()); //TODO: get real event id
        stackEvent.setResourceProperties(resourceInfo.getPropertiesJson());
        stackEvent.setResourceType(resourceInfo.getType());
        stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_IN_PROGRESS.toString());
        stackEvent.setResourceStatusReason("Part of stack");
        stackEvent.setTimestamp(new Date());
        StackEventEntityManager.addStackEvent(stackEvent, accountId);
        StackResource stackResource = new StackResource();
        stackResource.setResourceStatus(StackResourceEntity.Status.CREATE_IN_PROGRESS.toString());
        stackResource.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
        stackResource.setLogicalResourceId(resourceInfo.getLogicalResourceId());
        stackResource.setDescription(""); // deal later
        stackResource.setResourceStatusReason("Part of stack");
        stackResource.setResourceType(resourceInfo.getType());
        stackResource.setStackName(stack.getStackName());
        stackResource.setStackId(stack.getStackId());
        StackResourceEntityManager.addStackResource(stackResource, JsonHelper.getJsonNodeFromString(resourceInfo.getMetadataJson()), accountId);
        try {
          resourceAction.create();
          StackResourceEntityManager.updatePhysicalResourceId(stack.getStackId(), resourceInfo.getLogicalResourceId(), resourceInfo.getPhysicalResourceId(), accountId);
          StackResourceEntityManager.updateStatus(stack.getStackId(), resourceInfo.getLogicalResourceId(), StackResourceEntity.Status.CREATE_COMPLETE, "Complete!", accountId);
          stackEvent.setEventId(UUID.randomUUID().toString()); //TODO: get real event id
          stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_COMPLETE.toString());
          stackEvent.setResourceStatusReason("Complete!");
          stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
          stackEvent.setTimestamp(new Date());
          StackEventEntityManager.addStackEvent(stackEvent, accountId);
          template.getReferenceMap().get(resourceInfo.getLogicalResourceId()).setReady(true);
          template.getReferenceMap().get(resourceInfo.getLogicalResourceId()).setReferenceValueJson(resourceInfo.getReferenceValueJson());
        } catch (Exception ex) {
          LOG.error(ex, ex);
          StackResourceEntityManager.updateStatus(stack.getStackId(), resourceInfo.getLogicalResourceId(), StackResourceEntity.Status.CREATE_FAILED, ""+ex.getMessage(), accountId);
          stackEvent.setEventId(UUID.randomUUID().toString()); //TODO: get real event id
          stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_FAILED.toString());
          stackEvent.setTimestamp(new Date());
          stackEvent.setResourceStatusReason(""+ex.getMessage());
          stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
          StackEventEntityManager.addStackEvent(stackEvent, accountId);
          throw ex;
        }
      }
      StackEntityManager.updateStatus(stack.getStackId(), StackEntity.Status.CREATE_COMPLETE, "Complete!", accountId);

    } catch (Exception ex2) {
      LOG.error(ex2, ex2);
      StackEntityManager.updateStatus(stack.getStackId(), StackEntity.Status.CREATE_FAILED, ex2.getMessage(), accountId);
    }
  }
}
