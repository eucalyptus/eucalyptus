package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.cloudformation.StackEvent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Created by ethomas on 12/19/13.
 */
public class StackEventEntityManager {

  public static void addStackEvent(StackEvent stackEvent) {
    try ( TransactionResource db =
            Entities.transactionFor(StackEventEntity.class) ) {
      Entities.persist(stackEventToStackEventEntity(stackEvent));
      db.commit( );
    }
  }

  public static void deleteStackEvents(String stackName) {
    try ( TransactionResource db =
            Entities.transactionFor( StackEventEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEventEntity.class)
        .add(Restrictions.eq( "stackName" , stackName));
      List<StackEventEntity> entityList = criteria.list();
      for (StackEventEntity stackEventEntity: entityList) {
        Entities.delete(stackEventEntity);
      }
      db.commit( );
    }
  }

  public static StackEventEntity stackEventToStackEventEntity(StackEvent stackEvent) {
    StackEventEntity stackEventEntity = new StackEventEntity();
    stackEventEntity.setEventId(stackEvent.getEventId());
    stackEventEntity.setLogicalResourceId(stackEvent.getLogicalResourceId());
    stackEventEntity.setPhysicalResourceId(stackEvent.getPhysicalResourceId());
    stackEventEntity.setResourceProperties(stackEvent.getResourceProperties());
    stackEventEntity.setResourceStatus(StackResourceEntity.Status.valueOf(stackEvent.getResourceStatus()));
    stackEventEntity.setResourceStatusReason(stackEvent.getResourceStatusReason());
    stackEventEntity.setResourceType(stackEvent.getResourceType());
    stackEventEntity.setStackId(stackEvent.getStackId());
    stackEventEntity.setStackName(stackEvent.getStackName());
    stackEventEntity.setTimestamp(stackEvent.getTimestamp());
    return stackEventEntity;
  }

  public static StackEvent stackEventEntityToStackEvent(StackEventEntity stackEventEntity) {
    StackEvent stackEvent = new StackEvent();
    stackEvent.setEventId(stackEventEntity.getEventId());
    stackEvent.setLogicalResourceId(stackEventEntity.getLogicalResourceId());
    stackEvent.setPhysicalResourceId(stackEventEntity.getPhysicalResourceId());
    stackEvent.setResourceProperties(stackEventEntity.getResourceProperties());
    stackEvent.setResourceStatus(stackEventEntity.getResourceStatus().toString());
    stackEvent.setResourceStatusReason(stackEventEntity.getResourceStatusReason());
    stackEvent.setResourceType(stackEventEntity.getResourceType());
    stackEvent.setStackId(stackEventEntity.getStackId());
    stackEvent.setStackName(stackEventEntity.getStackName());
    stackEvent.setTimestamp(stackEventEntity.getTimestamp());
    return stackEvent;
  }

}


