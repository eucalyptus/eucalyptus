/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import com.eucalyptus.cloudformation.AlreadyExistsException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
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
  static final Logger LOG = Logger.getLogger(StackEntityManager.class);
  // more setters later...
  public static StackEntity addStack(StackEntity stackEntity) throws AlreadyExistsException {
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("stackName", stackEntity.getStackName()))
        .add(Restrictions.eq("accountId", stackEntity.getAccountId()))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackEntity> EntityList = criteria.list();
      if (!EntityList.isEmpty()) {
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
            Entities.transactionFor( StackEntity.class ) ) {
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
            Entities.transactionFor( StackEntity.class ) ) {
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
  public static StackEntity getNonDeletedStackById(String stackId, String accountId) {
    StackEntity stackEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackEntity> entityList = criteria.list();
      if (entityList != null && !entityList.isEmpty()) {
        stackEntity = entityList.get(0);
      }
    }
    return stackEntity;
  }

  public static StackEntity getNonDeletedStackByNameOrId(String stackNameOrId, String accountId) {
    StackEntity stackEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add( accountId != null ? Restrictions.eq("accountId", accountId) : Restrictions.conjunction( ) )
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

  public static StackEntity getAnyStackByNameOrId(String stackNameOrId, String accountId) {
    StackEntity stackEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
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

  public static void updateStack(StackEntity stackEntity) {
    try ( TransactionResource db =
            Entities.transactionFor( StackEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("naturalId" , stackEntity.getNaturalId()));
      StackEntity dbEntity = (StackEntity) criteria.uniqueResult();
      if (dbEntity == null) {
        Entities.persist(stackEntity);
      } else {
        dbEntity.setCreateOperationTimestamp(stackEntity.getCreateOperationTimestamp());
        dbEntity.setLastUpdateOperationTimestamp(stackEntity.getLastUpdateOperationTimestamp());
        dbEntity.setDeleteOperationTimestamp(stackEntity.getDeleteOperationTimestamp());
        dbEntity.setAccountId(stackEntity.getAccountId());
        dbEntity.setResourceDependencyManagerJson(stackEntity.getResourceDependencyManagerJson());
        dbEntity.setCapabilitiesJson(stackEntity.getCapabilitiesJson());
        dbEntity.setDescription(stackEntity.getDescription());
        dbEntity.setDisableRollback(stackEntity.getDisableRollback());
        dbEntity.setPseudoParameterMapJson(stackEntity.getPseudoParameterMapJson());
        dbEntity.setConditionMapJson(stackEntity.getConditionMapJson());
        dbEntity.setTemplateBody(stackEntity.getTemplateBody());
        dbEntity.setMappingJson(stackEntity.getMappingJson());
        dbEntity.setNotificationARNsJson(stackEntity.getNotificationARNsJson());
        dbEntity.setWorkingOutputsJson(stackEntity.getWorkingOutputsJson());
        dbEntity.setOutputsJson(stackEntity.getOutputsJson());
        dbEntity.setParametersJson(stackEntity.getParametersJson());
        dbEntity.setStackId(stackEntity.getStackId());
        dbEntity.setStackName(stackEntity.getStackName());
        dbEntity.setStackStatus(stackEntity.getStackStatus());
        dbEntity.setStackStatusReason(stackEntity.getStackStatusReason());
        dbEntity.setTagsJson(stackEntity.getTagsJson());
        dbEntity.setTemplateFormatVersion(stackEntity.getTemplateFormatVersion());
        dbEntity.setTimeoutInMinutes(stackEntity.getTimeoutInMinutes());
        dbEntity.setRecordDeleted(stackEntity.getRecordDeleted());
        dbEntity.setStackPolicy(stackEntity.getStackPolicy());
        // TODO: why doesn't the below work?
        // Entities.mergeDirect(stackEntity);
      }
      db.commit( );
    }
  }
}
