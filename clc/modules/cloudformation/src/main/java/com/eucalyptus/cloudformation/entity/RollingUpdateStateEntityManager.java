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

import com.eucalyptus.cloudformation.resources.standard.actions.AWSAutoScalingAutoScalingGroupResourceAction;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Created by ethomas on 4/19/16.
 */
public class RollingUpdateStateEntityManager {
  public static void copy(RollingUpdateStateEntity sourceEntity, RollingUpdateStateEntity destEntity) {
    destEntity.setAccountId(sourceEntity.getAccountId());
    destEntity.setStackId(sourceEntity.getStackId());
    destEntity.setLogicalResourceId(sourceEntity.getLogicalResourceId());
    destEntity.setMinSize(sourceEntity.getMinSize());
    destEntity.setMaxSize(sourceEntity.getMaxSize());
    destEntity.setDesiredCapacity(sourceEntity.getDesiredCapacity());
    destEntity.setState(sourceEntity.getState());
    destEntity.setPreviousRunningInstanceIds(sourceEntity.getPreviousRunningInstanceIds());
    destEntity.setCurrentBatchInstanceIds(sourceEntity.getCurrentBatchInstanceIds());
    destEntity.setObsoleteInstancesJson(sourceEntity.getObsoleteInstancesJson());
    destEntity.setAlreadySuspendedProcessNames(sourceEntity.getAlreadySuspendedProcessNames());
    destEntity.setNumExpectedTotalSignals(sourceEntity.getNumExpectedTotalSignals());
    destEntity.setSignalCutoffTimestamp(sourceEntity.getSignalCutoffTimestamp());
    destEntity.setBatchSize(sourceEntity.getBatchSize());
    destEntity.setTempDesiredCapacity(sourceEntity.getTempDesiredCapacity());
    destEntity.setNeedsRollbackUpdate(sourceEntity.getNeedsRollbackUpdate());
  }

  public static RollingUpdateStateEntity getRollingUpdateStateEntity(String accountId, String stackId, String logicalResourceId) {
    RollingUpdateStateEntity RollingUpdateStateEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( RollingUpdateStateEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(RollingUpdateStateEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("logicalResourceId" , logicalResourceId));
      List<RollingUpdateStateEntity> rollingUpdateStateEntityList = criteria.list();
      if (rollingUpdateStateEntityList != null && !rollingUpdateStateEntityList.isEmpty()) {
        RollingUpdateStateEntity = rollingUpdateStateEntityList.get(0);
      }
    }
    return RollingUpdateStateEntity;
  }


  public static void deleteRollingUpdateStateEntity(String accountId, String stackId, String logicalResourceId) {
    RollingUpdateStateEntity RollingUpdateStateEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( RollingUpdateStateEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(RollingUpdateStateEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("logicalResourceId" , logicalResourceId));
      List<RollingUpdateStateEntity> rollingUpdateStateEntityList = criteria.list();
      if (rollingUpdateStateEntityList != null && !rollingUpdateStateEntityList.isEmpty()) {
        for (RollingUpdateStateEntity rollingUpdateStateEntity: rollingUpdateStateEntityList) {
          Entities.delete(rollingUpdateStateEntity);
        }
      }
      db.commit();
    }
  }

  public static RollingUpdateStateEntity createRollingUpdateStateEntity(String accountId, String stackId, String logicalResourceId) {
    RollingUpdateStateEntity rollingUpdateStateEntity = new RollingUpdateStateEntity();
    rollingUpdateStateEntity.setAccountId(accountId);
    rollingUpdateStateEntity.setStackId(stackId);
    rollingUpdateStateEntity.setLogicalResourceId(logicalResourceId);
    rollingUpdateStateEntity.setState(AWSAutoScalingAutoScalingGroupResourceAction.UpdateRollbackInfo.State.NOT_STARTED);
    try ( TransactionResource db =
            Entities.transactionFor( RollingUpdateStateEntity.class ) ) {
      Entities.persist(rollingUpdateStateEntity);
      db.commit( );
    }
    return rollingUpdateStateEntity;
  }

  public static RollingUpdateStateEntity updateRollingUpdateStateEntity(RollingUpdateStateEntity rollingUpdateStateEntity) {
    try ( TransactionResource db =
            Entities.transactionFor( RollingUpdateStateEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(RollingUpdateStateEntity.class)
        .add(Restrictions.eq("naturalId" , rollingUpdateStateEntity.getNaturalId()));
      RollingUpdateStateEntity dbEntity = (RollingUpdateStateEntity) criteria.uniqueResult();
      if (dbEntity == null) {
        Entities.persist(rollingUpdateStateEntity);
      } else {
        copy(rollingUpdateStateEntity, dbEntity);
      }
      db.commit( );
    }
    return rollingUpdateStateEntity;
  }
}
