package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Created by ethomas on 2/18/14.
 */
public class TemplateEntityManager {

  public static String getTemplateJson(String stackId, String accountId) {
    String templateJson = null;
    try ( TransactionResource db =
            Entities.transactionFor(TemplateEntity.class) ) {
      Criteria criteria = Entities.createCriteria(TemplateEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<TemplateEntity> entityList = criteria.list();
      if (entityList != null && !entityList.isEmpty()) {
        templateJson = entityList.get(0).getTemplateJson();
      }
      db.commit( );
    }
    return templateJson;
  }

  public static void addOrUpdateTemplateJson(String stackId, String accountId, String templateJson) {
    try ( TransactionResource db =
            Entities.transactionFor(TemplateEntity.class) ) {
      Criteria criteria = Entities.createCriteria(TemplateEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<TemplateEntity> entityList = criteria.list();
      if (entityList != null && !entityList.isEmpty()) {
        for (TemplateEntity templateEntity: entityList) {
          templateEntity.setTemplateJson(templateJson);
        }
      } else {
        TemplateEntity templateEntity = new TemplateEntity();
        templateEntity.setTemplateJson(templateJson);
        templateEntity.setAccountId(accountId);
        templateEntity.setStackId(stackId);
        templateEntity.setRecordDeleted(Boolean.FALSE);
        Entities.persist(templateEntity);
      }
      db.commit();
    }
 }

  public static void deleteTemplateJson(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor(TemplateEntity.class) ) {
      Criteria criteria = Entities.createCriteria(TemplateEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("recordDeleted", Boolean.FALSE));
      List<TemplateEntity> entityList = criteria.list();
      for (TemplateEntity templateEntity: entityList) {
        templateEntity.setRecordDeleted(Boolean.TRUE);
      }
      db.commit();
    }
  }


}
