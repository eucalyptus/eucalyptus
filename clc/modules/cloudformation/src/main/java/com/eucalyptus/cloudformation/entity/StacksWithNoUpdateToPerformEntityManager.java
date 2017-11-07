/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Created by ethomas on 4/4/16.
 */
public class StacksWithNoUpdateToPerformEntityManager {

  public static boolean isStackWithNoUpdateToPerform(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StacksWithNoUpdateToPerformEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StacksWithNoUpdateToPerformEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId));
      List<StacksWithNoUpdateToPerformEntity> results = criteria.list();
      return (results != null && !results.isEmpty());
    }
  }


  public static void addStackWithNoUpdateToPerform(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StacksWithNoUpdateToPerformEntity.class ) ) {
      StacksWithNoUpdateToPerformEntity stacksWithNoUpdateToPerformEntity = new StacksWithNoUpdateToPerformEntity();
      stacksWithNoUpdateToPerformEntity.setStackId(stackId);
      stacksWithNoUpdateToPerformEntity.setAccountId(accountId);
      Entities.persist(stacksWithNoUpdateToPerformEntity);
      db.commit( );
    }
  }

  public static void deleteStackWithNoUpdateToPerform(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StacksWithNoUpdateToPerformEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StacksWithNoUpdateToPerformEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId));
      List<StacksWithNoUpdateToPerformEntity> results = criteria.list();
      if (results != null) {
        for (StacksWithNoUpdateToPerformEntity entity: results) {
          Entities.delete(entity);
        }
      }
      db.commit();
    }
  }
}
