/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 * Created by ethomas on 4/9/16.
 */
public class StackUpdateWorkflowSignalEntityManager {

  public static Collection<StackUpdateWorkflowSignalEntity> getSignals(String stackId, String accountId, String outerStackArn) {
    List<StackUpdateWorkflowSignalEntity> signals = Lists.newArrayList();
    try (TransactionResource db =
           Entities.transactionFor(StackUpdateWorkflowSignalEntity.class)) {
      Criteria criteria = Entities.createCriteria(StackUpdateWorkflowSignalEntity.class)
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("outerStackArn", outerStackArn));
      List<StackUpdateWorkflowSignalEntity> results = criteria.list();
      if (results != null) {
        signals.addAll(results);
      }
    }
    return signals;
  }

  public static void addSignal(String stackId, String accountId, String outerStackArn, StackUpdateWorkflowSignalEntity.Signal signal) {
    try (TransactionResource db =
           Entities.transactionFor(StackUpdateWorkflowSignalEntity.class)) {
      StackUpdateWorkflowSignalEntity stackUpdateWorkflowSignalEntity = new StackUpdateWorkflowSignalEntity();
      stackUpdateWorkflowSignalEntity.setAccountId(accountId);
      stackUpdateWorkflowSignalEntity.setStackId(stackId);
      stackUpdateWorkflowSignalEntity.setOuterStackArn(outerStackArn);
      stackUpdateWorkflowSignalEntity.setSignal(signal);
      Entities.persist(stackUpdateWorkflowSignalEntity);
      db.commit();
    }
  }

  public static void deleteSignal(String stackId, String accountId, String outerStackArn, StackUpdateWorkflowSignalEntity.Signal signal) {
    List<StackUpdateWorkflowSignalEntity> signals = Lists.newArrayList();
    try (TransactionResource db =
           Entities.transactionFor(StackUpdateWorkflowSignalEntity.class)) {
      Criteria criteria = Entities.createCriteria(StackUpdateWorkflowSignalEntity.class)
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("outerStackArn", outerStackArn))
        .add(Restrictions.eq("signal", signal));
      List<StackUpdateWorkflowSignalEntity> results = criteria.list();
      if (results != null) {
        for (StackUpdateWorkflowSignalEntity result : results) {
          Entities.delete(result);
        }
      }
      db.commit();
    }
  }

  public static void deleteSignals(String stackId, String accountId, String outerStackArn) {
    List<StackUpdateWorkflowSignalEntity> signals = Lists.newArrayList();
    try (TransactionResource db =
           Entities.transactionFor(StackUpdateWorkflowSignalEntity.class)) {
      Criteria criteria = Entities.createCriteria(StackUpdateWorkflowSignalEntity.class)
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("outerStackArn", outerStackArn));
      List<StackUpdateWorkflowSignalEntity> results = criteria.list();
      if (results != null) {
        for (StackUpdateWorkflowSignalEntity result : results) {
          Entities.delete(result);
        }
      }
      db.commit();
    }
  }

  public static boolean hasSignal(String stackId, String accountId, String outerStackArn, StackUpdateWorkflowSignalEntity.Signal signal) {
    List<StackUpdateWorkflowSignalEntity> signals = Lists.newArrayList();
    try (TransactionResource db =
           Entities.transactionFor(StackUpdateWorkflowSignalEntity.class)) {
      Criteria criteria = Entities.createCriteria(StackUpdateWorkflowSignalEntity.class)
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("outerStackArn", outerStackArn))
        .add(Restrictions.eq("signal", signal));
      List<StackUpdateWorkflowSignalEntity> results = criteria.list();
      if (results != null) {
        signals.addAll(results);
      }
    }
    return !signals.isEmpty();
  }

}
