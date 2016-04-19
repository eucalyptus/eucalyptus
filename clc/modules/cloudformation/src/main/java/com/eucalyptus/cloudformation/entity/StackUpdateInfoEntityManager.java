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
public class StackUpdateInfoEntityManager {


  public static StackUpdateInfoEntity getStackUpdateInfoEntity(String stackId, String accountId) throws CloudFormationException {
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateInfoEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackUpdateInfoEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId));
      List<StackUpdateInfoEntity> results = criteria.list();
      if (results == null || results.isEmpty()) return null;
      if (results.size() > 1) {
        throw new ValidationErrorException("More than one stack info record exists");
      } else {
        return results.get(0);
      }
    }
  }

  public static void setHasCalledRollbackStackState(String stackId, String accountId) throws CloudFormationException {
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateInfoEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackUpdateInfoEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId));
      List<StackUpdateInfoEntity> results = criteria.list();
      if (results == null || results.isEmpty()) {
        throw new ValidationErrorException("No stack info record exists");
      }
      if (results.size() > 1) {
        throw new ValidationErrorException("More than one stack info record exists");
      } else {
        results.get(0).setHasCalledRollbackStackState(true);
        db.commit();
      }
    }
  }

  public static void createUpdateInfo(String stackId, String accountId, String oldResourceDependencyManagerJson, String resourceDependencyManagerJson, Integer updatedStackVersion, String stackName, String accountAlias) {
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateInfoEntity.class ) ) {
      StackUpdateInfoEntity stackUpdateInfoEntity = new StackUpdateInfoEntity();
      stackUpdateInfoEntity.setStackId(stackId);
      stackUpdateInfoEntity.setAccountId(accountId);
      stackUpdateInfoEntity.setOldResourceDependencyManagerJson(oldResourceDependencyManagerJson);
      stackUpdateInfoEntity.setResourceDependencyManagerJson(resourceDependencyManagerJson);
      stackUpdateInfoEntity.setUpdatedStackVersion(updatedStackVersion);
      stackUpdateInfoEntity.setStackName(stackName);
      stackUpdateInfoEntity.setAccountAlias(accountAlias);
      Entities.persist(stackUpdateInfoEntity);
      db.commit( );
    }
  }

  private static void addRolledBackResourceWithStatus(String stackId, String accountId, String resourceId, StackUpdateInfoEntity.RolledBackResource.RollbackStatus status) throws ValidationErrorException {
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateInfoEntity.class ) ) {
      // First make sure there is an uodate info
      Criteria criteria = Entities.createCriteria(StackUpdateInfoEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId));
      List<StackUpdateInfoEntity> results = criteria.list();
      if (results == null || results.isEmpty()) {
        throw new ValidationErrorException("No record exists for this stack update info");
      }
      if (results.size() > 1) {
        throw new ValidationErrorException("More than one stack update info record exists");
      }

      // Now check the resource
      Criteria criteria2 = Entities.createCriteria(StackUpdateInfoEntity.RolledBackResource.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("resourceId", resourceId));
      List<StackUpdateInfoEntity.RolledBackResource> results2 = criteria2.list();
      if (results2 == null || results2.isEmpty()) {
        StackUpdateInfoEntity.RolledBackResource rolledBackResource = new StackUpdateInfoEntity.RolledBackResource();
        rolledBackResource.setResourceId(resourceId);
        rolledBackResource.setRollbackStatusValue(status);
        rolledBackResource.setAccountId(accountId);
        rolledBackResource.setStackId(stackId);
        Entities.persist(rolledBackResource);
      } else if (results2.size() > 1) {
        throw new ValidationErrorException("More than one resource record exists for resource " + resourceId + " in the stack update info");
      } else {
        StackUpdateInfoEntity.RolledBackResource rolledBackResource = results2.get(0);
        rolledBackResource.setRollbackStatusValue(status);
      }
      db.commit();
    }

  }
  public static void addRollbackCompletedResource(String stackId, String accountId, String completedResource) throws CloudFormationException {
    addRolledBackResourceWithStatus(stackId, accountId, completedResource, StackUpdateInfoEntity.RolledBackResource.RollbackStatus.COMPLETED);
  }

  public static void addRollbackStartedResource(String stackId, String accountId, String startedResource) throws CloudFormationException {
    addRolledBackResourceWithStatus(stackId, accountId, startedResource, StackUpdateInfoEntity.RolledBackResource.RollbackStatus.STARTED);
  }

  private static boolean isRolledBackResourceWithStatus(String stackId, String accountId, String resourceId, StackUpdateInfoEntity.RolledBackResource.RollbackStatus status) throws CloudFormationException {
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateInfoEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackUpdateInfoEntity.class)
        .add(Restrictions.eq("accountId", accountId))
        .add(Restrictions.eq("stackId", stackId));
      List<StackUpdateInfoEntity> results = criteria.list();
      if (results == null || results.isEmpty()) {
        throw new ValidationErrorException("No record exists for this stack update");
      }
      if (results.size() > 1) {
        throw new ValidationErrorException("More than one stack update info record exists");
      }

      // Now check the resource
      Criteria criteria2 = Entities.createCriteria(StackUpdateInfoEntity.RolledBackResource.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId))
        .add(Restrictions.eq("resourceId", resourceId));
      List<StackUpdateInfoEntity.RolledBackResource> results2 = criteria2.list();
      if (results2 == null || results2.isEmpty()) {
        return false;
      } else if (results2.size() > 1) {
        throw new ValidationErrorException("More than one rollback resource record exists for resource " + resourceId + " in the stack update rollback info");
      } else {
        StackUpdateInfoEntity.RolledBackResource rolledBackResource = results2.get(0);
        return rolledBackResource.getRollbackStatusValue() == status;
      }
    }
  }

  public static boolean isRollbackCompletedResource(String stackId, String accountId, String completedResource) throws CloudFormationException {
    return isRolledBackResourceWithStatus(stackId, accountId, completedResource, StackUpdateInfoEntity.RolledBackResource.RollbackStatus.COMPLETED);
  }

  public static boolean isRollbackStartedResource(String stackId, String accountId, String completedResource) throws CloudFormationException {
    return isRolledBackResourceWithStatus(stackId, accountId, completedResource, StackUpdateInfoEntity.RolledBackResource.RollbackStatus.STARTED);
  }

  public static void deleteStackUpdateInfo(String stackId, String accountId) {
    try ( TransactionResource db =
            Entities.transactionFor( StackUpdateInfoEntity.class ) ) {
      Criteria criteria = Entities.createCriteria(StackUpdateInfoEntity.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId));
      List<StackUpdateInfoEntity> results = criteria.list();
      if (results != null) {
        for (StackUpdateInfoEntity entity: results) {
          Entities.delete(entity);
        }
      }

      Criteria criteria2 = Entities.createCriteria(StackUpdateInfoEntity.RolledBackResource.class)
        .add(Restrictions.eq("accountId" , accountId))
        .add(Restrictions.eq("stackId" , stackId));
      List<StackUpdateInfoEntity.RolledBackResource> results2 = criteria2.list();
      if (results != null) {
        for (StackUpdateInfoEntity.RolledBackResource rolledBackResource : results2) {
          Entities.delete(rolledBackResource);
        }
      }

      db.commit();
    }
  }

  public static boolean hasNoUpdateInfoRecord(String stackId, String accountId) throws CloudFormationException {
    return getStackUpdateInfoEntity(stackId, accountId) == null;
  }
}
