package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.cloudformation.StackResource;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Lists;
import net.sf.json.JSONObject;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Created by ethomas on 12/19/13.
 */
public class StackResourceEntityManager {
  public static void addStackResource(StackResource stackResource, JSONObject metadata) {
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Entities.persist(stackResourceToStackResourceEntity(stackResource, metadata));
      db.commit( );
    }

  }

  public static void updatePhysicalResourceId(String stackName, String logicalResourceId, String physicalResourceId) {
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq("stackName", stackName))
        .add(Restrictions.eq( "logicalResourceId" , logicalResourceId));
      List<StackResourceEntity> entityList = criteria.list();
      for (StackResourceEntity stackResourceEntity: entityList) {
        stackResourceEntity.setPhysicalResourceId(physicalResourceId);
      }
      db.commit( );
    }

  }

  public static void updateStatus(String stackName, String logicalResourceId, StackResourceEntity.Status status, String statusReason) {
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq("stackName", stackName))
        .add(Restrictions.eq("logicalResourceId", logicalResourceId));
      List<StackResourceEntity> entityList = criteria.list();
      for (StackResourceEntity stackResourceEntity: entityList) {
        stackResourceEntity.setResourceStatus(status);
        stackResourceEntity.setResourceStatusReason(statusReason);
      }
      db.commit( );
    }
  }

  public static StackResourceEntity getStackResource(String stackName, String logicalResourceId) {
    StackResourceEntity stackResourceEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq( "stackName" , stackName))
        .add(Restrictions.eq( "logicalResourceId" , logicalResourceId));
      List<StackResourceEntity> stackResourceEntityList = criteria.list();
      if (stackResourceEntityList != null && !stackResourceEntityList.isEmpty()) {
        stackResourceEntity = stackResourceEntityList.get(0);
      }
      db.commit( );
    }
    return stackResourceEntity;
  }

  public static List<StackResourceEntity> getStackResources(String stackName) {
    List<StackResourceEntity> stackResourceEntityList = Lists.newArrayList();
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq( "stackName" , stackName));
      stackResourceEntityList = criteria.list();
      db.commit( );
    }
    return stackResourceEntityList;
  }

  public static void deleteStackResources(String stackName) {
    try ( TransactionResource db =
            Entities.transactionFor( StackResourceEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackResourceEntity.class)
        .add(Restrictions.eq( "stackName" , stackName));
      for (StackResourceEntity stackResourceEntity: (List<StackResourceEntity>) criteria.list()) {
        Entities.delete(stackResourceEntity);
      }
      db.commit( );
    }
  }

  public static StackResource stackResourceEntityToStackResource(StackResourceEntity stackResourceEntity) {
    StackResource stackResource = new StackResource();
    stackResource.setDescription(stackResourceEntity.getDescription());
    stackResource.setLogicalResourceId(stackResourceEntity.getLogicalResourceId());
    stackResource.setPhysicalResourceId(stackResourceEntity.getPhysicalResourceId());
    stackResource.setResourceStatus(stackResourceEntity.getResourceStatus().toString());
    stackResource.setResourceStatusReason(stackResourceEntity.getResourceStatusReason());
    stackResource.setResourceType(stackResourceEntity.getResourceType());
    stackResource.setStackId(stackResourceEntity.getStackId());
    stackResource.setStackName(stackResourceEntity.getStackName());
    stackResource.setTimestamp(stackResourceEntity.getLastUpdateTimestamp());
    return stackResource;
  }

  public static StackResourceEntity stackResourceToStackResourceEntity(StackResource stackResource, JSONObject metadataJSONObject) {
    StackResourceEntity stackResourceEntity = new StackResourceEntity();
    stackResourceEntity.setDescription(stackResource.getDescription());
    stackResourceEntity.setLogicalResourceId(stackResource.getLogicalResourceId());
    stackResourceEntity.setPhysicalResourceId(stackResource.getPhysicalResourceId());
    stackResourceEntity.setResourceStatus(StackResourceEntity.Status.valueOf(stackResource.getResourceStatus()));
    stackResourceEntity.setResourceStatusReason(stackResource.getResourceStatusReason());
    stackResourceEntity.setResourceType(stackResource.getResourceType());
    stackResourceEntity.setStackId(stackResource.getStackId());
    stackResourceEntity.setStackName(stackResource.getStackName());
    stackResourceEntity.setMetadata(metadataJSONObject != null ? metadataJSONObject.toString() : null);
    return stackResourceEntity;
  }
}
