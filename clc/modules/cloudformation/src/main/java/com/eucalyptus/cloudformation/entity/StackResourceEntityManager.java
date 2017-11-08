/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.InternalFailureException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceInfoHelper;
import com.eucalyptus.cloudformation.resources.ResourceResolverManager;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

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
    stackResourceEntity.setCreatedEnoughToDelete(resourceInfo.getCreatedEnoughToDelete());
    stackResourceEntity.setDeletionPolicy(resourceInfo.getDeletionPolicy());
    stackResourceEntity.setDescription(resourceInfo.getDescription());
    stackResourceEntity.setLogicalResourceId(resourceInfo.getLogicalResourceId());
    stackResourceEntity.setMetadataJson(resourceInfo.getMetadataJson());
    stackResourceEntity.setPhysicalResourceId(resourceInfo.getPhysicalResourceId());
    stackResourceEntity.setPropertiesJson(resourceInfo.getPropertiesJson());
    stackResourceEntity.setReady(resourceInfo.getReady());
    stackResourceEntity.setReferenceValueJson(resourceInfo.getReferenceValueJson());
    stackResourceEntity.setUpdatePolicyJson(resourceInfo.getUpdatePolicyJson());
    stackResourceEntity.setCreationPolicyJson(resourceInfo.getCreationPolicyJson());
    stackResourceEntity.setResourceAttributesJson(ResourceInfoHelper.getResourceAttributesJson(resourceInfo));
    return stackResourceEntity;
  }

  public static void copyStackResourceEntityData(StackResourceEntity sourceEntity, StackResourceEntity destEntity) {
    destEntity.setRecordDeleted(sourceEntity.getRecordDeleted());
    destEntity.setCreatedEnoughToDelete(sourceEntity.getCreatedEnoughToDelete());
    destEntity.setUpdateType(sourceEntity.getUpdateType());
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
    destEntity.setCreationPolicyJson(sourceEntity.getCreationPolicyJson());
    destEntity.setDeletionPolicy(sourceEntity.getDeletionPolicy());
    destEntity.setAllowedByCondition(sourceEntity.getAllowedByCondition());
    destEntity.setReferenceValueJson(sourceEntity.getReferenceValueJson());
    destEntity.setResourceAttributesJson(sourceEntity.getResourceAttributesJson());
    destEntity.setResourceVersion(sourceEntity.getResourceVersion());
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

  public static StackResourceEntity getStackResource(String stackId, String accountId, String logicalResourceId, int resourceVersion) {
    StackResourceEntity stackResourceEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("logicalResourceId" , logicalResourceId))
        .add(Restrictions.eq("resourceVersion", resourceVersion))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackResourceEntity> stackResourceEntityList = criteria.list();
      if (stackResourceEntityList != null && !stackResourceEntityList.isEmpty()) {
        stackResourceEntity = stackResourceEntityList.get(0);
      }
    }
    return stackResourceEntity;
  }


  public static StackResourceEntity getStackResourceByPhysicalResourceId(String stackId, String accountId, String physicalResourceId, int resourceVersion) {
    StackResourceEntity stackResourceEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("physicalResourceId" , physicalResourceId))
        .add(Restrictions.eq("resourceVersion", resourceVersion))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackResourceEntity> stackResourceEntityList = criteria.list();
      if (stackResourceEntityList != null && !stackResourceEntityList.isEmpty()) {
        stackResourceEntity = stackResourceEntityList.get(0);
      }
      db.commit( );
    }
    return stackResourceEntity;
  }

  public static List<StackResourceEntity> getStackResources(String stackId, String accountId, int resourceVersion) {
    List<StackResourceEntity> stackResourceEntityList = Lists.newArrayList();
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq( "accountId" , accountId))
        .add(Restrictions.eq( "stackId" , stackId))
        .add(Restrictions.eq("resourceVersion", resourceVersion))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      stackResourceEntityList = criteria.list();
    }
    return stackResourceEntityList;
  }

  public static void deleteStackResources(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      for (StackResourceEntity stackResourceEntity : (List<StackResourceEntity>) criteria.list()) {
        stackResourceEntity.setRecordDeleted(Boolean.TRUE);
      }
      db.commit( );
    }
  }

  public static void reallyDeleteAllVersionsExcept(String stackId, String accountId, int resourceVersion) {
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.ne("resourceVersion", resourceVersion))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      for (StackResourceEntity stackResourceEntity : (List<StackResourceEntity>) criteria.list()) {
        Entities.delete(stackResourceEntity);
      }
      db.commit( );
    }
  }

  public static ResourceInfo getResourceInfo(String stackId, String accountId, String logicalResourceId, int resourceVersion)
    throws CloudFormationException {
    return getResourceInfo(getStackResource(stackId, accountId, logicalResourceId, resourceVersion));
  }

  public static ResourceInfo getResourceInfo(StackResourceEntity stackResourceEntity)
    throws CloudFormationException {
    if (stackResourceEntity == null) return null;
    ResourceInfo resourceInfo = new ResourceResolverManager().resolveResourceInfo(stackResourceEntity.getResourceType());
    resourceInfo.setAccountId(stackResourceEntity.getAccountId());
    resourceInfo.setAllowedByCondition(stackResourceEntity.getAllowedByCondition());
    resourceInfo.setCreatedEnoughToDelete(stackResourceEntity.getCreatedEnoughToDelete());
    resourceInfo.setDescription(stackResourceEntity.getDescription());
    resourceInfo.setDeletionPolicy(stackResourceEntity.getDeletionPolicy());
    resourceInfo.setLogicalResourceId(stackResourceEntity.getLogicalResourceId());
    resourceInfo.setMetadataJson(stackResourceEntity.getMetadataJson());
    resourceInfo.setPhysicalResourceId(stackResourceEntity.getPhysicalResourceId());
    resourceInfo.setPropertiesJson(stackResourceEntity.getPropertiesJson());
    resourceInfo.setReady(stackResourceEntity.getReady());
    resourceInfo.setReferenceValueJson(stackResourceEntity.getReferenceValueJson());
    resourceInfo.setUpdatePolicyJson(stackResourceEntity.getUpdatePolicyJson());
    resourceInfo.setCreationPolicyJson(stackResourceEntity.getCreationPolicyJson());
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
        .add(accountId!=null ? Restrictions.eq("accountId", accountId) : Restrictions.conjunction( ))
        .add(Restrictions.or(
          Restrictions.and(Restrictions.eq("recordDeleted", Boolean.FALSE), Restrictions.eq("stackName", stackNameOrId)),
          Restrictions.eq("stackId", stackNameOrId))
        )
        .add(Restrictions.ne("resourceStatus", Status.NOT_STARTED)); // placeholder, AWS doesn't return these
      List<StackResourceEntity> result = takeLatestVersions(criteria.list());
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
    try ( final TransactionResource db = Entities.transactionFor( StackResourceEntity.class ) ) {
      // There is some weirdness in this request.  The stack name represents either the stack name of the
      // non-deleted stack or the stack id of the deleted or non-deleted stack.
      final Criteria criteria = Entities.createCriteria( StackResourceEntity.class ).add( accountId != null ?
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
            DetachedCriteria.forClass( StackResourceEntity.class, "subres" )
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
      return takeLatestVersions(criteria.list());
    }
  }

  public static List<StackResourceEntity> listStackResources(String accountId, String stackNameOrId) {
    return describeStackResources(accountId, stackNameOrId);
  }

  private static List<StackResourceEntity> takeLatestVersions(List<StackResourceEntity> original) {
    if (original == null) return null;
    Map<String, StackResourceEntity> latestVersionMap = Maps.newLinkedHashMap();
    for (StackResourceEntity stackResourceEntity : original) {
      String key = stackResourceEntity.getAccountId() + " | " + stackResourceEntity.getStackId() + " | " + stackResourceEntity.getLogicalResourceId();
      if (!latestVersionMap.containsKey(key)) {
        latestVersionMap.put(key, stackResourceEntity);
      } else {
        StackResourceEntity alreadyInMapStackResourceEntity = latestVersionMap.get(key);
        if (alreadyInMapStackResourceEntity.getResourceVersion() < stackResourceEntity.getResourceVersion()) {
          latestVersionMap.put(key, stackResourceEntity);
        }
      }
    }
    return Lists.newArrayList(latestVersionMap.values());
  }

  public static void flattenResources(String stackId, String accountId, int finalResourceVersion) {
    // This method is used for delete.  It essentially takes all the items you would get from describeResources() and makes them the only version that is seen.
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackResourceEntity> stackResourceEntityList = criteria.list();
      Map<String, List<StackResourceEntity>> stackResourceEntityVersionMap = Maps.newHashMap();
      if (stackResourceEntityList != null) {
        for (StackResourceEntity stackResourceEntity : stackResourceEntityList) {
          String key = stackResourceEntity.getAccountId() + " | " + stackResourceEntity.getStackId() + " | " + stackResourceEntity.getLogicalResourceId();
          if (!stackResourceEntityVersionMap.containsKey(key)) {
            stackResourceEntityVersionMap.put(key, Lists.<StackResourceEntity>newArrayList());
          }
          stackResourceEntityVersionMap.get(key).add(stackResourceEntity);
        }
        for (List<StackResourceEntity> stackResourceEntityPerKeyList : stackResourceEntityVersionMap.values()) {
          StackResourceEntity maxEntity = null;
          for (StackResourceEntity stackResourceEntity : stackResourceEntityPerKeyList) {
            if (maxEntity == null) {
              maxEntity = stackResourceEntity;
            } else if (stackResourceEntity.getResourceStatus() != Status.NOT_STARTED && maxEntity.getResourceStatus() == Status.NOT_STARTED) {
              maxEntity = stackResourceEntity;
            } else if (stackResourceEntity.getResourceStatus() == Status.NOT_STARTED && maxEntity.getResourceStatus() == Status.NOT_STARTED
              && stackResourceEntity.getResourceVersion() > maxEntity.getResourceVersion()) {
              maxEntity = stackResourceEntity;
            } else if (stackResourceEntity.getResourceStatus() != Status.NOT_STARTED && maxEntity.getResourceStatus() != Status.NOT_STARTED
              && stackResourceEntity.getResourceVersion() > maxEntity.getResourceVersion()) {
              maxEntity = stackResourceEntity;
            }
          }
          maxEntity.setResourceVersion(finalResourceVersion);
          for (StackResourceEntity stackResourceEntity : stackResourceEntityPerKeyList) {
            if (stackResourceEntity != maxEntity) { // this is a reference equals on purpose
              Entities.delete(stackResourceEntity);
            }
          }
        }
      }
      db.commit();
    }
  }

  public static String findOuterStackArnIfExists(String stackId, String accountId) throws ValidationErrorException {
    String returnValue = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("physicalResourceId", stackId)) // inner stack ARN == physical resource id
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackResourceEntity> stackResourceEntityList = criteria.list();
      if (stackResourceEntityList != null) {
        for (StackResourceEntity stackResourceEntity : stackResourceEntityList) {
          if (returnValue == null) {
            returnValue = stackResourceEntity.getStackId();
          }
          if (!returnValue.equals(stackResourceEntity.getStackId())) {
            throw new ValidationErrorException("Stack " + stackId + " is a resource in more than one stack:" + returnValue + " and " + stackResourceEntity.getStackId());
          }
        }
      }
      return returnValue;
    }
  }
}
