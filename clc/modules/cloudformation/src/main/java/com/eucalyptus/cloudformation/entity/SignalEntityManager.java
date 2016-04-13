/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Lists;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;
import java.util.List;

/**
 * Created by ethomas on 3/30/16.
 */
public class SignalEntityManager {

  public static SignalEntity addSignal(SignalEntity signalEntity) {
    try (TransactionResource db =
           Entities.transactionFor( SignalEntity.class ) ) {
      Entities.persist(signalEntity);
      db.commit( );
    }
    return signalEntity;
  }


  public static void updateSignal(SignalEntity signalEntity) {
    try ( TransactionResource db =
            Entities.transactionFor( SignalEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(SignalEntity.class)
        .add(Restrictions.eq("naturalId" , signalEntity.getNaturalId()));
      SignalEntity dbEntity = (SignalEntity) criteria.uniqueResult();
      if (dbEntity == null) {
        Entities.persist(signalEntity);
      } else {
        copySignalData(signalEntity, dbEntity);
      }
      db.commit( );
    }
  }

  public static void copySignalData(SignalEntity sourceEntity, SignalEntity destEntity) {
    destEntity.setStackId(sourceEntity.getStackId());
    destEntity.setAccountId(sourceEntity.getAccountId());
    destEntity.setLogicalResourceId(sourceEntity.getLogicalResourceId());
    destEntity.setResourceVersion(sourceEntity.getResourceVersion());
    destEntity.setUniqueId(sourceEntity.getUniqueId());
    destEntity.setStatus(sourceEntity.getStatus());
    destEntity.setProcessed(sourceEntity.getProcessed());
  }

  public static SignalEntity getSignal(String stackId, String accountId, String logicalResourceId, Integer resourceVersion, String uniqueId) {
    SignalEntity signalEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( SignalEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(SignalEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("logicalResourceId" , logicalResourceId))
        .add(Restrictions.eq("resourceVersion", resourceVersion))
        .add(Restrictions.eq("uniqueId", uniqueId));
      List<SignalEntity> signalEntityList = criteria.list();
      if (signalEntityList != null && !signalEntityList.isEmpty()) {
        signalEntity = signalEntityList.get(0);
      }
    }
    return signalEntity;
  }

  public static Collection<SignalEntity> getSignals(String stackId, String accountId, String logicalResourceId, Integer resourceVersion) {
    Collection<SignalEntity> retVal = Lists.newArrayList();
    try ( TransactionResource db =
            Entities.transactionFor( SignalEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(SignalEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("logicalResourceId" , logicalResourceId))
        .add(Restrictions.eq("resourceVersion", resourceVersion));
      List<SignalEntity> signalEntityList = criteria.list();
      for (SignalEntity signalEntity: signalEntityList) {
        retVal.add(signalEntity);
      }
    }
    return retVal;
  }

  public static void deleteSignals(String stackId, String accountId, String logicalResourceId, Integer resourceVersion) {
    Collection<SignalEntity> retVal = Lists.newArrayList();
    try ( TransactionResource db =
            Entities.transactionFor( SignalEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(SignalEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId));
      if (logicalResourceId != null) {
        criteria.add(Restrictions.eq("logicalResourceId" , logicalResourceId));
      }
      if (resourceVersion != null) {
        criteria.add(Restrictions.eq("resourceVersion" , resourceVersion));
      }
      List<SignalEntity> signalEntityList = criteria.list();
      for (SignalEntity signalEntity: signalEntityList) {
        Entities.delete(signalEntity);
      }
      db.commit();
    }
  }

  public static void deleteSignals(String stackId, String accountId, String logicalResourceId) {
    deleteSignals(stackId, accountId, logicalResourceId, null);
  }

  public static void deleteSignals(String stackId, String accountId) {
    deleteSignals(stackId, accountId, null,null);
  }

}
