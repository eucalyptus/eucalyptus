package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.cloudformation.model.ResourceStatus
import com.eucalyptus.cloudformation.Output
import com.eucalyptus.cloudformation.Parameter
import com.eucalyptus.cloudformation.StackEvent
import com.eucalyptus.cloudformation.StackResource
import com.eucalyptus.cloudformation.entity.StackEntity
import com.eucalyptus.cloudformation.entity.StackEntityManager
import com.eucalyptus.cloudformation.entity.StackEventEntityManager
import com.eucalyptus.cloudformation.entity.StackResourceEntity
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager
import com.eucalyptus.cloudformation.entity.TemplateEntityManager
import com.eucalyptus.cloudformation.resources.ResourceAction
import com.eucalyptus.cloudformation.resources.ResourceInfo
import com.eucalyptus.cloudformation.resources.ResourcePropertyResolver
import com.eucalyptus.cloudformation.resources.ResourceResolverManager
import com.eucalyptus.cloudformation.template.FunctionEvaluation
import com.eucalyptus.cloudformation.template.IntrinsicFunctions
import com.eucalyptus.cloudformation.template.JsonHelper
import com.eucalyptus.cloudformation.template.Template
import com.eucalyptus.cloudformation.template.TemplateParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.collect.Lists
import com.netflix.glisten.ActivityOperations
import com.netflix.glisten.impl.swf.SwfActivityOperations
import org.apache.log4j.Logger

/**
 * Created by ethomas on 2/18/14.
 */
public class StackActivityImpl implements StackActivity{
  @Delegate
  ActivityOperations activityOperations = new SwfActivityOperations();

