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
 * additional fatalrmation or have any questions.
 ************************************************************************/

package com.eucalyptus.cloudformation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.junit.Ignore;

@Ignore("Not a JUnit test")
public class CloudFormationServiceForBindingTest {
  private static int NUM_PARAMS = 2;
  private static final Logger LOG = Logger.getLogger(CloudFormationService.class);

  public CancelUpdateStackResponseType cancelUpdateStack(CancelUpdateStackType request)
      throws CloudFormationException {
    LOG.fatal("request.getStackName()="+request.getStackName());
    CancelUpdateStackResponseType reply = request.getReply();
    return reply;
  }

  public CreateStackResponseType createStack(CreateStackType request)
      throws CloudFormationException {
    for (String capability: request.getCapabilities().getMember()) {
      LOG.fatal("capability="+capability);
    }
    LOG.fatal("request.getDisableRollback()="+request.getDisableRollback());
    LOG.fatal("request.getNotificationARNs()="+request.getNotificationARNs());
    LOG.fatal("request.getOnFailure()="+request.getOnFailure());
    Parameters parameters = request.getParameters();
    for (Parameter parameter: parameters.getMember()) {
      LOG.fatal("request.getParameter().getParameterKey()="+parameter.getParameterKey());
      LOG.fatal("request.getParameter().getParameterValue()="+parameter.getParameterValue());
    }
    LOG.fatal("request.getStackName()="+request.getStackName());
    LOG.fatal("request.getStackPolicyBody()="+request.getStackPolicyBody());
    LOG.fatal("request.getStackPolicyURL()="+request.getStackPolicyURL());
    Tags tags = request.getTags();
    for (Tag tag: tags.getMember()) {
      LOG.fatal("request.getTags().getKey()="+tag.getKey());
      LOG.fatal("request.getTags().getValue()="+tag.getValue());
    }
    LOG.fatal("request.getTemplateBody()="+request.getTemplateBody());
    LOG.fatal("request.getTemplateURL()="+request.getTemplateURL());
    LOG.fatal("request.getTimeoutInMinutes()="+request.getTimeoutInMinutes());
    CreateStackResponseType reply = request.getReply();
    CreateStackResult createStackResult = new CreateStackResult();
    createStackResult.setStackId("stackId");
    reply.setCreateStackResult(createStackResult );
    return reply;
  }

  public DeleteStackResponseType deleteStack(DeleteStackType request)
      throws CloudFormationException {
    LOG.fatal("request.getStackName()="+request.getStackName());
    DeleteStackResponseType reply = request.getReply();
    return reply;
  }

  public DescribeStackEventsResponseType describeStackEvents(DescribeStackEventsType request)
      throws CloudFormationException {
    LOG.fatal("request.getNextToken()="+request.getNextToken());
    LOG.fatal("request.getStackName()="+request.getStackName());
    DescribeStackEventsResponseType reply = request.getReply();
    DescribeStackEventsResult describeStackEventsResult = new DescribeStackEventsResult();
    describeStackEventsResult.setNextToken("nextToken");
    StackEvents stackEvents = new StackEvents();
    ArrayList<StackEvent> stackEventList = new ArrayList<StackEvent>();
    for (int i=1;i<=NUM_PARAMS;i++) {
      StackEvent stackEvent = getStackEvent(i);
      stackEventList.add(stackEvent);
    }
    stackEvents.setMember(stackEventList );
    describeStackEventsResult.setStackEvents(stackEvents );
    reply.setDescribeStackEventsResult(describeStackEventsResult );
    return reply;
  }

