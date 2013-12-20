package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.cloudformation.*;
import com.eucalyptus.cloudformation.Stack;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import javax.persistence.EntityTransaction;
import java.util.*;

/**
 * Created by ethomas on 12/18/13.
 */
public class StackEntityManager {
  static final Logger LOG = Logger.getLogger(StackEntityManager.class);
  // more setters later...
  public static void addStack(Stack stack) throws Exception { // TODO: add template
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(
          Restrictions.or(
            Restrictions.eq( "stackName" , stack.getStackName()),
            Restrictions.eq( "stackId", stack.getStackId())
          )
        );
      List<StackEntity> EntityList = criteria.list();
      if (!EntityList.isEmpty()) {
        throw new Exception("Stack already exists");
      }
      LOG.info("stackName=" + stack.getStackName());
      StackEntity stackEntity = stackToStackEntity(stack);
      LOG.info("stackName=" + stackEntity.getStackName());
      Entities.persist(stackEntity);
      // do something
      db.commit( );
    }
  }

  public static Stack getStack(String stackName) {
    StackEntity stackEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("stackName", stackName));
      List<StackEntity> entityList = criteria.list();
      if (entityList != null && !entityList.isEmpty()) {
        stackEntity = entityList.get(0);
      }
      db.commit( );
    }
    if (stackEntity != null) return stackEntityToStack(stackEntity);
    return null;
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

  public static StackEntity stackToStackEntity(Stack stack) {
    StackEntity stackEntity = new StackEntity();

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
      results = criteria.list();
      db.commit( );
    }
    return results;
  }

  public static void deleteStack(String stackName) {
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq( "stackName" , stackName));
      List<StackEntity> entityList = criteria.list();
      for (StackEntity stackEntity: entityList) {
        Entities.delete(stackEntity);
      }
      db.commit( );
    }
  }

  public static String getStackName(String stackId) {
    Stack stack = getStack(stackId);
    if (stack == null) return null;
    return stack.getStackName();
  }

  public static void updateStatus(String stackName, StackEntity.Status status, String statusReason) {
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq( "stackName" , stackName));
      List<StackEntity> entityList = criteria.list();
      for (StackEntity stackEntity: entityList) {
        stackEntity.setStackStatus(status);
        stackEntity.setStackStatusReason(statusReason);
      }
      db.commit();
    }
  }
}
