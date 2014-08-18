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
package com.eucalyptus.simpleworkflow;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata;
import com.eucalyptus.util.OwnerFullName;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_simpleworkflow" )
@Table( name = "swf_timer" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class Timer extends AbstractOwnedPersistent implements SimpleWorkflowMetadata.ActivityTaskMetadata {
  private static final long serialVersionUID = 1L;

  @ManyToOne
  @JoinColumn( name = "workflow_execution_id", nullable = false, updatable = false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private WorkflowExecution workflowExecution;

  @Column( name = "domain", length = 256, nullable = false, updatable = false )
  private String domain;

  @Column( name = "workflow_run_id", nullable = false, updatable = false )
  private String workflowRunId;

  @Column( name = "decision_task_comp_event_id", nullable = false, updatable = false )
  private Long decisionTaskCompletedEventId;

  @Column( name = "started_event_id", nullable = false, updatable = false )
  private Long startedEventId;

  @Column( name = "control", updatable = false )
  @Lob
  @Type( type = "org.hibernate.type.StringClobType" )
  private String control;

  @Column( name = "start_to_fire_timeout", nullable = false, updatable = false )
  private Integer startToFireTimeout;

  @Column( name = "timeout_timestamp", nullable = false, updatable = false )
  @Temporal( TemporalType.TIMESTAMP )
  private Date timeoutTimestamp;

  protected Timer() {
  }

  protected Timer( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static Timer create( final OwnerFullName ownerFullName,
                              final WorkflowExecution workflowExecution,
                              final String domain,
                              final String timerId,
                              final String control,
                              final Integer startToFireTimeout,
                              final Long decisionTaskCompletedEventId,
                              final Long startedEventId ) {
    final Timer timer = new Timer( ownerFullName, timerId );
    timer.setWorkflowExecution( workflowExecution );
    timer.setDomain( domain );
    timer.setWorkflowRunId( workflowExecution.getDisplayName() );
    timer.setControl( control );
    timer.setStartToFireTimeout( startToFireTimeout );
    timer.setDecisionTaskCompletedEventId( decisionTaskCompletedEventId );
    timer.setStartedEventId( startedEventId );
    return timer;
  }

  public static Timer exampleWithOwner( final OwnerFullName owner ) {
    return new Timer( owner, null );
  }

  public static Timer exampleWithTimerId( final OwnerFullName owner,
                                          final String domainName,
                                          final String runId,
                                          final String timerId ) {
    final Timer timer = new Timer( owner, timerId );
    timer.setDomain( domainName );
    timer.setWorkflowRunId( runId );
    return timer;
  }

  public static Timer exampleWithWorkflowExecution( final OwnerFullName owner,
                                                    final String domainName,
                                                    final String runId ) {
    final Timer timer = new Timer( owner, null );
    timer.setDomain( domainName );
    timer.setWorkflowRunId( runId );
    return timer;
  }

  public static Timer exampleWithUniqueName( final OwnerFullName owner,
                                             final String runId,
                                             final String timerId ) {
    final Timer timer = new Timer( owner, null );
    timer.setUniqueName( createUniqueName( owner.getAccountNumber(), runId, timerId ) );
    return timer;
  }

  private static String createUniqueName( final String accountNumber,
                                          final String runId,
                                          final String timerId ) {
    return accountNumber + ":" + runId + ":" + timerId;
  }

  @Override
  protected String createUniqueName() {
    return createUniqueName( getOwnerAccountNumber(), getWorkflowExecution().getDisplayName(), getDisplayName() );
  }

  public Date calculateTimeout() {
    final Long timeout = toTimeout( getCreationTimestamp( ), startToFireTimeout );
    return timeout == Long.MAX_VALUE ?
        null :
        new Date( timeout );
  }

  private static Long toTimeout( final Date from, final Integer period ) {
    return period == null ?
        null :
        from.getTime() + TimeUnit.SECONDS.toMillis( period );
  }

  public WorkflowExecution getWorkflowExecution() {
    return workflowExecution;
  }

  public void setWorkflowExecution( final WorkflowExecution workflowExecution ) {
    this.workflowExecution = workflowExecution;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain( final String domain ) {
    this.domain = domain;
  }

  public String getWorkflowRunId() {
    return workflowRunId;
  }

  public void setWorkflowRunId( final String workflowRunId ) {
    this.workflowRunId = workflowRunId;
  }

  public Long getDecisionTaskCompletedEventId() {
    return decisionTaskCompletedEventId;
  }

  public void setDecisionTaskCompletedEventId( final Long decisionTaskCompletedEventId ) {
    this.decisionTaskCompletedEventId = decisionTaskCompletedEventId;
  }

  public Long getStartedEventId() {
    return startedEventId;
  }

  public void setStartedEventId( final Long startedEventId ) {
    this.startedEventId = startedEventId;
  }

  public String getControl() {
    return control;
  }

  public void setControl( final String control ) {
    this.control = control;
  }

  public Integer getStartToFireTimeout() {
    return startToFireTimeout;
  }

  public void setStartToFireTimeout( final Integer startToFireTimeout ) {
    this.startToFireTimeout = startToFireTimeout;
  }

  public Date getTimeoutTimestamp() {
    return timeoutTimestamp;
  }

  public void setTimeoutTimestamp( final Date timeoutTimestamp ) {
    this.timeoutTimestamp = timeoutTimestamp;
  }

  @PreUpdate
  @PrePersist
  protected void updateTimeout( ) {
    updateTimeStamps( );
    setTimeoutTimestamp( calculateTimeout( ) );
  }
}
