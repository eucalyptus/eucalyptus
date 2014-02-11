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
import com.eucalyptus.cloudformation.resources.Resource;
import com.eucalyptus.cloudformation.template.FunctionEvaluation;
import com.eucalyptus.cloudformation.template.IntrinsicFunctions;
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
      for (String resourceName: template.getResourceDependencyManager().dependencyList()) {
        Resource resource = template.getResourceMap().get(resourceName);
        if (!resource.isAllowedByCondition()) continue;
        // Finally evaluate all properties
        if (resource.getPropertiesJsonNode() != null) {
          List<String> propertyKeys = Lists.newArrayList(resource.getPropertiesJsonNode().fieldNames());
          for (String propertyKey: propertyKeys) {
            JsonNode evaluatedPropertyNode = FunctionEvaluation.evaluateFunctions(resource.getPropertiesJsonNode().get(propertyKey), template);
            if (IntrinsicFunctions.NO_VALUE.evaluateMatch(evaluatedPropertyNode).isMatch()) {
              ((ObjectNode) resource.getPropertiesJsonNode()).remove(propertyKey);
            } else {
              ((ObjectNode) resource.getPropertiesJsonNode()).put(propertyKey, evaluatedPropertyNode);
            }
          }
        }
        resource.populateResourceProperties(resource.getPropertiesJsonNode());
        StackEvent stackEvent = new StackEvent();
        stackEvent.setStackId(stack.getStackId());
        stackEvent.setStackName(stack.getStackName());
        stackEvent.setLogicalResourceId(resource.getLogicalResourceId());
        stackEvent.setPhysicalResourceId(resource.getPhysicalResourceId());
        stackEvent.setEventId(UUID.randomUUID().toString()); //TODO: get real event id
        stackEvent.setResourceProperties(resource.getPropertiesJsonNode().toString());
        stackEvent.setResourceType(resource.getType());
        stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_IN_PROGRESS.toString());
        stackEvent.setResourceStatusReason("Part of stack");
        stackEvent.setTimestamp(new Date());
        StackEventEntityManager.addStackEvent(stackEvent, accountId);
        StackResource stackResource = new StackResource();
        stackResource.setResourceStatus(StackResourceEntity.Status.CREATE_IN_PROGRESS.toString());
        stackResource.setPhysicalResourceId(resource.getPhysicalResourceId());
        stackResource.setLogicalResourceId(resource.getLogicalResourceId());
        stackResource.setDescription(""); // deal later
        stackResource.setResourceStatusReason("Part of stack");
        stackResource.setResourceType(resource.getType());
        stackResource.setStackName(stack.getStackName());
        stackResource.setStackId(stack.getStackId());
        StackResourceEntityManager.addStackResource(stackResource, resource.getMetadataJsonNode(), accountId);
        try {
          resource.create();
          StackResourceEntityManager.updatePhysicalResourceId(stack.getStackName(), resource.getLogicalResourceId(), resource.getPhysicalResourceId(), accountId);
          StackResourceEntityManager.updateStatus(stack.getStackName(), resource.getLogicalResourceId(), StackResourceEntity.Status.CREATE_COMPLETE, "Complete!", accountId);
          stackEvent.setEventId(UUID.randomUUID().toString()); //TODO: get real event id
          stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_COMPLETE.toString());
          stackEvent.setResourceStatusReason("Complete!");
          stackEvent.setPhysicalResourceId(resource.getPhysicalResourceId());
          stackEvent.setTimestamp(new Date());
          StackEventEntityManager.addStackEvent(stackEvent, accountId);
          template.getReferenceMap().get(resource.getLogicalResourceId()).setReady(true);
          template.getReferenceMap().get(resource.getLogicalResourceId()).setReferenceValue(resource.referenceValue());
        } catch (Exception ex) {
          LOG.error(ex, ex);
          StackResourceEntityManager.updateStatus(stack.getStackName(), resource.getLogicalResourceId(), StackResourceEntity.Status.CREATE_FAILED, ""+ex.getMessage(), accountId);
          stackEvent.setEventId(UUID.randomUUID().toString()); //TODO: get real event id
          stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_FAILED.toString());
          stackEvent.setTimestamp(new Date());
          stackEvent.setResourceStatusReason(""+ex.getMessage());
          stackEvent.setPhysicalResourceId(resource.getPhysicalResourceId());
          StackEventEntityManager.addStackEvent(stackEvent, accountId);
          throw ex;
        }
      }
      StackEntityManager.updateStatus(stack.getStackName(), StackEntity.Status.CREATE_COMPLETE, "Complete!", accountId);
      for (String conditionName: template.getConditionMap().keySet()) {
        LOG.info("Condition: " + conditionName + "=" + FunctionEvaluation.evaluateBoolean(template.getConditionMap().get(conditionName)));
      }
      for (String outputName: template.getOutputJsonNodeMap().keySet()) {
        LOG.info("Output: " + outputName + "=" + FunctionEvaluation.evaluateFunctions(template.getOutputJsonNodeMap().get(outputName), template));
      }

    } catch (Exception ex2) {
      LOG.error(ex2, ex2);
      StackEntityManager.updateStatus(stack.getStackName(), StackEntity.Status.CREATE_FAILED, ex2.getMessage(), accountId);
    }
  }
}
