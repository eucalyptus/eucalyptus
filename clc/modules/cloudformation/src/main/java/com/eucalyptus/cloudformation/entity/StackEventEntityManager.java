/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

import com.eucalyptus.cloudformation.common.msgs.StackEvent;
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

  public static void addSignalStackEvent(SignalEntity signal) {
    StackResourceEntity stackResourceEntity = StackResourceEntityManager.getStackResource(signal.getStackId(), signal.getAccountId(), signal.getLogicalResourceId(), signal.getResourceVersion());
    Date timestamp = new Date();
    String eventId = stackResourceEntity.getLogicalResourceId() + "-" + stackResourceEntity.getResourceStatus() + "-" + timestamp.getTime();
    addStackEvent(stackResourceEntity.getAccountId(), eventId, stackResourceEntity.getLogicalResourceId(),
      stackResourceEntity.getPhysicalResourceId(), stackResourceEntity.getPropertiesJson(),
      stackResourceEntity.getResourceStatus(), "Received " + signal.getStatus() + " signal with UniqueId " + signal.getUniqueId(),
      stackResourceEntity.getResourceType(), stackResourceEntity.getStackId(), stackResourceEntity.getStackName(),
      timestamp);
  }
}


