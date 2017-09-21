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
package com.eucalyptus.simpleworkflow;

import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.ActivityTaskMetadata;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.annotations.Type;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Pair;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_simpleworkflow" )
@Table( name = "swf_activity_task" )
public class ActivityTask extends AbstractOwnedPersistent implements ActivityTaskMetadata {
  private static final long serialVersionUID = 1L;

  public enum State {
    Pending,
    Active,
  }

  @ManyToOne
  @JoinColumn( name = "workflow_execution_id", nullable = false, updatable = false )
  private WorkflowExecution workflowExecution;

  @Column( name = "state", nullable = false )
  @Enumerated( EnumType.STRING )
  private State state;

  @Column( name = "domain", length = 256, nullable = false, updatable = false )
  private String domain;

  @Column( name = "domain_uuid", nullable = false, updatable = false )
  private String domainUuid;

  @Column( name = "workflow_run_id", nullable = false, updatable = false )
  private String workflowRunId;

  @Column( name = "task_list", length = 256, nullable = false, updatable = false  )
  private String taskList;

  @Column( name = "scheduled_event_id", nullable = false, updatable = false  )
  private Long scheduledEventId;

  @Column( name = "started_event_id" )
  private Long startedEventId;

  @Column( name = "cancel_requested_event_id" )
  private Long cancelRequestedEventId;

  @Column( name = "activity_type", length = 256, nullable = false, updatable = false  )
  private String activityType;

  @Column( name = "activity_version", length = 64, nullable = false, updatable = false  )
  private String activityVersion;

  @Column( name = "input", updatable = false  )
  @Type(type="text")
  private String input;

  @Column( name = "schedule_to_close_timeout", updatable = false )
  private Integer scheduleToCloseTimeout;

  @Column( name = "schedule_to_start_timeout", updatable = false )
  private Integer scheduleToStartTimeout;

  @Column( name = "start_to_close_timeout", updatable = false )
  private Integer startToCloseTimeout;

  @Column( name = "heartbeat_timeout", updatable = false )
  private Integer heartbeatTimeout;

  @Column( name = "heartbeat_details", updatable = false )
  @Type(type="text")
  private String heartbeatDetails;

  @Column( name = "started_timestamp" )
  @Temporal( TemporalType.TIMESTAMP )
  private Date startedTimestamp;

  @Column( name = "timeout_timestamp" )
  @Temporal( TemporalType.TIMESTAMP )
  private Date timeoutTimestamp;

  protected ActivityTask( ) {
  }

