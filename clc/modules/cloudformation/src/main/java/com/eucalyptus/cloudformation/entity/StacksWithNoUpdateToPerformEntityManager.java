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
