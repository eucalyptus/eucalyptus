/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceInfoHelper;
import com.eucalyptus.cloudformation.resources.ResourceResolverManager;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Lists;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by ethomas on 12/19/13.
 */
public class StackResourceEntityManager {
  public static void addStackResource(StackResourceEntity stackResourceEntity) {
    try ( TransactionResource db =
            Entities.transactionFor( stackResourceEntity.getClass() ) ) {
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

  public static void copyStackResourceEntityData(StackResourceEntity sourceEntity, StackResourceEntity destEntity) {
    destEntity.setRecordDeleted(sourceEntity.getRecordDeleted());
    destEntity.setDescription(sourceEntity.getDescription());
    destEntity.setLogicalResourceId(sourceEntity.getLogicalResourceId());
    destEntity.setPhysicalResourceId(sourceEntity.getPhysicalResourceId());
    destEntity.setResourceStatus(sourceEntity.getResourceStatus());
    destEntity.setResourceStatusReason(sourceEntity.getResourceStatusReason());
    destEntity.setResourceType(sourceEntity.getResourceType());
    destEntity.setStackId(sourceEntity.getStackId());
    destEntity.setStackName(sourceEntity.getStackName());
    destEntity.setAccountId(sourceEntity.getAccountId());
    destEntity.setMetadataJson(sourceEntity.getMetadataJson());
    destEntity.setReady(sourceEntity.getReady());
    destEntity.setPropertiesJson(sourceEntity.getPropertiesJson());
    destEntity.setUpdatePolicyJson(sourceEntity.getUpdatePolicyJson());
    destEntity.setDeletionPolicy(sourceEntity.getDeletionPolicy());
    destEntity.setAllowedByCondition(sourceEntity.getAllowedByCondition());
    destEntity.setReferenceValueJson(sourceEntity.getReferenceValueJson());
    destEntity.setResourceAttributesJson(sourceEntity.getResourceAttributesJson());
  }

  public static void updateStackResource(StackResourceEntity stackResourceEntity) {
    try ( TransactionResource db =
            Entities.transactionFor( stackResourceEntity.getClass() ) ) {
      Criteria criteria = Entities.createCriteria(stackResourceEntity.getClass())
        .add(Restrictions.eq("naturalId" , stackResourceEntity.getNaturalId()));
      StackResourceEntity dbEntity = (StackResourceEntity) criteria.uniqueResult();
      if (dbEntity == null) {
        Entities.persist(stackResourceEntity);
      } else {
        copyStackResourceEntityData(stackResourceEntity, dbEntity);
      }
      db.commit( );
    }
  }

  public static StackResourceEntity getStackResourceInUse(String stackId, String accountId, String logicalResourceId) {
    StackResourceEntity stackResourceEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntityInUse.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntityInUse.class)
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

  public static StackResourceEntity getStackResourceForUpdate(String stackId, String accountId, String logicalResourceId) {
    StackResourceEntity stackResourceEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntityForUpdate.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntityForUpdate.class)
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

  public static StackResourceEntity getStackResourceForCleanup(String stackId, String accountId, String logicalResourceId) {
    StackResourceEntity stackResourceEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntityForCleanup.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntityForCleanup.class)
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

  public static StackResourceEntity getStackResourceInUseByPhysicalResourceId(String stackId, String accountId, String physicalResourceId) {
    StackResourceEntity stackResourceEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntityInUse.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntityInUse.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("physicalResourceId" , physicalResourceId))
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
            Entities.transactionFor( StackResourceEntityInUse.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntityInUse.class)
        .add(Restrictions.eq( "accountId" , accountId))
        .add(Restrictions.eq( "stackId" , stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      stackResourceEntityList = criteria.list();
    }
    return stackResourceEntityList;
  }

  public static void deleteStackResourcesInUse(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntityInUse.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntityInUse.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      for (StackResourceEntity stackResourceEntity : (List<StackResourceEntityInUse>) criteria.list()) {
        stackResourceEntity.setRecordDeleted(Boolean.TRUE);
      }
      db.commit( );
    }
  }

  public static void deleteStackResourcesForCleanup(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntityForCleanup.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntityForCleanup.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      for (StackResourceEntity stackResourceEntity : (List<StackResourceEntityForCleanup>) criteria.list()) {
        stackResourceEntity.setRecordDeleted(Boolean.TRUE);
      }
      db.commit( );
    }
  }

