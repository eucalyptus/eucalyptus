package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.entities.AbstractPersistent;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StackWorkflowEntity extends AbstractPersistent {

  @Column(name = "stack_id", nullable = false )
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
    DELETE_STACK_WORKFLOW,
    MONITOR_CREATE_STACK_WORKFLOW
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
