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
package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.cloudformation.Output;
import com.eucalyptus.cloudformation.Outputs;
import com.eucalyptus.cloudformation.Parameter;
import com.eucalyptus.cloudformation.Parameters;
import com.eucalyptus.cloudformation.ResourceList;
import com.eucalyptus.cloudformation.Stack;
import com.eucalyptus.cloudformation.Tag;
import com.eucalyptus.cloudformation.Tags;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Created by ethomas on 12/18/13.
 */
public class StackEntityManager {
  static final Logger LOG = Logger.getLogger(StackEntityManager.class);
  // more setters later...
  public static void addStack(Stack stack, String accountId) throws Exception { // TODO: add template
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("stackName", stack.getStackName()))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackEntity> EntityList = criteria.list();
      if (!EntityList.isEmpty()) {
        throw new Exception("Stack already exists");
      }
      StackEntity stackEntity = stackToStackEntity(stack, accountId);
      Entities.persist(stackEntity);
      // do something
      db.commit( );
    }
  }

  public static Stack getStack(String stackName, String accountId) {
    Stack stack = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackName", stackName))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackEntity> entityList = criteria.list();
      if (entityList != null && !entityList.isEmpty()) {
        stack = stackEntityToStack(entityList.get(0));
      }
      db.commit( );
    }
    return stack;
  }

  public static void addOutputsToStack(String stackName, String accountId, Collection<Output> outputs) {
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackName", stackName))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackEntity> entityList = criteria.list();
      if (entityList != null && !entityList.isEmpty()) {
        for (StackEntity stackEntity: entityList) {
          if (stackEntity.getOutputs() == null) {
            stackEntity.setOutputs(new ArrayList<StackEntity.Output>());
          }
          for (Output output: outputs) {
            StackEntity.Output stackEntityOutput = new StackEntity.Output();
            stackEntityOutput.setDescription(output.getDescription());
            stackEntityOutput.setOutputKey(output.getOutputKey());
            stackEntityOutput.setOutputValue(output.getOutputValue());
            stackEntity.getOutputs().add(stackEntityOutput);
          }
        }
      }
      db.commit( );
    }
  }

  public static Stack stackEntityToStack(StackEntity stackEntity) {
    Stack stack = new Stack();

    ArrayList<String> capabilitiesMember = Lists.newArrayList();
    if (stackEntity.getCapabilities() != null) {
      capabilitiesMember.addAll(stackEntity.getCapabilities());
    }
    ResourceList capabilities = new ResourceList();
    capabilities.setMember(capabilitiesMember);
    stack.setCapabilities(capabilities);

    stack.setCreationTime(stackEntity.getCreationTimestamp());
    stack.setDescription(stackEntity.getDescription());
    stack.setDisableRollback(stackEntity.getDisableRollback());
    stack.setLastUpdatedTime(stackEntity.getLastUpdateTimestamp());

    ArrayList<String> notificationARNsMember = Lists.newArrayList();
    if (stackEntity.getCapabilities() != null) {
      notificationARNsMember.addAll(stackEntity.getNotificationARNs());
    }
    ResourceList notificationARNs = new ResourceList();
    notificationARNs.setMember(notificationARNsMember);
    stack.setNotificationARNs(notificationARNs);

    ArrayList<Output> outputsMember = Lists.newArrayList();
    if (stackEntity.getOutputs() != null) {
      for (StackEntity.Output stackEntityOutput : stackEntity.getOutputs()) {
        Output output = new Output();
        output.setDescription(stackEntityOutput.getDescription());
        output.setOutputKey(stackEntityOutput.getOutputKey());
        output.setOutputValue(stackEntityOutput.getOutputValue());
        outputsMember.add(output);
      }
    }
    Outputs outputs = new Outputs();
    outputs.setMember(outputsMember);
    stack.setOutputs(outputs);

    ArrayList<Parameter> parametersMember = Lists.newArrayList();
    if (stackEntity.getParameters() != null) {
      for (StackEntity.Parameter stackEntityParameter : stackEntity.getParameters()) {
        Parameter parameter = new Parameter();
        parameter.setParameterKey(stackEntityParameter.getParameterKey());
        parameter.setParameterValue(stackEntityParameter.getParameterValue());
        parametersMember.add(parameter);
      }
    }
    Parameters parameters = new Parameters();
    parameters.setMember(parametersMember);
    stack.setParameters(parameters);

    stack.setStackId(stackEntity.getStackId());
    stack.setStackName(stackEntity.getStackName());
    stack.setStackStatus(stackEntity.getStackStatus().toString());
    stack.setStackStatusReason(stackEntity.getStackStatusReason());

    ArrayList<Tag> tagsMember = Lists.newArrayList();
    if (stackEntity.getTags() != null) {
      for (StackEntity.Tag stackEntityTag : stackEntity.getTags()) {
        Tag tag = new Tag();
        tag.setKey(stackEntityTag.getKey());
        tag.setValue(stackEntityTag.getValue());
        tagsMember.add(tag);
      }
    }
    Tags tags = new Tags();
    tags.setMember(tagsMember);
    stack.setTags(tags);

    stack.setTimeoutInMinutes(stackEntity.getTimeoutInMinutes());
    return stack;
  }

  public static StackEntity stackToStackEntity(Stack stack, String accountId) {
    StackEntity stackEntity = new StackEntity();
    stackEntity.setRecordDeleted(Boolean.FALSE);
    stackEntity.setAccountId(accountId);
    if (stack.getCapabilities() != null && stack.getCapabilities().getMember() != null) {
      stackEntity.setCapabilities(stack.getCapabilities().getMember());
    }

    stackEntity.setCreationTimestamp(stack.getCreationTime());
    stackEntity.setDescription(stack.getDescription());
    stackEntity.setDisableRollback(stack.getDisableRollback());
    stackEntity.setLastUpdateTimestamp(stack.getLastUpdatedTime());

    if (stack.getNotificationARNs() != null && stack.getNotificationARNs().getMember() != null) {
      stackEntity.setNotificationARNs(stack.getNotificationARNs().getMember());
    }

    if (stack.getOutputs() != null && stack.getOutputs().getMember() != null) {
      ArrayList<StackEntity.Output> outputs = Lists.newArrayList();
      for (Output stackOutput : stack.getOutputs().getMember()) {
        StackEntity.Output output = new StackEntity.Output();
        output.setDescription(stackOutput.getDescription());
        output.setOutputKey(stackOutput.getOutputKey());
        output.setOutputValue(stackOutput.getOutputValue());
        outputs.add(output);
      }
      stackEntity.setOutputs(outputs);
    }

    if (stack.getParameters() != null && stack.getParameters().getMember() != null) {
      ArrayList<StackEntity.Parameter> parameters = Lists.newArrayList();
      for (Parameter stackParameter : stack.getParameters().getMember()) {
        StackEntity.Parameter parameter = new StackEntity.Parameter();
        parameter.setParameterKey(stackParameter.getParameterKey());
        parameter.setParameterValue(stackParameter.getParameterValue());
        parameters.add(parameter);
      }
      stackEntity.setParameters(parameters);
    }

    stackEntity.setStackId(stack.getStackId());
    stackEntity.setStackName(stack.getStackName());
    stackEntity.setStackStatus(StackEntity.Status.valueOf(stack.getStackStatus()));
    stackEntity.setStackStatusReason(stack.getStackStatusReason());

    if (stack.getTags() != null && stack.getTags().getMember() != null) {
      ArrayList<StackEntity.Tag> tags = Lists.newArrayList();
      for (Tag stackTag : stack.getTags().getMember()) {
        StackEntity.Tag tag = new StackEntity.Tag();
        tag.setKey(stackTag.getKey());
        tag.setValue(stackTag.getValue());
        tags.add(tag);
      }
      stackEntity.setTags(tags);
    }
    stackEntity.setTimeoutInMinutes(stack.getTimeoutInMinutes());
    return stackEntity;
  }

  public static List<StackEntity> getAllStacks() {
    List<StackEntity> results = Lists.newArrayList();
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class);
      results = criteria.list(); // TODO: this will cause lazy load issues
      db.commit( );
    }
    return results;
  }

  public static void deleteStack(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq( "stackId" , stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));

      List<StackEntity> entityList = criteria.list();
      for (StackEntity stackEntity: entityList) {
        stackEntity.setRecordDeleted(Boolean.TRUE);
      }
      db.commit( );
    }
  }

  public static String getStackName(String stackId, String accountId) {
    Stack stack = getStack(stackId, accountId);
    if (stack == null) return null;
    return stack.getStackName();
  }

  public static void updateStatus(String stackName, StackEntity.Status status, String statusReason, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq( "stackName" , stackName))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackEntity> entityList = criteria.list();
      for (StackEntity stackEntity: entityList) {
        stackEntity.setStackStatus(status);
        stackEntity.setStackStatusReason(statusReason);
      }
      db.commit();
    }
  }
}