  private StackEvent getStackEvent(int i) {
    StackEvent stackEvent = new StackEvent();
    stackEvent.setEventId("eventId"+i);
    stackEvent.setLogicalResourceId("logicalResourceId"+i);
    stackEvent.setPhysicalResourceId("physicalResourceId"+i);
    stackEvent.setResourceProperties("resourceProperties"+i);
    stackEvent.setResourceStatus("resourceStatus"+i);
    stackEvent.setResourceStatusReason("resourceStatusReason"+i);
    stackEvent.setResourceType("resourceType"+i);
    stackEvent.setStackId("stackId"+i);
    stackEvent.setStackName("stackName"+i);
    try {
      stackEvent.setTimestamp(sdf.parse("2013-11-0"+i+" 00:00:00.000"));
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return stackEvent;
  }

  public DescribeStackResourceResponseType describeStackResource(DescribeStackResourceType request)
      throws CloudFormationException {
    LOG.fatal("request.getLogicalResourceId()="+request.getLogicalResourceId());
    LOG.fatal("request.getStackName()="+request.getStackName());
    DescribeStackResourceResponseType reply = request.getReply();
    DescribeStackResourceResult describeStackResourceResult = new DescribeStackResourceResult();
    StackResourceDetail stackResourceDetail = new StackResourceDetail();
    stackResourceDetail.setDescription("desc");
    try {
      stackResourceDetail.setLastUpdatedTimestamp(sdf.parse("2013-11-05 00:00:00.000"));
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    stackResourceDetail.setLogicalResourceId("logicalResourceId");
    stackResourceDetail.setMetadata("metadata");
    stackResourceDetail.setPhysicalResourceId("physicalResourceId");
    stackResourceDetail.setResourceStatus("resourceStatus");
    stackResourceDetail.setResourceStatusReason("resourceStatusReason");
    stackResourceDetail.setResourceType("resourceType");
    stackResourceDetail.setStackId("stackId");
    stackResourceDetail.setStackName("stackName");
    describeStackResourceResult.setStackResourceDetail(stackResourceDetail );
    reply.setDescribeStackResourceResult(describeStackResourceResult );
    return reply;
  }

  public DescribeStackResourcesResponseType describeStackResources(DescribeStackResourcesType request)
      throws CloudFormationException {
    LOG.fatal("request.getLogicalResourceId()="+request.getLogicalResourceId());
    LOG.fatal("request.getPhysicalResourceId()="+request.getPhysicalResourceId());
    LOG.fatal("request.getStackName()="+request.getStackName());
    DescribeStackResourcesResponseType reply = request.getReply();
    DescribeStackResourcesResult describeStackResourcesResult = new DescribeStackResourcesResult();
    StackResources stackResources = new StackResources();
    ArrayList<StackResource> stackResourceList = new ArrayList<StackResource>();
    for (int i=1;i<=NUM_PARAMS;i++) {
      StackResource stackResource = getStackResource(i);
      stackResourceList.add(stackResource);
    }
    stackResources.setMember(stackResourceList );
    describeStackResourcesResult.setStackResources(stackResources );
    reply.setDescribeStackResourcesResult(describeStackResourcesResult );
    return reply;
  }

  private StackResource getStackResource(int i) {
    StackResource stackResource = new StackResource();
    stackResource.setDescription("desc"+i);
    stackResource.setLogicalResourceId("logicalResourceId"+i);
    stackResource.setPhysicalResourceId("physicalResourceId"+i);
    stackResource.setResourceStatus("resourceStatus"+i);
    stackResource.setResourceStatusReason("resourceStatusReason"+i);
    stackResource.setResourceType("resourceType"+i);
    stackResource.setStackId("stackId"+i);
    stackResource.setStackName("stackName"+i);
    try {
      stackResource.setTimestamp(sdf.parse("2013-11-0"+i+" 00:00:00.000"));
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return stackResource;
  }

  public DescribeStacksResponseType describeStacks(DescribeStacksType request)
      throws CloudFormationException {
    LOG.fatal("request.getNextToken()="+request.getNextToken());
    LOG.fatal("request.getStackName()="+request.getStackName());
    DescribeStacksResponseType reply = request.getReply();
    DescribeStacksResult describeStacksResult = new DescribeStacksResult();
    describeStacksResult.setNextToken("nextToken");
    Stacks stacks = new Stacks();
    ArrayList<Stack> stackList = new ArrayList<Stack>();
    for (int i=1;i<=NUM_PARAMS;i++) {
      Stack stack = getStack(i);
      stackList.add(stack);
    }
    stacks.setMember(stackList );
    describeStacksResult.setStacks(stacks );
    reply.setDescribeStacksResult(describeStacksResult );
    return reply;
  }
  private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  private Stack getStack(int i) {
    Stack stack = new Stack();
    ArrayList<String> capabilitiesMember = new ArrayList<String>();
    for (int k=1;k<=i;k++) {
      capabilitiesMember.add("capability-"+k+"-"+i);
    }
    ResourceList capabilities = new ResourceList();
    capabilities.setMember(capabilitiesMember);
    stack.setCapabilities(capabilities);
    try {
      stack.setCreationTime(sdf.parse("2013-11-0"+i+" 00:00:00.000"));
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    stack.setDescription("desc" + i);
    stack.setDisableRollback(i % 2 == 0);
    try {
      stack.setLastUpdatedTime(sdf.parse("2013-12-0"+i+" 00:00:00.000"));
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    ArrayList<String> notificationARNMember = new ArrayList<String>();
    for (int k=1;k<=i;k++) {
      notificationARNMember.add("notificationARN-"+k+"-"+i);
    }
    ResourceList notificationARNs = new ResourceList();
    notificationARNs.setMember(notificationARNMember);
    stack.setNotificationARNs(notificationARNs);
    ArrayList<Output> outputsMember = new ArrayList<Output>();
    for (int k=1;k<=i;k++) {
      Output output = new Output();
      output.setDescription("desc" + k);
      output.setOutputKey("key"+k);
      output.setOutputValue("value"+k);
      outputsMember.add(output);
    }
    Outputs outputs = new Outputs();
    outputs.setMember(outputsMember);
    stack.setOutputs(outputs);
    ArrayList<Parameter> parametersMember = new ArrayList<Parameter>();
    for (int k=1;k<=i;k++) {
      Parameter parameter = new Parameter();
      parameter.setParameterKey("parameterKey"+k);
      parameter.setParameterValue("parameterValue"+k);
      parametersMember.add(parameter);
    }
    Parameters parameters = new Parameters();
    parameters.setMember(parametersMember);
    stack.setParameters(parameters);
    stack.setStackId("stackId"+i);
    stack.setStackName("stackName"+i);
    stack.setStackStatus("stackStatus"+i);
    stack.setStackStatusReason("stackStatusReason"+i);
    ArrayList<Tag> tagsMember = new ArrayList<Tag>();
    for (int k=1;k<=i;k++) {
      Tag tag = new Tag();
      tag.setKey("key"+k);
      tag.setValue("value"+k);
      tagsMember.add(tag);
    }
    Tags tags = new Tags();
    tags.setMember(tagsMember);
    stack.setTags(tags);
    stack.setTimeoutInMinutes(i);
    return stack;
  }

  public EstimateTemplateCostResponseType estimateTemplateCost(EstimateTemplateCostType request)
      throws CloudFormationException {
    LOG.fatal("request.getTemplateBody()="+request.getTemplateBody());
    LOG.fatal("request.getTemplateURL()="+request.getTemplateURL());
    for (Parameter parameter:request.getParameters().getMember()) {
      LOG.fatal("parameter.getParameterKey()="+parameter.getParameterKey());
      LOG.fatal("parameter.getParameterValue()="+parameter.getParameterValue());
    }
    EstimateTemplateCostResponseType reply = request.getReply();
    
    EstimateTemplateCostResult estimateTemplateCostResult = new EstimateTemplateCostResult();
    estimateTemplateCostResult.setUrl("url");
    reply.setEstimateTemplateCostResult(estimateTemplateCostResult );
    return reply;
  }

  public GetStackPolicyResponseType getStackPolicy(GetStackPolicyType request)
      throws CloudFormationException {
    LOG.fatal("request.getStackName()="+request.getStackName());
    GetStackPolicyResponseType reply = request.getReply();
    GetStackPolicyResult getStackPolicyResult = new GetStackPolicyResult();
    getStackPolicyResult.setStackPolicyBody("stackPolicyBody");
    reply.setGetStackPolicyResult(getStackPolicyResult );
    return reply;
  }

  public GetTemplateResponseType getTemplate(GetTemplateType request)
      throws CloudFormationException {
    LOG.fatal("request.getStackName()="+request.getStackName());
    GetTemplateResponseType reply = request.getReply();
    GetTemplateResult getTemplateResult = new GetTemplateResult();
    getTemplateResult.setTemplateBody("templateBody");
    reply.setGetTemplateResult(getTemplateResult );
    return reply;
  }

  public ListStackResourcesResponseType listStackResources(ListStackResourcesType request)
      throws CloudFormationException {
    LOG.fatal("request.getNextToken()="+request.getNextToken());
    LOG.fatal("request.getStackName()="+request.getStackName());
    ListStackResourcesResponseType reply = request.getReply();
    ListStackResourcesResult listStackResourcesResult = new ListStackResourcesResult();
    listStackResourcesResult.setNextToken("nextToken");
    StackResourceSummaries stackResourceSummaries = new StackResourceSummaries();
    ArrayList<StackResourceSummary> stackResourceSummaryList = new ArrayList<StackResourceSummary>();
    for (int i=1;i<=NUM_PARAMS;i++) {
      StackResourceSummary stack = getStackResourceSummary(i);
      stackResourceSummaryList.add(stack);
    }
    stackResourceSummaries.setMember(stackResourceSummaryList );
    listStackResourcesResult.setStackResourceSummaries(stackResourceSummaries);
    reply.setListStackResourcesResult(listStackResourcesResult );
    return reply;
  }
  
  private StackResourceSummary getStackResourceSummary(int i) {
    StackResourceSummary stackResourceSummary = new StackResourceSummary();
    try {
      stackResourceSummary.setLastUpdatedTimestamp(sdf.parse("2013-11-0"+i+" 00:00:00.000"));
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    stackResourceSummary.setLogicalResourceId("logicalResourceId"+i);
    stackResourceSummary.setPhysicalResourceId("physicalResourceId"+i);
    stackResourceSummary.setResourceStatus("resourceStatus"+i);
    stackResourceSummary.setResourceStatusReason("resourceStatusReason"+i);
    stackResourceSummary.setResourceType("resourceType"+i);
    return stackResourceSummary;
  }

  public ListStacksResponseType listStacks(ListStacksType request)
      throws CloudFormationException {
    LOG.fatal("request.getNextToken()="+request.getNextToken());
    for (String stackStatusFilter: request.getStackStatusFilter().getMember()) {
      LOG.fatal("stackStatusFilter="+stackStatusFilter);
    }
    ListStacksResponseType reply = request.getReply();
    ListStacksResult listStacksResult = new ListStacksResult();
    listStacksResult.setNextToken("nextToken");
    StackSummaries stackSummaries = new StackSummaries();
    ArrayList<StackSummary> stackSummaryList = new ArrayList<StackSummary>();
    for (int i=1;i<=NUM_PARAMS;i++) {
      StackSummary stackSummary = getStackSummary(i);
      stackSummaryList.add(stackSummary);
    }
    stackSummaries.setMember(stackSummaryList );
    listStacksResult.setStackSummaries(stackSummaries );
    reply.setListStacksResult(listStacksResult );
    return reply;
  }

  private StackSummary getStackSummary(int i) {
    StackSummary stackSummary = new StackSummary();
    try {
      stackSummary.setCreationTime(sdf.parse("2013-11-0"+i+" 00:00:00.000"));
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      stackSummary.setDeletionTime(sdf.parse("2013-12-1"+i+" 00:00:00.000"));
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      stackSummary.setLastUpdatedTime(sdf.parse("2013-12-0"+i+" 00:00:00.000"));
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    stackSummary.setStackId("stackId"+i);
    stackSummary.setStackName("stackName"+i);
    stackSummary.setStackStatus("stackStatus"+i);
    stackSummary.setStackStatusReason("stackStatusReason"+i);
    stackSummary.setTemplateDescription("templateDescription"+i);
    return stackSummary;
  }

  public SetStackPolicyResponseType setStackPolicy(SetStackPolicyType request)
      throws CloudFormationException {
    LOG.fatal("request.getStackName()="+request.getStackName());
    LOG.fatal("request.getStackPolicyBody()="+request.getStackPolicyBody());
    LOG.fatal("request.getStackPolicyURL()="+request.getStackPolicyURL());
    SetStackPolicyResponseType reply = request.getReply();
    return reply;
  }

  public UpdateStackResponseType updateStack(UpdateStackType request)
      throws CloudFormationException {
    for (String capability: request.getCapabilities().getMember()) {
      LOG.fatal("capability="+capability);
    }
    Parameters parameters = request.getParameters();
    for (Parameter parameter: parameters.getMember()) {
      LOG.fatal("request.getParameter().getParameterKey()="+parameter.getParameterKey());
      LOG.fatal("request.getParameter().getParameterValue()="+parameter.getParameterValue());
    }
    LOG.fatal("request.getStackName()="+request.getStackName());
    LOG.fatal("request.getStackPolicyBody()="+request.getStackPolicyBody());
    LOG.fatal("request.getStackPolicyDuringUpdateBody()="+request.getStackPolicyDuringUpdateBody());
    LOG.fatal("request.getStackPolicyDuringUpdateURL()="+request.getStackPolicyDuringUpdateURL());
    LOG.fatal("request.getStackPolicyURL()="+request.getStackPolicyURL());
    LOG.fatal("request.getTemplateBody()="+request.getTemplateBody());
    LOG.fatal("request.getTemplateURL()="+request.getTemplateURL());
    UpdateStackResponseType reply = request.getReply();
    UpdateStackResult updateStackResult = new UpdateStackResult();
    updateStackResult.setStackId("stackId");
    reply.setUpdateStackResult(updateStackResult );
    return reply;
  }

  public ValidateTemplateResponseType validateTemplate(ValidateTemplateType request)
      throws CloudFormationException {
    LOG.fatal("request.getTemplateBody()="+request.getTemplateBody());
    LOG.fatal("request.getTemplateURL()="+request.getTemplateURL());
    ValidateTemplateResponseType reply = request.getReply();
    ValidateTemplateResult validateTemplateResult = new ValidateTemplateResult();
    ArrayList<String> capabilitiesMember = new ArrayList<String>();
    for (int k=1;k<=NUM_PARAMS;k++) {
      capabilitiesMember.add("capability-"+k);
    }
    ResourceList capabilities = new ResourceList();
    capabilities.setMember(capabilitiesMember);
    validateTemplateResult.setCapabilities(capabilities);
    validateTemplateResult.setCapabilitiesReason("capabilitiesReason");
    validateTemplateResult.setDescription("description");
    ArrayList<TemplateParameter> templateParametersMember = new ArrayList<TemplateParameter>();
    for (int k=1;k<=NUM_PARAMS;k++) {
      TemplateParameter templateParameter = new TemplateParameter();
      templateParameter.setDefaultValue("defaultValue"+k);
      templateParameter.setDescription("desc"+k);
      templateParameter.setNoEcho(k % 2 == 0);
      templateParameter.setParameterKey("parameterKey"+k);
      templateParametersMember.add(templateParameter);
    }
    TemplateParameters parameters = new TemplateParameters();
    parameters.setMember(templateParametersMember);
    validateTemplateResult.setParameters(parameters);
    reply.setValidateTemplateResult(validateTemplateResult );
    return reply;
  }
}
