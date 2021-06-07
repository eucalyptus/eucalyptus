/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.cloudformation.AlreadyExistsException;
import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.InternalFailureException;
import com.eucalyptus.cloudformation.common.msgs.UpdateStackType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.template.Template;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.util.Date;
import java.util.List;


/**
 * Created by ethomas on 12/18/13.
 */
public class StackEntityManager {

  private static final Class STACK_ENTITY_TRANSACTION_CLASS = StackEntity.class; // We use one object for transactions on both StackEntity and PastStackEntity
  private static final List<Class<? extends VersionedStackEntity>> ALL_STACK_ENTITY_CLASSES = Lists.newArrayList(StackEntity.class, PastStackEntity.class);

  public synchronized static void rollbackUpdateStack(String stackId, String accountId, int rolledBackStackVersion) throws CloudFormationException {
    StackUpdateInfoEntity stackUpdateInfoEntity = StackUpdateInfoEntityManager.getStackUpdateInfoEntity(stackId, accountId);
    if (stackUpdateInfoEntity == null) {
      throw new ValidationErrorException("No stack update info entity found");
    }
    if (!stackUpdateInfoEntity.getHasCalledRollbackStackState()) {
      int updatedStackVersion = rolledBackStackVersion - 1; // version passed in is 1 more than the current updated stack version
      try (TransactionResource db =
             Entities.transactionFor(STACK_ENTITY_TRANSACTION_CLASS)) {
        // Get current stack
        Criteria criteria = Entities.createCriteria(StackEntity.class)
          .add(Restrictions.eq("stackId", stackId))
          .add(Restrictions.eq("accountId", accountId))
          .add(Restrictions.eq("stackVersion", updatedStackVersion))
          .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
        List<StackEntity> entityList = criteria.list();
        if (entityList == null || entityList.isEmpty()) {
          throw new ValidationErrorException("Stack does not exist");
        } else if (entityList.size() > 1) {
          throw new InternalFailureException("More than one stack  exists with this id " + stackId + " and version " + updatedStackVersion);
        }
        StackEntity currentStackEntity = entityList.get(0);
        // Get previous stack
        Criteria pastCriteria = Entities.createCriteria(PastStackEntity.class)
          .add(Restrictions.eq("stackId", stackId))
          .add(Restrictions.eq("accountId", accountId))
          .add(Restrictions.eq("stackVersion", updatedStackVersion - 1))
          .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
        List<PastStackEntity> pastEntityList = pastCriteria.list();
        if (pastEntityList == null || pastEntityList.isEmpty()) {
          throw new ValidationErrorException("Previous version of stack does not exist with version " + (updatedStackVersion - 1));
        } else if (entityList.size() > 1) {
          throw new InternalFailureException("More than one stack  exists with this id " + stackId + " and version " + (updatedStackVersion - 1));
        }
        PastStackEntity pastStackEntity = pastEntityList.get(0);
        // Basically swap the two entries.
        swapStackEntityFields(pastStackEntity, currentStackEntity);
        // set the versions accordingly
        currentStackEntity.setStackVersion(rolledBackStackVersion);
        pastStackEntity.setStackVersion(updatedStackVersion);
        // the past stack entry now has the current status, so update that at least
        currentStackEntity.setStackStatus(pastStackEntity.getStackStatus());
        currentStackEntity.setStackStatusReason(pastStackEntity.getStackStatusReason());
        db.commit();
        StackUpdateInfoEntityManager.setHasCalledRollbackStackState(stackId, accountId);
      }
    }
  }

  public static void swapStackEntityFields(VersionedStackEntity s1, VersionedStackEntity s2) {
    VersionedStackEntity temp = new StackEntity(); // could be any subtype, it really doesn't matter.
    copyStackEntityFields(s1, temp);
    copyStackEntityFields(s2, s1);
    copyStackEntityFields(temp, s2);
  }

