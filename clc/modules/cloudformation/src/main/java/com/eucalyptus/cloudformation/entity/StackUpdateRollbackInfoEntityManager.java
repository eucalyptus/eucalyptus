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
public class StackUpdateRollbackInfoEntityManager {


  public static StackUpdateRollbackInfoEntity getStackUpdateRollbackInfoEntity(String stackId, String accountId) throws CloudFormationException {
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateRollbackInfoEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackUpdateRollbackInfoEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId));
      List<StackUpdateRollbackInfoEntity> results = criteria.list();
      if (results == null || results.isEmpty()) return null;
      if (results.size() > 1) {
        throw new ValidationErrorException("More than one stack update rollback info record exists");
      } else {
        return results.get(0);
      }
    }
  }

  public static void createUpdateRollbackInfo(String stackId, String accountId, String oldResourceDependencyManagerJson, String resourceDependencyManagerJson, Integer rolledBackStackVersion) {
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateRollbackInfoEntity.class ) ) {
      StackUpdateRollbackInfoEntity stackUpdateRollbackInfoEntity = new StackUpdateRollbackInfoEntity();
      stackUpdateRollbackInfoEntity.setStackId(stackId);
      stackUpdateRollbackInfoEntity.setAccountId(accountId);
      stackUpdateRollbackInfoEntity.setOldResourceDependencyManagerJson(oldResourceDependencyManagerJson);
      stackUpdateRollbackInfoEntity.setResourceDependencyManagerJson(resourceDependencyManagerJson);
      stackUpdateRollbackInfoEntity.setRolledBackStackVersion(rolledBackStackVersion);
      Entities.persist(stackUpdateRollbackInfoEntity);
      db.commit( );
    }
  }

  private static void addResourceWithStatus(String stackId, String accountId, String resourceId, StackUpdateRollbackInfoEntity.Resource.RollbackStatus status) throws ValidationErrorException {
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateRollbackInfoEntity.class ) ) {
      // First make sure there is a rollback info
      Criteria criteria = Entities.createCriteria(StackUpdateRollbackInfoEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId));
      List<StackUpdateRollbackInfoEntity> results = criteria.list();
      if (results == null || results.isEmpty()) {
        throw new ValidationErrorException("No record exists for this stack update rollback");
      }
      if (results.size() > 1) {
        throw new ValidationErrorException("More than one stack update rollback info record exists");
      }

      // Now check the resource
      Criteria criteria2 = Entities.createCriteria(StackUpdateRollbackInfoEntity.Resource.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("resourceId", resourceId));
      List<StackUpdateRollbackInfoEntity.Resource> results2 = criteria2.list();
      if (results2 == null || results2.isEmpty()) {
        StackUpdateRollbackInfoEntity.Resource resource = new StackUpdateRollbackInfoEntity.Resource();
        resource.setResourceId(resourceId);
        resource.setRollbackStatusValue(status);
        resource.setAccountId(accountId);
        resource.setStackId(stackId);
        Entities.persist(resource);
      } else if (results2.size() > 1) {
        throw new ValidationErrorException("More than one resource record exists for resource " + resourceId + " in the stack update rollback info");
      } else {
        StackUpdateRollbackInfoEntity.Resource resource = results2.get(0);
        resource.setRollbackStatusValue(status);
      }
      db.commit();
    }

  }
  public static void addCompletedResource(String stackId, String accountId, String completedResource) throws CloudFormationException {
    addResourceWithStatus(stackId, accountId, completedResource, StackUpdateRollbackInfoEntity.Resource.RollbackStatus.COMPLETED);
  }

  public static void addStartedResource(String stackId, String accountId, String startedResource) throws CloudFormationException {
    addResourceWithStatus(stackId, accountId, startedResource, StackUpdateRollbackInfoEntity.Resource.RollbackStatus.STARTED);
  }

  private static boolean isResourceWithStatus(String stackId, String accountId, String resourceId, StackUpdateRollbackInfoEntity.Resource.RollbackStatus status) throws CloudFormationException {
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateRollbackInfoEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackUpdateRollbackInfoEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackId", stackId));
      // TODO: maybe a join?
      List<StackUpdateRollbackInfoEntity> results = criteria.list();
      if (results == null || results.isEmpty()) {
        throw new ValidationErrorException("No record exists for this stack update rollback");
      }
      if (results.size() > 1) {
        throw new ValidationErrorException("More than one stack update rollback info record exists");
      }

      // Now check the resource
      Criteria criteria2 = Entities.createCriteria(StackUpdateRollbackInfoEntity.Resource.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("resourceId", resourceId));
      List<StackUpdateRollbackInfoEntity.Resource> results2 = criteria2.list();
      if (results2 == null || results2.isEmpty()) {
        return false;
      } else if (results2.size() > 1) {
        throw new ValidationErrorException("More than one resource record exists for resource " + resourceId + " in the stack update rollback info");
      } else {
        StackUpdateRollbackInfoEntity.Resource resource = results2.get(0);
        return resource.getRollbackStatusValue() == status;
      }
    }
  }

  public static boolean isCompletedResource(String stackId, String accountId, String completedResource) throws CloudFormationException {
    return isResourceWithStatus(stackId, accountId, completedResource, StackUpdateRollbackInfoEntity.Resource.RollbackStatus.COMPLETED);
  }

  public static boolean isStartedResource(String stackId, String accountId, String completedResource) throws CloudFormationException {
    return isResourceWithStatus(stackId, accountId, completedResource, StackUpdateRollbackInfoEntity.Resource.RollbackStatus.STARTED);
  }

  public static void deleteStackUpdateRollbackInfo(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateRollbackInfoEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackUpdateRollbackInfoEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId));
      List<StackUpdateRollbackInfoEntity> results = criteria.list();
      if (results != null) {
        for (StackUpdateRollbackInfoEntity entity: results) {
          Entities.delete(entity);
        }
      }

      Criteria criteria2 = Entities.createCriteria(StackUpdateRollbackInfoEntity.Resource.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId));
      List<StackUpdateRollbackInfoEntity.Resource> results2 = criteria2.list();
      if (results != null) {
        for (StackUpdateRollbackInfoEntity.Resource resource: results2) {
          Entities.delete(resource);
        }
      }

      db.commit();
    }
  }
}
