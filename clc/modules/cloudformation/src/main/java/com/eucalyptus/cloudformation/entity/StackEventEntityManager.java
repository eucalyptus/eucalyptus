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

import com.eucalyptus.cloudformation.StackEvent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Lists;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by ethomas on 12/19/13.
 */
public class StackEventEntityManager {

  public static void addStackEvent(StackResourceEntity stackResourceEntity) {
    Date timestamp = new Date();
    String eventId = stackResourceEntity.getLogicalResourceId() + "-" + stackResourceEntity.getResourceStatus() + "-" + timestamp.getTime();
    addStackEvent(stackResourceEntity.getAccountId(), eventId, stackResourceEntity.getLogicalResourceId(),
      stackResourceEntity.getPhysicalResourceId(), stackResourceEntity.getPropertiesJson(),
      stackResourceEntity.getResourceStatus(), stackResourceEntity.getResourceStatusReason(),
      stackResourceEntity.getResourceType(), stackResourceEntity.getStackId(), stackResourceEntity.getStackName(),
      timestamp);
  }

  public static void addStackEvent(String accountId, String eventId, String logicalResourceId,
                                   String physicalResourceId, String resourceProperties, Status resourceStatus,
                                   String resourceStatusReason, String resourceType, String stackId, String stackName,
                                   Date timestamp) {
    StackEventEntity stackEventEntity = new StackEventEntity();
    stackEventEntity.setRecordDeleted(Boolean.FALSE);
    stackEventEntity.setAccountId(accountId);
    stackEventEntity.setEventId(eventId);
    stackEventEntity.setLogicalResourceId(logicalResourceId);
    stackEventEntity.setPhysicalResourceId(physicalResourceId);
    stackEventEntity.setResourceProperties(resourceProperties);
    stackEventEntity.setResourceStatus(resourceStatus);
    stackEventEntity.setResourceStatusReason(resourceStatusReason);
    stackEventEntity.setResourceType(resourceType);
    stackEventEntity.setStackId(stackId);
    stackEventEntity.setStackName(stackName);
    stackEventEntity.setTimestamp(timestamp);
    try ( TransactionResource db =
            Entities.transactionFor(StackEventEntity.class) ) {
      Entities.persist(stackEventEntity);
      db.commit( );
    }
  }

  public static void deleteStackEvents(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StackEventEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEventEntity.class)
        .add(Restrictions.eq( "accountId" , accountId))
        .add(Restrictions.eq( "stackId" , stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackEventEntity> entityList = criteria.list();
      for (StackEventEntity stackEventEntity: entityList) {
        stackEventEntity.setRecordDeleted(Boolean.TRUE);
      }
      db.commit( );
    }
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

  public static ArrayList<StackEvent> getStackEventsByNameOrId(String stackNameOrId, String accountId) {
    ArrayList<StackEvent> returnValue = Lists.newArrayList();
    try ( TransactionResource db =
            Entities.transactionFor( StackEventEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEventEntity.class)
        .add(accountId != null ? Restrictions.eq("accountId", accountId) : Restrictions.conjunction( ))
        .add(Restrictions.or(
            Restrictions.and(Restrictions.eq("recordDeleted", Boolean.FALSE), Restrictions.eq("stackName", stackNameOrId)),
            Restrictions.eq("stackId", stackNameOrId))
        ).addOrder( Order.desc("timestamp") );
      List<StackEventEntity> results = criteria.list();
      if (results != null) {
        for (StackEventEntity stackEventEntity: results) {
          returnValue.add(stackEventEntityToStackEvent(stackEventEntity));
        }
      }
    }
    return returnValue;
  }

  public static ArrayList<StackEventEntity> getStackEventEntitiesById(String stackId, String accountId) {
    ArrayList<StackEventEntity> returnValue = Lists.newArrayList();
    try ( TransactionResource db =
            Entities.transactionFor( StackEventEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackEventEntity.class)
        .add(Restrictions.eq( "accountId" , accountId))
        .add(Restrictions.eq( "stackId" , stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackEventEntity> entityList = criteria.list();
      if (entityList != null) {
        returnValue.addAll(entityList);
      }
    }
    return returnValue;
  }
}


