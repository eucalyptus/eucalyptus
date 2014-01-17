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

import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.entity.StackEntityManager;
import com.eucalyptus.cloudformation.resources.Resource;
import com.eucalyptus.cloudformation.template.Template;
import com.eucalyptus.cloudformation.template.TemplateParser;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    try {
      final Context ctx = Contexts.lookup();
      User user = ctx.getUser();
      String userId = user.getUserId();
      String accountId = user.getAccount().getAccountNumber();

      String stackName = request.getStackName();
      String templateBody = request.getTemplateBody();
      if (stackName == null) throw new ValidationErrorException("Stack name is null");
      if (templateBody == null) throw new ValidationErrorException("template body is null");
      List<Parameter> parameters = null;
      if (request.getParameters() != null && request.getParameters().getMember() != null) {
        parameters = request.getParameters().getMember();
      }
      Template template = new TemplateParser().parse(templateBody, parameters);
      for (Resource resource: template.getResourceList()) {
        resource.setEffectiveUserId(userId);
      }
      // create the stack here to make sure not duplicated...
      Stack stack = new Stack();
      stack.setStackName(stackName);
      stack.setStackId(UUID.randomUUID().toString());
      stack.setDescription(template.getDescription());
      ArrayList<Parameter> templateParameters = Lists.newArrayList();
      for (Template.Parameter templateParameter: template.getParameterList()) {
        Parameter parameter = new Parameter();
        parameter.setParameterValue(templateParameter.getParameterValue());
        parameter.setParameterKey(templateParameter.getParameterKey());
      }
      Parameters stackParameters = new Parameters();
      stackParameters.setMember(templateParameters);
      stack.setParameters(stackParameters);
      stack.setStackStatus(StackEntity.Status.CREATE_IN_PROGRESS.toString());
      stack.setStackStatusReason("User initiated");
      stack.setDisableRollback(true);
      StackEntityManager.addStack(stack, accountId);
      new StackCreator(stack, templateBody, template, accountId).start();
      CreateStackResult createStackResult = new CreateStackResult();
      createStackResult.setStackId(stack.getStackId());
      reply.setCreateStackResult(createStackResult);
    } catch (Exception ex) {
      LOG.error(ex, ex);
      throw new ValidationErrorException(ex.getMessage());
    }
    return reply;
  }

  public DeleteStackResponseType deleteStack(DeleteStackType request)
      throws CloudFormationException {
    DeleteStackResponseType reply = request.getReply();
    try {
      final Context ctx = Contexts.lookup();
      User user = ctx.getUser();
      String userId = user.getUserId();
      String accountId = user.getAccount().getAccountNumber();
      String stackName = request.getStackName();
      if (stackName == null) throw new ValidationErrorException("Stack name is null");
      Stack stack = StackEntityManager.getStack(stackName, accountId);
      if (stack == null) throw new ValidationErrorException("Stack " + stackName + " does not exist");
      new StackDeletor(stack, userId, accountId).start();
    } catch (Exception ex) {
      LOG.error(ex, ex);
    }
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
