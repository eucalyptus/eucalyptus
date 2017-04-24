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

import com.eucalyptus.entities.AbstractPersistent;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

/**
 * Created by ethomas on 10/4/14.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "stack_workflows" )
public class StackWorkflowEntity extends AbstractPersistent {

  @Column(name = "stack_id", nullable = false, length = 400 )
  String stackId;

  @Column(name = "domain", nullable = false )
  String domain;

  @Column(name = "workflow_id", nullable = false )
  String workflowId;

  @Column(name = "run_id", nullable = false )
  String runId;

  @Column(name = "workflow_type", nullable = false )
  @Enumerated(EnumType.STRING)
  WorkflowType workflowType;

  public enum WorkflowType {
    CREATE_STACK_WORKFLOW,
    UPDATE_STACK_WORKFLOW,
    DELETE_STACK_WORKFLOW,
    ROLLBACK_STACK_WORKFLOW,
    MONITOR_CREATE_STACK_WORKFLOW,
    MONITOR_UPDATE_STACK_WORKFLOW,
    MONITOR_DELETE_STACK_WORKFLOW,
    MONITOR_ROLLBACK_STACK_WORKFLOW,
    MONITOR_UPDATE_ROLLBACK_STACK_WORKFLOW,
    MONITOR_UPDATE_CLEANUP_STACK_WORKFLOW,
    MONITOR_UPDATE_ROLLBACK_CLEANUP_STACK_WORKFLOW,
    UPDATE_ROLLBACK_STACK_WORKFLOW,
    UPDATE_CLEANUP_STACK_WORKFLOW,
    UPDATE_ROLLBACK_CLEANUP_STACK_WORKFLOW
  }

  public StackWorkflowEntity() {
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getRunId() {
    return runId;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }

  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
  }
}