  private static final Logger LOG = Logger.getLogger(StackActivityImpl.class);
  @Override
  public String createGlobalStackEvent(String templateJson, String resourceStatus, String resourceStatusReason) {
    Template template = Template.fromJsonNode(JsonHelper.getJsonNodeFromString(templateJson));
    String stackId = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_STACK_ID)).textValue();
    String stackName = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_STACK_NAME)).textValue();
    String accountId = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_ACCOUNT_ID)).textValue();
    StackEvent stackEvent = new StackEvent();
    stackEvent.setStackId(stackId);
    stackEvent.setStackName(stackName);
    stackEvent.setLogicalResourceId(stackName);
    stackEvent.setPhysicalResourceId(stackId);
    stackEvent.setEventId(UUID.randomUUID().toString()); //TODO: AWS has a value related to stack id. (I think)
    ObjectNode properties = new ObjectMapper().createObjectNode();
    for (Parameter parameter: template.getNoEchoFilteredParameterList()) {
      properties.put(parameter.getParameterKey(), parameter.getParameterValue());
    }
    stackEvent.setResourceProperties(JsonHelper.getStringFromJsonNode(properties));
    stackEvent.setResourceType("AWS::CloudFormation::Stack");
    stackEvent.setResourceStatus(resourceStatus);
    stackEvent.setResourceStatusReason(resourceStatusReason);
    stackEvent.setTimestamp(new Date());
    StackEventEntityManager.addStackEvent(stackEvent, accountId);
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public String createResource(String resourceId, String templateJson, String resourceMapJson) {
    ObjectNode returnNode = new ObjectMapper().createObjectNode();
    returnNode.put("resourceId", resourceId);
    Template template = Template.fromJsonNode(JsonHelper.getJsonNodeFromString(templateJson));
    String stackId = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_STACK_ID)).textValue();
    String stackName = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_STACK_NAME)).textValue();
    String accountId = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_ACCOUNT_ID)).textValue();
    Map<String, ResourceInfo> resourceInfoMap = Template.jsonNodeToResourceMap(JsonHelper.getJsonNodeFromString(resourceMapJson));
    for (String resourceName: resourceInfoMap.keySet()) {
      ResourceInfo resourceInfo = resourceInfoMap.get(resourceName);
      template.getResourceMap().put(resourceName, resourceInfo);
    }
    ResourceInfo resourceInfo = template.getResourceMap().get(resourceId);
    if (!resourceInfo.getAllowedByCondition()) {
      returnNode.put("resourceInfo", Template.resourceInfoToJsonNode(resourceInfo));
      return JsonHelper.getStringFromJsonNode(returnNode);
    };
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
    ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
    resourceAction.setResourceInfo(resourceInfo);
    ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
    StackEvent stackEvent = new StackEvent();
    stackEvent.setStackId(stackId);
    stackEvent.setStackName(stackName);
    stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
    stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
    stackEvent.setEventId(resourceInfo.getPhysicalResourceId() + "-" + StackResourceEntity.Status.CREATE_IN_PROGRESS.toString() + "-" + System.currentTimeMillis()); //TODO: see if this really matches
    stackEvent.setResourceProperties(resourceInfo.getPropertiesJson());
    stackEvent.setResourceType(resourceInfo.getType());
    stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_IN_PROGRESS.toString());
    stackEvent.setResourceStatusReason(null);
    stackEvent.setTimestamp(new Date());
    StackEventEntityManager.addStackEvent(stackEvent, accountId);
    StackResource stackResource = new StackResource();
    stackResource.setResourceStatus(StackResourceEntity.Status.CREATE_IN_PROGRESS.toString());
    stackResource.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
    stackResource.setLogicalResourceId(resourceInfo.getLogicalResourceId());
    stackResource.setDescription(""); // deal later
    stackResource.setResourceStatusReason(null);
    stackResource.setResourceType(resourceInfo.getType());
    stackResource.setStackName(stackName);
    stackResource.setStackId(stackId);
    StackResourceEntityManager.addStackResource(stackResource, JsonHelper.getJsonNodeFromString(resourceInfo.getMetadataJson()), accountId);
    try {
      resourceAction.create();
      StackResourceEntityManager.updatePhysicalResourceId(stackId, resourceInfo.getLogicalResourceId(), resourceInfo.getPhysicalResourceId(), accountId);
      StackResourceEntityManager.updateStatus(stackId, resourceInfo.getLogicalResourceId(), StackResourceEntity.Status.CREATE_COMPLETE, "", accountId);
      stackEvent.setEventId(resourceInfo.getPhysicalResourceId() + "-" + StackResourceEntity.Status.CREATE_COMPLETE.toString() + "-" + System.currentTimeMillis()); //TODO: see if this really matches
      stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_COMPLETE.toString());
      stackEvent.setResourceStatusReason("");
      stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
      stackEvent.setTimestamp(new Date());
      StackEventEntityManager.addStackEvent(stackEvent, accountId);
      returnNode.put("resourceInfo", Template.resourceInfoToJsonNode(resourceAction.getResourceInfo()));
      return JsonHelper.getStringFromJsonNode(returnNode);
    } catch (Exception ex) {
      LOG.error(ex, ex);
      StackResourceEntityManager.updateStatus(stackId, resourceInfo.getLogicalResourceId(), StackResourceEntity.Status.CREATE_FAILED, ""+ex.getMessage(), accountId);
      stackEvent.setEventId(resourceInfo.getPhysicalResourceId() + "-" + StackResourceEntity.Status.CREATE_FAILED.toString() + "-" + System.currentTimeMillis()); //TODO: see if this really matches
      stackEvent.setResourceStatus(StackResourceEntity.Status.CREATE_FAILED.toString());
      stackEvent.setTimestamp(new Date());
      stackEvent.setResourceStatusReason(""+ex.getMessage());
      stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
      StackEventEntityManager.addStackEvent(stackEvent, accountId);
      throw ex;
    }
  }

  @Override
  public String deleteResource(String resourceId, String templateJson, String resourceMapJson) {
    ObjectNode returnNode = new ObjectMapper().createObjectNode();
    returnNode.put("resourceId", resourceId);
    Template template = Template.fromJsonNode(JsonHelper.getJsonNodeFromString(templateJson));
    String stackId = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_STACK_ID)).textValue();
    String stackName = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_STACK_NAME)).textValue();
    String accountId = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_ACCOUNT_ID)).textValue();
    Map<String, ResourceInfo> resourceInfoMap = Template.jsonNodeToResourceMap(JsonHelper.getJsonNodeFromString(resourceMapJson));
    for (String resourceName: resourceInfoMap.keySet()) {
      ResourceInfo resourceInfo = resourceInfoMap.get(resourceName);
      template.getResourceMap().put(resourceName, resourceInfo);
    }
    ResourceInfo resourceInfo = template.getResourceMap().get(resourceId);
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(stackId, resourceId, accountId);
    if (stackResourceEntity != null && stackResourceEntity.getResourceStatus() != ResourceStatus.DELETE_COMPLETE) {
      try {
        ResourceAction resourceAction = new ResourceResolverManager().resolveResourceAction(resourceInfo.getType());
        resourceAction.setResourceInfo(resourceInfo);
        ResourcePropertyResolver.populateResourceProperties(resourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(resourceInfo.getPropertiesJson()));
        if (resourceInfo.getDeletionPolicy() == "Retain") {
          StackEvent stackEvent = new StackEvent();
          stackEvent.setStackId(stackId);
          stackEvent.setStackName(stackName);
          stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
          stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
          stackEvent.setEventId(resourceInfo.getPhysicalResourceId() + "-" + StackResourceEntity.Status.DELETE_SKIPPED.toString() + "-" + System.currentTimeMillis()); //TODO: see if this really matches
          stackEvent.setResourceProperties(resourceInfo.getPropertiesJson());
          stackEvent.setResourceType(resourceInfo.getType());
          stackEvent.setResourceStatus(StackResourceEntity.Status.DELETE_SKIPPED.toString());
          stackEvent.setResourceStatusReason(null);
          stackEvent.setTimestamp(new Date());
          StackEventEntityManager.addStackEvent(stackEvent, accountId);
          StackResourceEntityManager.updateStatus(stackId, resourceInfo.getLogicalResourceId(), StackResourceEntity.Status.DELETE_SKIPPED, null, accountId);
          returnNode.put("status", "failure");
        } else {
          StackEvent stackEvent = new StackEvent();
          stackEvent.setStackId(stackId);
          stackEvent.setStackName(stackName);
          stackEvent.setLogicalResourceId(resourceInfo.getLogicalResourceId());
          stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
          stackEvent.setEventId(resourceInfo.getPhysicalResourceId() + "-" + StackResourceEntity.Status.DELETE_IN_PROGRESS.toString() + "-" + System.currentTimeMillis()); //TODO: see if this really matches
          stackEvent.setResourceProperties(resourceInfo.getPropertiesJson());
          stackEvent.setResourceType(resourceInfo.getType());
          stackEvent.setResourceStatus(StackResourceEntity.Status.DELETE_IN_PROGRESS.toString());
          stackEvent.setResourceStatusReason(null);
          stackEvent.setTimestamp(new Date());
          StackEventEntityManager.addStackEvent(stackEvent, accountId);
          StackResourceEntityManager.updateStatus(stackId, resourceInfo.getLogicalResourceId(), StackResourceEntity.Status.DELETE_IN_PROGRESS, null, accountId);
          resourceAction.delete();
          StackResourceEntityManager.updateStatus(stackId, resourceInfo.getLogicalResourceId(), StackResourceEntity.Status.DELETE_COMPLETE, null, accountId);
          stackEvent.setEventId(resourceInfo.getPhysicalResourceId() + "-" + StackResourceEntity.Status.DELETE_COMPLETE.toString() + "-" + System.currentTimeMillis()); //TODO: see if this really matches
          stackEvent.setResourceStatus(StackResourceEntity.Status.DELETE_COMPLETE.toString());
          stackEvent.setResourceStatusReason(null);
          stackEvent.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
          stackEvent.setTimestamp(new Date());
          StackEventEntityManager.addStackEvent(stackEvent, accountId);
          returnNode.put("status", "success");
        }
      } catch (Exception ex) {
        LOG.error(ex, ex);
        returnNode.put("status", "failure");
      }
    } else {
      returnNode.put("status", "success"); // already deleted
    }
    return JsonHelper.getStringFromJsonNode(returnNode);
  }

  @Override
  public String updateTemplate(String templateJson, String resourceMapJson) {
    Template template = Template.fromJsonNode(JsonHelper.getJsonNodeFromString(templateJson));
    String stackId = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_STACK_ID)).textValue();
    String stackName = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_STACK_NAME)).textValue();
    String accountId = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_ACCOUNT_ID)).textValue();
    Map<String, ResourceInfo> resourceInfoMap = Template.jsonNodeToResourceMap(JsonHelper.getJsonNodeFromString(resourceMapJson));
    for (String resourceName: resourceInfoMap.keySet()) {
      ResourceInfo resourceInfo = resourceInfoMap.get(resourceName);
      template.getResourceMap().put(resourceName, resourceInfo);
    }
    TemplateEntityManager.addOrUpdateTemplateJson(stackId, accountId, JsonHelper.getStringFromJsonNode(template.toJsonNode()));
    return "";
  }

  @Override
  public String finalizeCreateStack(String templateJson, String resourceInfoMapJson) {
    Template template = Template.fromJsonNode(JsonHelper.getJsonNodeFromString(templateJson));
    Map<String, ResourceInfo> resourceInfoMap = Template.jsonNodeToResourceMap(JsonHelper.getJsonNodeFromString(resourceInfoMapJson));
    template.setResourceMap(resourceInfoMap);
    String stackId = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_STACK_ID)).textValue();
    String stackName = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_STACK_NAME)).textValue();
    String accountId = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_ACCOUNT_ID)).textValue();
    // Now add outputs...
    List<Output> outputs = Lists.newArrayList();
    for (String outputKey: template.getOutputJsonMap().keySet()) {
      JsonNode outputNode = JsonHelper.getJsonNodeFromString(template.getOutputJsonMap().get(outputKey));
      // Outputs already checked for type in parse template
      if (outputNode.has("Condition") && template.getConditionMap().get(outputNode.get("Condition").textValue()) != Boolean.TRUE) {
        continue; // TODO: log skipping condition?
      }
      Output output = new Output();
      output.setOutputKey(outputKey);
      String description = JsonHelper.getString(outputNode, "Description");
      if (description != null) {
        output.setDescription(description);
      }
      output.setOutputValue(FunctionEvaluation.evaluateFunctions(outputNode.get("Value"), template).textValue());
      outputs.add(output);
    }
    StackEntityManager.addOutputsToStack(stackName, accountId, outputs);
    // now done...update template, update status, and add new stack event for "done"
    TemplateEntityManager.addOrUpdateTemplateJson(stackId, accountId, JsonHelper.getStringFromJsonNode(template.toJsonNode()));
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public String logException(Throwable t) {
    LOG.error(t, t);
    return ""; // promiseFor() doesn't work on void return types
  }

  @Override
  public String deleteAllStackRecords(String templateJson) {
    Template template = Template.fromJsonNode(JsonHelper.getJsonNodeFromString(templateJson));
    String stackId = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_STACK_ID)).textValue();
    String stackName = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_STACK_NAME)).textValue();
    String accountId = JsonHelper.getJsonNodeFromString(template.getPseudoParameterMap().get(TemplateParser.AWS_ACCOUNT_ID)).textValue();
    StackResourceEntityManager.deleteStackResources(stackId, accountId);
    StackEventEntityManager.deleteStackEvents(stackId, accountId);
    StackEntityManager.deleteStack(stackId, accountId);
    TemplateEntityManager.deleteTemplateJson(stackId, accountId);
    return ""; // promiseFor() doesn't work on void return types
  }
}