  public synchronized static StackEntity checkValidUpdateStatusAndUpdateStack(String stackId, String accountId, Template newTemplate, String newTemplateText, UpdateStackType request, int previousStackVersion) throws CloudFormationException {
    try ( TransactionResource db =
            Entities.transactionFor( STACK_ENTITY_TRANSACTION_CLASS ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackEntity> entityList = criteria.list();
      if (entityList == null || entityList.isEmpty()) {
        throw new ValidationErrorException("Stack does not exist");
      } else if (entityList.size() > 1) {
        throw new InternalFailureException("More than one stack exists with this id " + stackId);
      }
      StackEntity currentStackEntity = entityList.get(0);
      if (currentStackEntity.getStackVersion() != previousStackVersion) {
        throw new ValidationErrorException("Stack: " + stackId + " is already being updated");
      }
      // Finally make sure the stack state is ok
      if (currentStackEntity.getStackStatus() != Status.CREATE_COMPLETE && currentStackEntity.getStackStatus() != Status.UPDATE_COMPLETE &&
        currentStackEntity.getStackStatus() != Status.UPDATE_ROLLBACK_COMPLETE) {
        throw new ValidationErrorException("Stack:" + stackId + " is in " + currentStackEntity.getStackStatus().toString() + " state and can not be updated.");
      }

      // put the old stack into the past table
      PastStackEntity previousStackEntity = new PastStackEntity();
      copyStackEntityFields(currentStackEntity, previousStackEntity);
      addStack(previousStackEntity);

      // populate all the stack entity fields with the new info
      StackEntityHelper.populateStackEntityWithTemplate(currentStackEntity, newTemplate);
      currentStackEntity.setTemplateBody(newTemplateText);
      currentStackEntity.setStackStatus(Status.UPDATE_IN_PROGRESS);
      currentStackEntity.setStackStatusReason("User initiated");
      currentStackEntity.setLastUpdateOperationTimestamp(new Date());
      if (request.getCapabilities() != null && request.getCapabilities().getMember() != null) {
        currentStackEntity.setCapabilitiesJson(StackEntityHelper.capabilitiesToJson(request.getCapabilities().getMember()));
      } else {
        currentStackEntity.setCapabilitiesJson(null);
      }
      if (request.getNotificationARNs()!= null && request.getNotificationARNs().getMember() != null) {
        currentStackEntity.setNotificationARNsJson(StackEntityHelper.notificationARNsToJson(request.getNotificationARNs().getMember()));
      } else {
        currentStackEntity.setNotificationARNsJson(null);
      }
      if (request.getTags()!= null && request.getTags().getMember() != null) {
        currentStackEntity.setTagsJson(StackEntityHelper.tagsToJson(request.getTags().getMember()));
      }
      currentStackEntity.setStackVersion(previousStackVersion + 1);
      db.commit( );
      return currentStackEntity;
    }
  }
  static final Logger LOG = Logger.getLogger(StackEntityManager.class);
  // more setters later...

  public static VersionedStackEntity addStack(StackEntity stackEntity) throws AlreadyExistsException {
    try ( TransactionResource db =
            Entities.transactionFor( STACK_ENTITY_TRANSACTION_CLASS ) ) {
      Criteria criteria = Entities.createCriteria(stackEntity.getClass())
        .add(Restrictions.eq("stackName", stackEntity.getStackName()))
        .add(Restrictions.eq("accountId", stackEntity.getAccountId()))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List entityList = criteria.list();
      if (!entityList.isEmpty()) {
        throw new AlreadyExistsException("Stack already exists");
      }
      if (stackEntity.getCreateOperationTimestamp() == null) {
        stackEntity.setCreateOperationTimestamp(new Date());
      }
      Entities.persist(stackEntity);
      // do something
      db.commit( );
    }
    return stackEntity;
  }

  public static VersionedStackEntity addStack(PastStackEntity stackEntity) throws AlreadyExistsException {
    try ( TransactionResource db =
            Entities.transactionFor( STACK_ENTITY_TRANSACTION_CLASS ) ) {
      Criteria criteria = Entities.createCriteria(stackEntity.getClass())
        .add(Restrictions.eq("stackName", stackEntity.getStackName()))
        .add(Restrictions.eq("accountId", stackEntity.getAccountId()))
        .add(Restrictions.eq("stackVersion", stackEntity.getStackVersion()))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List entityList = criteria.list();
      if (!entityList.isEmpty()) {
        throw new AlreadyExistsException("Stack already exists");
      }
      if (stackEntity.getCreateOperationTimestamp() == null) {
        stackEntity.setCreateOperationTimestamp(new Date());
      }
      Entities.persist(stackEntity);
      // do something
      db.commit( );
    }
    return stackEntity;
  }

  public static List<StackEntity> describeStacks(String accountId, String stackNameOrId) {
    final List<StackEntity> returnValue;
    try ( TransactionResource db =
            Entities.transactionFor( STACK_ENTITY_TRANSACTION_CLASS ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add( accountId != null ? Restrictions.eq("accountId", accountId) : Restrictions.conjunction( ) );
      if (stackNameOrId != null) {
        // stack name or id can be stack name on non-deleted stacks or stack id on any stack
        criteria.add(Restrictions.or(
            Restrictions.and(Restrictions.eq("recordDeleted", Boolean.FALSE), Restrictions.eq("stackName", stackNameOrId)),
            Restrictions.eq("stackId", stackNameOrId))
        );
      } else {
        criteria.add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      }
      returnValue = criteria.list();
    }
    return returnValue;
  }

  public static List<StackEntity> listStacks(String accountId, List<Status> statusValues) {
    List<StackEntity> returnValue;
    try ( TransactionResource db =
            Entities.transactionFor( STACK_ENTITY_TRANSACTION_CLASS ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("accountId", accountId));
      if (statusValues != null && !statusValues.isEmpty()) {
        Criterion[] orClauses = new Criterion[statusValues.size()];
        int ctr = 0;
        for (Status statusValue: statusValues) {
          orClauses[ctr++] = Restrictions.eq("stackStatus", statusValue);
        }
        criteria.add(Restrictions.or(orClauses));
      }
      returnValue = criteria.list();
    }
    return returnValue;
  }

  public static VersionedStackEntity getNonDeletedVersionedStackById(String stackId, String accountId, int stackVersion) {
    VersionedStackEntity stackEntity = null;
    for (Class stackEntityClass: ALL_STACK_ENTITY_CLASSES) {
      if (stackEntity == null) {
        try ( TransactionResource db =
                Entities.transactionFor( STACK_ENTITY_TRANSACTION_CLASS ) ) {
          Criteria criteria = Entities.createCriteria(stackEntityClass)
            .add(Restrictions.eq("accountId", accountId))
            .add(Restrictions.eq("stackId", stackId))
            .add(Restrictions.eq("stackVersion", stackVersion))
            .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
          List<VersionedStackEntity> entityList = criteria.list();
          if (entityList != null && !entityList.isEmpty()) {
            stackEntity = entityList.get(0);
          }
        }
      }
    }
    return stackEntity;
  }

  public static StackEntity getNonDeletedStackByNameOrId(String stackNameOrId, String accountId) {
    StackEntity stackEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( STACK_ENTITY_TRANSACTION_CLASS ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(accountId != null ? Restrictions.eq("accountId", accountId) : Restrictions.conjunction())
          // stack name or id can be stack name on non-deleted stacks or stack id on any stack
        .add(Restrictions.or(Restrictions.eq("stackName", stackNameOrId), Restrictions.eq("stackId", stackNameOrId)))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackEntity> entityList = criteria.list();
      if (entityList != null && !entityList.isEmpty()) {
        stackEntity = entityList.get(0);
      }
    }
    return stackEntity;
  }

  public static StackEntity getNonDeletedStackById(String stackId) {
    StackEntity stackEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( STACK_ENTITY_TRANSACTION_CLASS ) ) {
      List<StackEntity> entityList = Entities.criteriaQuery(StackEntity.class)
        .whereEqual(StackEntity_.stackId, stackId)
        .whereEqual(StackEntity_.recordDeleted, Boolean.FALSE)
        .list();
      if (entityList != null && !entityList.isEmpty()) {
        stackEntity = entityList.get(0);
      }
    }
    return stackEntity;
  }

  public static StackEntity getAnyStackByNameOrId(String stackNameOrId, String accountId) {
    StackEntity stackEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( STACK_ENTITY_TRANSACTION_CLASS ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add( accountId != null ? Restrictions.eq("accountId", accountId) : Restrictions.conjunction( ) )
          // stack name or id can be stack name on non-deleted stacks or stack id on any stack
        .add(Restrictions.or(
            Restrictions.and(Restrictions.eq("recordDeleted", Boolean.FALSE), Restrictions.eq("stackName", stackNameOrId)),
            Restrictions.eq("stackId", stackNameOrId))
        );
      List<StackEntity> entityList = criteria.list();
      if (entityList != null && !entityList.isEmpty()) {
        stackEntity = entityList.get(0);
      }
    }
    return stackEntity;
  }

  public static void deleteStack(String stackId, String accountId) {
    try (TransactionResource db =
           Entities.transactionFor(STACK_ENTITY_TRANSACTION_CLASS)) {
      for (Class stackEntityClass: ALL_STACK_ENTITY_CLASSES) {
        Criteria criteria = Entities.createCriteria(stackEntityClass)
          .add(Restrictions.eq("accountId", accountId))
          .add(Restrictions.eq("stackId", stackId))
          .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
        // in this case, all versions
        List<VersionedStackEntity> entityList = criteria.list();
        for (VersionedStackEntity stackEntity : entityList) {
          stackEntity.setRecordDeleted(Boolean.TRUE);
        }
      }
      db.commit();
    }
  }

  public static void reallyDeleteAllStackVersionsExcept(String stackId, String accountId, int stackVersion) {
    try (TransactionResource db =
           Entities.transactionFor(STACK_ENTITY_TRANSACTION_CLASS)) {
      for (Class stackEntityClass: ALL_STACK_ENTITY_CLASSES) {
        Criteria criteria = Entities.createCriteria(stackEntityClass)
          .add(Restrictions.eq("accountId", accountId))
          .add(Restrictions.eq("stackId", stackId))
          .add(Restrictions.ne("stackVersion", stackVersion))
          .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
        List<VersionedStackEntity> entityList = criteria.list();
        for (VersionedStackEntity stackEntity : entityList) {
          Entities.delete(stackEntity);
        }
      }
      db.commit();
    }
  }

  public static void updateStack(VersionedStackEntity stackEntity) {
    try ( TransactionResource db =
            Entities.transactionFor( STACK_ENTITY_TRANSACTION_CLASS ) ) {
      Criteria criteria = Entities.createCriteria(stackEntity.getClass())
        .add(Restrictions.eq("naturalId" , stackEntity.getNaturalId()));
      VersionedStackEntity dbEntity = (VersionedStackEntity) criteria.uniqueResult();
      if (dbEntity == null) {
        Entities.persist(stackEntity);
      } else {
        copyStackEntityFields(stackEntity, dbEntity);
      }
      db.commit( );
    }
  }

  private static void copyStackEntityFields(VersionedStackEntity src, VersionedStackEntity dest) {
    dest.setCreateOperationTimestamp(src.getCreateOperationTimestamp());
    dest.setLastUpdateOperationTimestamp(src.getLastUpdateOperationTimestamp());
    dest.setDeleteOperationTimestamp(src.getDeleteOperationTimestamp());
    dest.setAccountId(src.getAccountId());
    dest.setResourceDependencyManagerJson(src.getResourceDependencyManagerJson());
    dest.setCapabilitiesJson(src.getCapabilitiesJson());
    dest.setDescription(src.getDescription());
    dest.setDisableRollback(src.getDisableRollback());
    dest.setPseudoParameterMapJson(src.getPseudoParameterMapJson());
    dest.setConditionMapJson(src.getConditionMapJson());
    dest.setTemplateBody(src.getTemplateBody());
    dest.setMappingJson(src.getMappingJson());
    dest.setNotificationARNsJson(src.getNotificationARNsJson());
    dest.setWorkingOutputsJson(src.getWorkingOutputsJson());
    dest.setOutputsJson(src.getOutputsJson());
    dest.setParametersJson(src.getParametersJson());
    dest.setStackId(src.getStackId());
    dest.setStackName(src.getStackName());
    dest.setStackStatus(src.getStackStatus());
    dest.setStackStatusReason(src.getStackStatusReason());
    dest.setTagsJson(src.getTagsJson());
    dest.setTemplateFormatVersion(src.getTemplateFormatVersion());
    dest.setTimeoutInMinutes(src.getTimeoutInMinutes());
    dest.setRecordDeleted(src.getRecordDeleted());
    dest.setStackPolicy(src.getStackPolicy());
    dest.setStackVersion(src.getStackVersion());

  }
}

