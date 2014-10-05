package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.google.common.collect.Lists;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Created by ethomas on 10/4/14.
 */
public class StackWorkflowEntityManager {

  public StackWorkflowEntity createStackWorkflow(String stackId, StackWorkflowEntity.WorkflowType workflowType, String domain, String workflowId, String runId) {
    StackWorkflowEntity stackWorkflowEntity = new StackWorkflowEntity();
    stackWorkflowEntity.setDomain(domain);
    stackWorkflowEntity.setStackId(stackId);
    stackWorkflowEntity.setRunId(runId);
    stackWorkflowEntity.setWorkflowId(workflowId);
    stackWorkflowEntity.setWorkflowType(workflowType);
    try ( TransactionResource db =
            Entities.transactionFor(StackWorkflowEntity.class) ) {
      Entities.persist(stackWorkflowEntity);
      // do something
      db.commit( );
    }
    return stackWorkflowEntity;
  }

  public List<StackWorkflowEntity> getStackWorkflowEntities(String stackId, StackWorkflowEntity.WorkflowType workflowType) {
    List<StackWorkflowEntity> workflows = Lists.newArrayList();
    try (TransactionResource db =
           Entities.transactionFor(StackWorkflowEntity.class)) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("workflowType", workflowType));
      List<StackWorkflowEntity> match = criteria.list();
      if (match != null) {
        workflows.addAll(match);
      }
    }
    return workflows;
  }

  public List<StackWorkflowEntity> getStackWorkflowEntities(String stackId) {
    List<StackWorkflowEntity> workflows = Lists.newArrayList();
    try (TransactionResource db =
           Entities.transactionFor(StackWorkflowEntity.class)) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("stackId", stackId));
      List<StackWorkflowEntity> match = criteria.list();
      if (match != null) {
        workflows.addAll(match);
      }
    }
    return workflows;
  }

  // this one is not soft delete
  public void deleteStackWorkflowEntities(String stackId) {
    try (TransactionResource db =
           Entities.transactionFor(StackWorkflowEntity.class)) {
      Criteria criteria = Entities.createCriteria(StackEntity.class)
        .add(Restrictions.eq("stackId", stackId));
      List<StackWorkflowEntity> match = criteria.list();
      if (match != null) {
        for (StackWorkflowEntity stackWorkflowEntity: match) {
          Entities.delete(stackWorkflowEntity);
        }
      }
    }
  }
}
