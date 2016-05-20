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
    destEntity.setObsoleteInstancesJson(sourceEntity.getObsoleteInstancesJson());
    destEntity.setAlreadySuspendedProcessNames(sourceEntity.getAlreadySuspendedProcessNames());
    destEntity.setNumSuccessSignals(sourceEntity.getNumSuccessSignals());
    destEntity.setNumFailureSignals(sourceEntity.getNumFailureSignals());
    destEntity.setNumNeededSignalsThisBatch(sourceEntity.getNumNeededSignalsThisBatch());
    destEntity.setNumReceivedSignalsThisBatch(sourceEntity.getNumReceivedSignalsThisBatch());
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
