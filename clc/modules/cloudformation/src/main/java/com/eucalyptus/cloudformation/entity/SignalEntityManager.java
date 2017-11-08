/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
