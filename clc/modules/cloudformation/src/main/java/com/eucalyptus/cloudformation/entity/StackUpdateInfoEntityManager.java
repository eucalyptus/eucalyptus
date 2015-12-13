package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.cloudformation.AlreadyExistsException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Created by ethomas on 12/10/15.
 */
public class StackUpdateInfoEntityManager {

  public static StackUpdateInfoEntity addStackUpdateInfo(StackUpdateInfoEntity stackUpdateInfoEntity) throws AlreadyExistsException {
    try ( TransactionResource db =
            Entities.transactionFor(StackUpdateInfoEntity.class) ) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("stackName", stackUpdateInfoEntity.getStackName()))
        .add(Restrictions.eq("accountId", stackUpdateInfoEntity.getAccountId()))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackEntity> EntityList = criteria.list();
      if (!EntityList.isEmpty()) {
        throw new AlreadyExistsException("Stack already exists");
      }
      Entities.persist(stackUpdateInfoEntity);
      // do something
      db.commit( );
    }
    return stackUpdateInfoEntity;
  }

  public static StackUpdateInfoEntity getStackUpdateInfo(String stackId, String accountId) {
    StackUpdateInfoEntity stackUpdateInfoEntity = null;
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateInfoEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackUpdateInfoEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<StackUpdateInfoEntity> stackUpdateInfoEntityList = criteria.list();
      if (stackUpdateInfoEntityList != null && !stackUpdateInfoEntityList.isEmpty()) {
        stackUpdateInfoEntity = stackUpdateInfoEntityList.get(0);
      }
      db.commit( );
    }
    return stackUpdateInfoEntity;
  }

  public static void deleteStackUpdateInfo(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateInfoEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackUpdateInfoEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq( "stackId" , stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));

      List<StackUpdateInfoEntity> entityList = criteria.list();
      for (StackUpdateInfoEntity stackUpdateInfoEntity: entityList) {
        stackUpdateInfoEntity.setRecordDeleted(Boolean.TRUE);
      }
      db.commit( );
    }
  }

}
