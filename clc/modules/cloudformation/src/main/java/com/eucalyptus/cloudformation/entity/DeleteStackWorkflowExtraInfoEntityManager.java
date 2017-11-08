/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
public class DeleteStackWorkflowExtraInfoEntityManager {

  public static DeleteStackWorkflowExtraInfoEntity addOrUpdateExtraInfo(String stackId, String domain, String workflowId, String runId, String retainedResourcesStr) throws CloudFormationException {
    DeleteStackWorkflowExtraInfoEntity deleteStackWorkflowExtraInfoEntity = new DeleteStackWorkflowExtraInfoEntity();
    deleteStackWorkflowExtraInfoEntity.setDomain(domain);
    deleteStackWorkflowExtraInfoEntity.setStackId(stackId);
    deleteStackWorkflowExtraInfoEntity.setRunId(runId);
    deleteStackWorkflowExtraInfoEntity.setWorkflowId(workflowId);
    deleteStackWorkflowExtraInfoEntity.setRetainedResourcesStr(retainedResourcesStr);
    try ( TransactionResource db =
            Entities.transactionFor(DeleteStackWorkflowExtraInfoEntity.class) ) {
      // Make sure one is not there yet.
      Criteria criteria = Entities.createCriteria(DeleteStackWorkflowExtraInfoEntity.class)
        .add(Restrictions.eq("stackId", stackId));
      List<DeleteStackWorkflowExtraInfoEntity> match = criteria.list();
      if (match != null && match.size() > 1) {
        throw new InternalFailureException("More than one delete stack workflow extra info entity already exists for stack id " + stackId);
      } else if (match != null && match.size() == 1) {
        match.get(0).setDomain(domain);
        match.get(0).setStackId(stackId);
        match.get(0).setRunId(runId);
        match.get(0).setWorkflowId(workflowId);
        match.get(0).setRetainedResourcesStr(retainedResourcesStr);
      } else {
        Entities.persist(deleteStackWorkflowExtraInfoEntity);
      }
      // do something
      db.commit( );
    }
    return deleteStackWorkflowExtraInfoEntity;
  }

  public static List<DeleteStackWorkflowExtraInfoEntity> getExtraInfoEntities(String stackId) {
    List<DeleteStackWorkflowExtraInfoEntity> workflows = Lists.newArrayList();
    try (TransactionResource db =
           Entities.transactionFor(DeleteStackWorkflowExtraInfoEntity.class)) {
      Criteria criteria = Entities.createCriteria(DeleteStackWorkflowExtraInfoEntity.class)
        .add(Restrictions.eq("stackId", stackId));
      List<DeleteStackWorkflowExtraInfoEntity> match = criteria.list();
      if (match != null) {
        workflows.addAll(match);
      }
    }
    return workflows;
  }

  // this one is not soft delete
  public static void deleteExtraInfoEntities(String stackId) {
    try (TransactionResource db =
           Entities.transactionFor(DeleteStackWorkflowExtraInfoEntity.class)) {
      Criteria criteria = Entities.createCriteria(DeleteStackWorkflowExtraInfoEntity.class)
        .add(Restrictions.eq("stackId", stackId));
      List<DeleteStackWorkflowExtraInfoEntity> match = criteria.list();
      if (match != null) {
        for (DeleteStackWorkflowExtraInfoEntity deleteStackWorkflowExtraInfoEntity: match) {
          Entities.delete(deleteStackWorkflowExtraInfoEntity);
        }
      }
    }
  }
}
