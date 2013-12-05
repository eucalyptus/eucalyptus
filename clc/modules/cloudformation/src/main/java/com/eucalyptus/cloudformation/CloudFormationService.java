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

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableClass;

@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class CloudFormationService {
  private static final Logger LOG = Logger.getLogger(CloudFormationService.class);

  public CancelUpdateStackResponseType cancelUpdateStack(CancelUpdateStackType request)
      throws CloudFormationException {
    CancelUpdateStackResponseType reply = request.getReply();
    return reply;
  }

  public CreateStackResponseType createStack(CreateStackType request)
      throws CloudFormationException {
    CreateStackResponseType reply = request.getReply();
    return reply;
  }

  public DeleteStackResponseType deleteStack(DeleteStackType request)
      throws CloudFormationException {
    DeleteStackResponseType reply = request.getReply();
    return reply;
  }

  public DescribeStackEventsResponseType describeStackEvents(DescribeStackEventsType request)
      throws CloudFormationException {
    DescribeStackEventsResponseType reply = request.getReply();
    return reply;
  }

  public DescribeStackResourceResponseType describeStackResource(DescribeStackResourceType request)
      throws CloudFormationException {
    DescribeStackResourceResponseType reply = request.getReply();
    return reply;
  }

  public DescribeStackResourcesResponseType describeStackResources(DescribeStackResourcesType request)
      throws CloudFormationException {
    DescribeStackResourcesResponseType reply = request.getReply();
    return reply;
  }

  public DescribeStacksResponseType describeStacks(DescribeStacksType request)
      throws CloudFormationException {
    DescribeStacksResponseType reply = request.getReply();
    return reply;
  }

  public EstimateTemplateCostResponseType estimateTemplateCost(EstimateTemplateCostType request)
      throws CloudFormationException {
    EstimateTemplateCostResponseType reply = request.getReply();
    return reply;
  }

  public GetStackPolicyResponseType getStackPolicy(GetStackPolicyType request)
      throws CloudFormationException {
    GetStackPolicyResponseType reply = request.getReply();
    return reply;
  }

  public GetTemplateResponseType getTemplate(GetTemplateType request)
      throws CloudFormationException {
    GetTemplateResponseType reply = request.getReply();
    return reply;
  }

  public ListStackResourcesResponseType listStackResources(ListStackResourcesType request)
      throws CloudFormationException {
    ListStackResourcesResponseType reply = request.getReply();
    return reply;
  }
  
  public ListStacksResponseType listStacks(ListStacksType request)
      throws CloudFormationException {
    ListStacksResponseType reply = request.getReply();
    return reply;
  }

  public SetStackPolicyResponseType setStackPolicy(SetStackPolicyType request)
      throws CloudFormationException {
    SetStackPolicyResponseType reply = request.getReply();
    return reply;
  }

  public UpdateStackResponseType updateStack(UpdateStackType request)
      throws CloudFormationException {
    UpdateStackResponseType reply = request.getReply();
    return reply;
  }

  public ValidateTemplateResponseType validateTemplate(ValidateTemplateType request)
      throws CloudFormationException {
    ValidateTemplateResponseType reply = request.getReply();
    return reply;
  }
}
