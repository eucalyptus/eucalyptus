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

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.InternalFailureException;
import com.eucalyptus.cloudformation.StackResource;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceInfoHelper;
import com.eucalyptus.cloudformation.resources.ResourceResolverManager;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;
import java.util.List;

/**
 * Created by ethomas on 12/19/13.
 */
public class StackResourceEntityManager {
  public static void addStackResource(StackResourceEntity stackResourceEntity) {
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Entities.persist(stackResourceEntity);
      db.commit( );
    }

  }

  public static StackResourceEntity updateResourceInfo(StackResourceEntity stackResourceEntity, ResourceInfo resourceInfo) throws CloudFormationException {
    stackResourceEntity.setAccountId(resourceInfo.getAccountId());
    stackResourceEntity.setResourceType(resourceInfo.getType());
    stackResourceEntity.setAllowedByCondition(resourceInfo.getAllowedByCondition());
    stackResourceEntity.setDeletionPolicy(resourceInfo.getDeletionPolicy());
    stackResourceEntity.setDescription(resourceInfo.getDescription());
    stackResourceEntity.setLogicalResourceId(resourceInfo.getLogicalResourceId());
    stackResourceEntity.setMetadataJson(resourceInfo.getMetadataJson());
    stackResourceEntity.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
    stackResourceEntity.setPropertiesJson(resourceInfo.getPropertiesJson());
    stackResourceEntity.setReady(resourceInfo.getReady());
    stackResourceEntity.setReferenceValueJson(resourceInfo.getReferenceValueJson());
    stackResourceEntity.setUpdatePolicyJson(resourceInfo.getUpdatePolicyJson());
    stackResourceEntity.setResourceAttributesJson(ResourceInfoHelper.getResourceAttributesJson(resourceInfo));
    return stackResourceEntity;
  }

  public static void updateStackResource(StackResourceEntity stackResourceEntity) {
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq("naturalId" , stackResourceEntity.getNaturalId()));
      StackResourceEntity dbEntity = (StackResourceEntity) criteria.uniqueResult();
      if (dbEntity == null) {
        Entities.persist(stackResourceEntity);
      } else {
        dbEntity.setRecordDeleted(stackResourceEntity.getRecordDeleted());
        dbEntity.setDescription(stackResourceEntity.getDescription());
        dbEntity.setLogicalResourceId(stackResourceEntity.getLogicalResourceId());
        dbEntity.setPhysicalResourceId(stackResourceEntity.getPhysicalResourceId());
        dbEntity.setResourceStatus(stackResourceEntity.getResourceStatus());
        dbEntity.setResourceStatusReason(stackResourceEntity.getResourceStatusReason());
        dbEntity.setResourceType(stackResourceEntity.getResourceType());
        dbEntity.setStackId(stackResourceEntity.getStackId());
        dbEntity.setStackName(stackResourceEntity.getStackName());
        dbEntity.setAccountId(stackResourceEntity.getAccountId());
        dbEntity.setMetadataJson(stackResourceEntity.getMetadataJson());
        dbEntity.setReady(stackResourceEntity.getReady());
        dbEntity.setPropertiesJson(stackResourceEntity.getPropertiesJson());
        dbEntity.setUpdatePolicyJson(stackResourceEntity.getUpdatePolicyJson());
        dbEntity.setDeletionPolicy(stackResourceEntity.getDeletionPolicy());
        dbEntity.setAllowedByCondition(stackResourceEntity.getAllowedByCondition());
        dbEntity.setReferenceValueJson(stackResourceEntity.getReferenceValueJson());
        dbEntity.setResourceAttributesJson(stackResourceEntity.getResourceAttributesJson());
        // TODO: why doesn't the below work?
        // Entities.mergeDirect(stackResourceEntity);
      }
      db.commit( );
    }
  }

  public static StackResourceEntity getStackResource(String stackId, String accountId, String logicalResourceId) {
    StackResourceEntity stackResourceEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("logicalResourceId" , logicalResourceId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackResourceEntity> stackResourceEntityList = criteria.list();
      if (stackResourceEntityList != null && !stackResourceEntityList.isEmpty()) {
        stackResourceEntity = stackResourceEntityList.get(0);
      }
      db.commit( );
    }
    return stackResourceEntity;
  }

  public static List<StackResourceEntity> getStackResources(String stackId, String accountId) {
    List<StackResourceEntity> stackResourceEntityList = Lists.newArrayList();
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq( "accountId" , accountId))
        .add(Restrictions.eq( "stackId" , stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      stackResourceEntityList = criteria.list();
      db.commit( );
    }
    return stackResourceEntityList;
  }

  public static void deleteStackResources(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq( "accountId" , accountId))
        .add(Restrictions.eq( "stackName" , stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      for (StackResourceEntity stackResourceEntity: (List<StackResourceEntity>) criteria.list()) {
        stackResourceEntity.setRecordDeleted(Boolean.TRUE);
      }
      db.commit( );
    }
  }

  public static ResourceInfo getResourceInfo(String stackId, String accountId, String logicalResourceId)
    throws CloudFormationException {
    return getResourceInfo(getStackResource(stackId, accountId, logicalResourceId));
  }

  public static ResourceInfo getResourceInfo(StackResourceEntity stackResourceEntity)
    throws CloudFormationException {
    if (stackResourceEntity == null) return null;
    ResourceInfo resourceInfo = new ResourceResolverManager().resolveResourceInfo(stackResourceEntity.getResourceType());
    resourceInfo.setAccountId(stackResourceEntity.getAccountId());
    resourceInfo.setAllowedByCondition(stackResourceEntity.getAllowedByCondition());
    resourceInfo.setDescription(stackResourceEntity.getDescription());
    resourceInfo.setDeletionPolicy(stackResourceEntity.getDeletionPolicy());
    resourceInfo.setLogicalResourceId(stackResourceEntity.getLogicalResourceId());
    resourceInfo.setMetadataJson(stackResourceEntity.getMetadataJson());
    resourceInfo.setPhysicalResourceId(stackResourceEntity.getPhysicalResourceId());
    resourceInfo.setPropertiesJson(stackResourceEntity.getPropertiesJson());
    resourceInfo.setReady(stackResourceEntity.getReady());
    resourceInfo.setReferenceValueJson(stackResourceEntity.getReferenceValueJson());
    resourceInfo.setUpdatePolicyJson(stackResourceEntity.getUpdatePolicyJson());
    ResourceInfoHelper.setResourceAttributesJson(resourceInfo, stackResourceEntity.getResourceAttributesJson());
    return resourceInfo;
  }

  public static StackResourceEntity describeStackResource(String accountId, String stackNameOrId, String logicalResourceId)  throws CloudFormationException {
    StackResourceEntity matchingStackResourceEntity = null;
    String stackId = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      // There is some weirdness in this request.  The stack name represents either the stack name of the
      // non-deleted stack or the stack id of the deleted or non-deleted stack.
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.or(
          Restrictions.and(Restrictions.eq("recordDeleted", Boolean.FALSE), Restrictions.eq("stackName", stackNameOrId)),
          Restrictions.eq("stackId", stackNameOrId))
        )
        .add(Restrictions.ne("resourceStatus", StackResourceEntity.Status.NOT_STARTED)); // placeholder, AWS doesn't return these
      List<StackResourceEntity> result = criteria.list();
      if (result == null || result.isEmpty()) {
        // TODO: in theory the stack may exist but with no resources.  Either way though there is an error, so this is ok.
        throw new ValidationErrorException("Stack with name " + stackNameOrId +" does not exist");
      }
      for (StackResourceEntity stackResourceEntity: result) {
        if (stackId == null) {
          stackId = stackResourceEntity.getStackId();
        } else if (!stackId.equals(stackResourceEntity.getStackId())) {
          throw new InternalFailureException("Got results from more than one stack");
        }
        if (logicalResourceId.equals(stackResourceEntity.getLogicalResourceId())) {
          if (matchingStackResourceEntity != null) {
            throw new InternalFailureException("More than one record exists for Resource " + logicalResourceId + " on stack " + stackId);
          } else {
            matchingStackResourceEntity = stackResourceEntity;
          }
        }
      }
      if (matchingStackResourceEntity == null) {
        throw new ValidationErrorException("Resource " + logicalResourceId + " does not exist for stack " + stackId);
      }
      db.commit( );
    }
    return matchingStackResourceEntity;
  }

  public static List<StackResourceEntity> describeStackResources(String accountId, String stackNameOrId, String physicalResourceId, String logicalResourceId) {
    List<StackResourceEntity> returnValue = null;
    String stackId = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      // There is some weirdness in this request.  The stack name represents either the stack name of the
      // non-deleted stack or the stack id of the deleted or non-deleted stack.
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq("accountId", accountId));
      if (stackNameOrId != null) {
        criteria.add(Restrictions.or(
          Restrictions.and(Restrictions.eq("recordDeleted", Boolean.FALSE), Restrictions.eq("stackName", stackNameOrId)),
          Restrictions.eq("stackId", stackNameOrId))
        );
      }
      if (logicalResourceId != null) {
        criteria.add(Restrictions.eq("logicalResourceId", logicalResourceId));
      }
      if (physicalResourceId != null) {
        criteria.add(Restrictions.eq("physicalResourceId", logicalResourceId));
      }
      returnValue = criteria.list();
      db.commit( );
    }
    return returnValue;
  }

  public static List<StackResourceEntity> listStackResources(String accountId, String stackNameOrId) {
    return describeStackResources(accountId, stackNameOrId, null, null);
  }

}
