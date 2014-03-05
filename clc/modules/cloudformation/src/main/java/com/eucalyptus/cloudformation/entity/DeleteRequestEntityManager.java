package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Lists;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Created by ethomas on 3/3/14.
 */
public class DeleteRequestEntityManager {

  public void addDeleteRequest(String stackName, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( DeleteRequestEntity.class ) ) {
      DeleteRequestEntity deleteRequestEntity = new DeleteRequestEntity();
      deleteRequestEntity.setAccountId(accountId);
      deleteRequestEntity.setStackName(stackName);
      deleteRequestEntity.setRecordDeleted(Boolean.FALSE);
      Entities.persist(deleteRequestEntity);
      db.commit( );
    }
  }

  public List<DeleteRequestEntity> getDeleteRequests(String stackName, String accountId) {
    List<DeleteRequestEntity> returnList = Lists.newArrayList();
    try ( TransactionResource db =
            Entities.transactionFor(DeleteRequestEntity.class) ) {
      Criteria criteria = Entities.createCriteria(DeleteRequestEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq( "stackName" , stackName))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));

      List<DeleteRequestEntity> entityList = criteria.list();
      for (DeleteRequestEntity deleteRequestEntity: entityList) {
        returnList.add(deleteRequestEntity);
      }
      db.commit( );
    }
    return returnList;
  }

  public void removeDeleteRequests(String stackName, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor(DeleteRequestEntity.class) ) {
      Criteria criteria = Entities.createCriteria(DeleteRequestEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq( "stackName" , stackName))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));

      java.util.List<DeleteRequestEntity> entityList = criteria.list();
      for (DeleteRequestEntity deleteRequestEntity: entityList) {
        deleteRequestEntity.setRecordDeleted(Boolean.TRUE);
      }
      db.commit( );
    }

  }
}
