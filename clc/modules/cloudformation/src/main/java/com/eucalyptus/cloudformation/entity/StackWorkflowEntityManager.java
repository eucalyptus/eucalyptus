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

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.InternalFailureException;
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

  public static StackWorkflowEntity addOrUpdateStackWorkflowEntity(String stackId, StackWorkflowEntity.WorkflowType workflowType, String domain, String workflowId, String runId) throws CloudFormationException {
    StackWorkflowEntity stackWorkflowEntity = new StackWorkflowEntity();
    stackWorkflowEntity.setDomain(domain);
    stackWorkflowEntity.setStackId(stackId);
    stackWorkflowEntity.setRunId(runId);
    stackWorkflowEntity.setWorkflowId(workflowId);
    stackWorkflowEntity.setWorkflowType(workflowType);
    try ( TransactionResource db =
            Entities.transactionFor(StackWorkflowEntity.class) ) {
      // Make sure one is not there yet.
      Criteria criteria = Entities.createCriteria(StackWorkflowEntity.class)
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("workflowType", workflowType));
      List<StackWorkflowEntity> match = criteria.list();
      if (match != null && match.size() > 1) {
        throw new InternalFailureException("More than one stack workflow entity of type " + workflowType + " already exists for stack id " + stackId);
      } else if (match != null && match.size() == 1) {
        match.get(0).setDomain(domain);
        match.get(0).setStackId(stackId);
        match.get(0).setRunId(runId);
        match.get(0).setWorkflowId(workflowId);
        match.get(0).setWorkflowType(workflowType);
      } else {
        Entities.persist(stackWorkflowEntity);
      }
      // do something
      db.commit( );
    }
    return stackWorkflowEntity;
  }

  public static List<StackWorkflowEntity> getStackWorkflowEntities(String stackId, StackWorkflowEntity.WorkflowType workflowType) {
    List<StackWorkflowEntity> workflows = Lists.newArrayList();
    try (TransactionResource db =
           Entities.transactionFor(StackWorkflowEntity.class)) {
      Criteria criteria = Entities.createCriteria(StackWorkflowEntity.class)
        .add(Restrictions.eq("stackId", stackId))
        .add(Restrictions.eq("workflowType", workflowType));
      List<StackWorkflowEntity> match = criteria.list();
      if (match != null) {
        workflows.addAll(match);
      }
    }
    return workflows;
  }

  public static List<StackWorkflowEntity> getStackWorkflowEntities(String stackId) {
    List<StackWorkflowEntity> workflows = Lists.newArrayList();
    try (TransactionResource db =
           Entities.transactionFor(StackWorkflowEntity.class)) {
      Criteria criteria = Entities.createCriteria(StackWorkflowEntity.class)
        .add(Restrictions.eq("stackId", stackId));
      List<StackWorkflowEntity> match = criteria.list();
      if (match != null) {
        workflows.addAll(match);
      }
    }
    return workflows;
  }

  // this one is not soft delete
  public static void deleteStackWorkflowEntities(String stackId) {
    try (TransactionResource db =
           Entities.transactionFor(StackWorkflowEntity.class)) {
      Criteria criteria = Entities.createCriteria(StackWorkflowEntity.class)
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
