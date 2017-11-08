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
    MONITOR_CREATE_STACK_WORKFLOW,
    MONITOR_UPDATE_STACK_WORKFLOW,
    UPDATE_ROLLBACK_STACK_WORKFLOW,
    UPDATE_CLEANUP_STACK_WORKFLOW,
    UPDATE_ROLLBACK_CLEANUP_STACK_WORKFLOW,
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