  protected ActivityTask( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static ActivityTask create( final OwnerFullName ownerFullName,
                                     final WorkflowExecution workflowExecution,
                                     final String domain,
                                     final String domainUuid,
                                     final String activityId,
                                     final String activityType,
                                     final String activityVersion,
                                     final String input,
                                     final Long scheduledEventId,
                                     final String taskList,
                                     final Integer scheduleToCloseTimeout,
                                     final Integer scheduleToStartTimeout,
                                     final Integer startToCloseTimeout,
                                     final Integer heartbeatTimeout ) {
    final ActivityTask activityTask = new ActivityTask( ownerFullName, activityId );
    activityTask.setWorkflowExecution( workflowExecution );
    activityTask.setDomain( domain );
    activityTask.setDomainUuid( domainUuid );
    activityTask.setWorkflowRunId( workflowExecution.getDisplayName( ) );
    activityTask.setActivityType( activityType );
    activityTask.setActivityVersion( activityVersion );
    activityTask.setInput( input );
    activityTask.setScheduledEventId( scheduledEventId );
    activityTask.setStartedEventId( 0L );
    activityTask.setTaskList( taskList );
    activityTask.setScheduleToCloseTimeout( scheduleToCloseTimeout );
    activityTask.setScheduleToStartTimeout( scheduleToStartTimeout );
    activityTask.setStartToCloseTimeout( startToCloseTimeout );
    activityTask.setHeartbeatTimeout( heartbeatTimeout );
    activityTask.setState( State.Pending );
    return activityTask;
  }

  public static ActivityTask exampleWithOwner( final OwnerFullName owner ) {
    return new ActivityTask( owner, null );
  }

  public static ActivityTask exampleWithActivityId( final OwnerFullName owner,
                                                    final String domainName,
                                                    final String runId,
                                                    final String name ) {
    final ActivityTask activityTask = new ActivityTask( owner, name );
    activityTask.setDomain( domainName );
    activityTask.setWorkflowRunId( runId );
    return activityTask;
  }

  public static ActivityTask exampleWithWorkflowExecution( final OwnerFullName owner,
                                                           final String domainName,
                                                           final String runId ) {
    final ActivityTask activityTask = new ActivityTask( owner, null );
    activityTask.setDomain( domainName );
    activityTask.setWorkflowRunId( runId );
    return activityTask;
  }

  public static ActivityTask exampleWithUniqueName( final OwnerFullName owner,
                                                    final String runId,
                                                    final Long scheduledEventId ) {
    final ActivityTask activityTask = new ActivityTask( owner, null );
    activityTask.setUniqueName( createUniqueName( owner.getAccountNumber(), runId, scheduledEventId ) );
    return activityTask;
  }

  public static ActivityTask examplePending( final OwnerFullName ownerFullName,
                                             final String domain,
                                             final String taskList ) {
    final ActivityTask example = new ActivityTask( ownerFullName, null );
    example.setDomain( domain );
    example.setTaskList( taskList );
    example.setState( State.Pending );
    return example;
  }

  private static String createUniqueName( final String accountNumber,
                                          final String runId,
                                          final Long scheduledEventId ) {
    return accountNumber + ":" + runId + ":" + scheduledEventId;
  }

  @Override
  protected String createUniqueName( ) {
    return createUniqueName( getOwnerAccountNumber( ), getWorkflowExecution( ).getDisplayName( ), getScheduledEventId( ) );
  }

  public Pair<String,Date> calculateNextTimeout( ) {
    @SuppressWarnings( "unchecked" )
    final Iterable<Pair<String,Optional<Long>>> taggedTimeouts = Iterables.filter( Lists.newArrayList(
        Pair.ropair( "SCHEDULE_TO_CLOSE", toTimeout( getCreationTimestamp( ), getScheduleToCloseTimeout( ) ) ),
        Pair.ropair( "SCHEDULE_TO_START", toTimeout( getCreationTimestamp( ), getScheduleToStartTimeout( ) ) ),
        getState( ) == State.Active ? Pair.ropair( "START_TO_CLOSE", toTimeout( getStartedTimestamp( ), getStartToCloseTimeout( ) ) ) : null,
        getState( ) == State.Active ? Pair.ropair( "HEARTBEAT", toTimeout( getLastUpdateTimestamp( ), getHeartbeatTimeout( ) ) ) : null
    ), Predicates.notNull( ) );
    final Function<Pair<String,Optional<Long>>,Long> timeExtractor = Functions.compose(
        CollectionUtils.<Long>optionalOrNull( ),
        Pair.<String, Optional<Long>>right( ) );
    final Long timeout = CollectionUtils.reduce(
        CollectionUtils.fluent( taggedTimeouts )
            .transform( timeExtractor )
            .filter( Predicates.notNull( ) ), Long.MAX_VALUE, CollectionUtils.lmin( ) );
    final String tag = Iterables.tryFind( taggedTimeouts,
        CollectionUtils.propertyPredicate( timeout, timeExtractor ) )
        .transform( Pair.<String, Optional<Long>>left( ) ).or( "SCHEDULE_TO_CLOSE" );
    return timeout == Long.MAX_VALUE ?
        null :
        Pair.pair( tag, new Date( timeout ) );
  }

  private static Long toTimeout( final Date from, final Integer period ) {
    return period == null ?
        null :
        from.getTime( ) + TimeUnit.SECONDS.toMillis( period );
  }

  public WorkflowExecution getWorkflowExecution( ) {
    return workflowExecution;
  }

  public void setWorkflowExecution( final WorkflowExecution workflowExecution ) {
    this.workflowExecution = workflowExecution;
  }

  public State getState( ) {
    return state;
  }

  public void setState( final State state ) {
    this.state = state;
  }

  public String getDomain( ) {
    return domain;
  }

  public void setDomain( final String domain ) {
    this.domain = domain;
  }

  public String getDomainUuid( ) {
    return domainUuid;
  }

  public void setDomainUuid( final String domainUuid ) {
    this.domainUuid = domainUuid;
  }

  public String getWorkflowRunId() {
    return workflowRunId;
  }

  public void setWorkflowRunId( final String workflowRunId ) {
    this.workflowRunId = workflowRunId;
  }

  public String getTaskList( ) {
    return taskList;
  }

  public void setTaskList( final String taskList ) {
    this.taskList = taskList;
  }

  public Long getScheduledEventId( ) {
    return scheduledEventId;
  }

  public void setScheduledEventId( final Long scheduledEventId ) {
    this.scheduledEventId = scheduledEventId;
  }

  public Long getStartedEventId() {
    return startedEventId;
  }

  public void setStartedEventId( final Long startedEventId ) {
    this.startedEventId = startedEventId;
  }

  public Long getCancelRequestedEventId() {
    return cancelRequestedEventId;
  }

  public void setCancelRequestedEventId( final Long cancelRequestedEventId ) {
    this.cancelRequestedEventId = cancelRequestedEventId;
  }

  public String getActivityType( ) {
    return activityType;
  }

  public void setActivityType( final String activityType ) {
    this.activityType = activityType;
  }

  public String getActivityVersion( ) {
    return activityVersion;
  }

  public void setActivityVersion( final String activityVersion ) {
    this.activityVersion = activityVersion;
  }

  public String getInput( ) {
    return input;
  }

  public void setInput( final String input ) {
    this.input = input;
  }

  public Integer getScheduleToCloseTimeout() {
    return scheduleToCloseTimeout;
  }

  public void setScheduleToCloseTimeout( final Integer scheduleToCloseTimeout ) {
    this.scheduleToCloseTimeout = scheduleToCloseTimeout;
  }

  public Integer getScheduleToStartTimeout() {
    return scheduleToStartTimeout;
  }

  public void setScheduleToStartTimeout( final Integer scheduleToStartTimeout ) {
    this.scheduleToStartTimeout = scheduleToStartTimeout;
  }

  public Integer getStartToCloseTimeout() {
    return startToCloseTimeout;
  }

  public void setStartToCloseTimeout( final Integer startToCloseTimeout ) {
    this.startToCloseTimeout = startToCloseTimeout;
  }

  public Integer getHeartbeatTimeout() {
    return heartbeatTimeout;
  }

  public void setHeartbeatTimeout( final Integer heartbeatTimeout ) {
    this.heartbeatTimeout = heartbeatTimeout;
  }

  public String getHeartbeatDetails() {
    return heartbeatDetails;
  }

  public void setHeartbeatDetails( final String heartbeatDetails ) {
    this.heartbeatDetails = heartbeatDetails;
  }

  public Date getStartedTimestamp() {
    return startedTimestamp;
  }

  public void setStartedTimestamp( final Date startedTimestamp ) {
    this.startedTimestamp = startedTimestamp;
  }

  public Date getTimeoutTimestamp() {
    return timeoutTimestamp;
  }

  public void setTimeoutTimestamp( final Date timeoutTimestamp ) {
    this.timeoutTimestamp = timeoutTimestamp;
  }

  @Override
  public void updateTimeStamps() {
    super.updateTimeStamps( );
    if ( getState( ) == State.Active && startedTimestamp == null ) {
      startedTimestamp = new Date( );
    }
  }

  @PreUpdate
  @PrePersist
  protected void updateTimeout( ) {
    updateTimeStamps( );
    setTimeoutTimestamp( Optional.fromNullable( calculateNextTimeout( ) ).transform( Pair.<String,Date>right( ) ).orNull( ) );
  }
}