  public static void deleteStackResourcesForUpdate(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntityForUpdate.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntityForUpdate.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      for (StackResourceEntity stackResourceEntity : (List<StackResourceEntityForUpdate>) criteria.list()) {
        stackResourceEntity.setRecordDeleted(Boolean.TRUE);
      }
      db.commit( );
    }
  }


  public static ResourceInfo getResourceInfo(String stackId, String accountId, String logicalResourceId)
    throws CloudFormationException {
    return getResourceInfo(getStackResourceInUse(stackId, accountId, logicalResourceId));
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
            Entities.transactionFor( StackResourceEntityInUse.class ) ) {
      // There is some weirdness in this request.  The stack name represents either the stack name of the
      // non-deleted stack or the stack id of the deleted or non-deleted stack.
      Criteria criteria = Entities.createCriteria(StackResourceEntityInUse.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.or(
          Restrictions.and(Restrictions.eq("recordDeleted", Boolean.FALSE), Restrictions.eq("stackName", stackNameOrId)),
          Restrictions.eq("stackId", stackNameOrId))
        )
        .add(Restrictions.ne("resourceStatus", Status.NOT_STARTED)); // placeholder, AWS doesn't return these
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
    }
    return matchingStackResourceEntity;
  }

  public static List<StackResourceEntity> describeStackResources( String accountId, String stackNameOrId ) {
    return describeStackResources(accountId, stackNameOrId, null, null);
  }

  public static List<StackResourceEntity> describeStackResources(
      @Nullable final String accountId,
      @Nullable final String stackNameOrId,
      @Nullable final String physicalResourceId,
      @Nullable final String logicalResourceId
  ) {
    if ( stackNameOrId == null && physicalResourceId == null ) {
      throw new IllegalArgumentException( "stackNameOrId or physicalResourceId required" );
    }
    try ( final TransactionResource db = Entities.transactionFor( StackResourceEntityInUse.class ) ) {
      // There is some weirdness in this request.  The stack name represents either the stack name of the
      // non-deleted stack or the stack id of the deleted or non-deleted stack.
      final Criteria criteria = Entities.createCriteria( StackResourceEntityInUse.class ).add( accountId != null ?
          Restrictions.eq("accountId", accountId) :
          Restrictions.conjunction( ) );

      if ( stackNameOrId != null ) { // stack explicitly specified
        criteria.add(Restrictions.or(
                Restrictions.and( Restrictions.eq( "recordDeleted", Boolean.FALSE ), Restrictions.eq( "stackName", stackNameOrId ) ),
                Restrictions.eq( "stackId", stackNameOrId ) )
        );
      }

      if ( physicalResourceId != null ) { // stack specified via physical resource identifier
        criteria.add( Subqueries.propertyIn(
            "stackId",
            DetachedCriteria.forClass( StackResourceEntityInUse.class, "subres" )
                .add( Restrictions.eq( "subres.physicalResourceId", physicalResourceId ) )
                .add( Restrictions.eq( "subres.recordDeleted", Boolean.FALSE ) )
                .setProjection( Projections.property( "subres.stackId" ) )
        ) );
      }

      if ( logicalResourceId != null ) { // filter results for a specific logical resource
        criteria.add( Restrictions.eq( "logicalResourceId", logicalResourceId ) );
      }

      criteria.add(Restrictions.ne("resourceStatus", Status.NOT_STARTED)); // placeholder, AWS doesn't return these

      //noinspection unchecked
      return Lists.<StackResourceEntity>newArrayList((List<StackResourceEntityInUse>) criteria.list());
    }
  }

  public static List<StackResourceEntity> listStackResources(String accountId, String stackNameOrId) {
    return describeStackResources(accountId, stackNameOrId);
  }

}
