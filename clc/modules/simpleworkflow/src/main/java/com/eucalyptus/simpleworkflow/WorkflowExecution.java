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

import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.WorkflowExecutionMetadata;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadatas;
import com.eucalyptus.simpleworkflow.common.model.WorkflowEventAttributes;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_simpleworkflow" )
@Table( name = "swf_workflow_execution" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class WorkflowExecution extends UserMetadata<WorkflowExecution.ExecutionStatus> implements WorkflowExecutionMetadata {
  private static final long serialVersionUID = 1L;

  public enum ExecutionStatus {
    Open,
    Closed,
    ;

    public String toString( ) {
      return name( ).toUpperCase( );
    }
  }

  public enum CloseStatus {
    Completed,
    Failed,
    Canceled,
    Terminated,
    Continued_As_New,
    Timed_Out
    ;

    public String toString( ) {
      return name( ).toUpperCase( );
    }

    public static CloseStatus fromString( final String value ) {
      return Iterables.tryFind(
          Arrays.asList( values( ) ),
          CollectionUtils.propertyPredicate( value, Functions.toStringFunction( ) )  ).or( CloseStatus.Completed );
    }
  }

  public enum DecisionStatus {
    Idle,
    Pending,
    Active,
    ;
  }

  @ManyToOne
  @JoinColumn( name = "domain_id", nullable = false, updatable = false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Domain domain;

  @ManyToOne
  @JoinColumn( name = "workflow_type_id", nullable = false, updatable = false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private WorkflowType workflowType;

  @Column( name = "workflow_id", length = 256, nullable = false, updatable = false )
  private String workflowId;

  @Column( name = "child_policy", nullable = false, updatable = false )
  private String childPolicy;

  @Column( name = "domain", length = 256, nullable = false, updatable = false )
  private String domainName;

  @Column( name = "domain_uuid", nullable = false, updatable = false )
  private String domainUuid;

  @Column( name = "task_list", length = 256, nullable = false, updatable = false )
  private String taskList;

  @Column( name = "exec_start_to_close_timeout", nullable = false, updatable = false )
  private Integer executionStartToCloseTimeout;

  @Column( name = "task_start_to_close_timeout", updatable = false )
  private Integer taskStartToCloseTimeout;

  @Column( name = "cancel_requested", nullable = false )
  private Boolean cancelRequested;

  @Column( name = "decision_status" )
  @Enumerated( EnumType.STRING )
  private DecisionStatus decisionStatus;

  @Column( name = "decision_timestamp", nullable = false)
  @Temporal( TemporalType.TIMESTAMP )
  private Date decisionTimestamp;

  @Column( name = "close_status" )
  @Enumerated( EnumType.STRING )
  private CloseStatus closeStatus;

  @Column( name = "close_timestamp" )
  @Temporal( TemporalType.TIMESTAMP )
  private Date closeTimestamp;

  @Column( name = "retention_timestamp" )
  @Temporal( TemporalType.TIMESTAMP )
  private Date retentionTimestamp;

  @ElementCollection
  @CollectionTable( name = "swf_workflow_execution_tags" )
  @Column( name = "tag", length = 256 )
  @JoinColumn( name = "workflow_execution_id" )
  @OrderColumn( name = "tag_index")
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<String> tagList;

  @Column( name = "latest_activity_timestamp" )
  @Temporal( TemporalType.TIMESTAMP )
  private Date latestActivityTaskScheduled;

  @Column( name = "latest_execution_context" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  private String latestExecutionContext;

  @Column( name = "timeout_timestamp" )
  @Temporal( TemporalType.TIMESTAMP )
  private Date timeoutTimestamp;

  @OneToMany( fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REMOVE }, orphanRemoval = true, mappedBy = "workflowExecution" )
  @OrderColumn( name = "event_id" )
  private List<WorkflowHistoryEvent> workflowHistory;

  @OneToMany( fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, orphanRemoval = true, mappedBy = "workflowExecution" )
  @OrderColumn( name = "scheduled_event_id" )
  private List<ActivityTask> activityTasks;

  protected WorkflowExecution( ) {
  }

  protected WorkflowExecution( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static WorkflowExecution create( final OwnerFullName owner,
                                          final String name, /* runId */
                                          final Domain domain,
                                          final WorkflowType workflowType,
                                          final String workflowId,
                                          final String childPolicy,
                                          final String taskList,
                                          @Nullable final Integer executionStartToCloseTimeout,
                                          @Nullable final Integer taskStartToCloseTimeout,
                                          final List<String> tags,
                                          final List<WorkflowEventAttributes> eventAttributes ) {
    final WorkflowExecution workflowExecution = new WorkflowExecution( owner, name );
    workflowExecution.setDomain( domain );
    workflowExecution.setDomainName( domain.getDisplayName( ) );
    workflowExecution.setDomainUuid( domain.getNaturalId( ) );
    workflowExecution.setWorkflowType( workflowType );
    workflowExecution.setWorkflowId( workflowId );
    workflowExecution.setState( ExecutionStatus.Open );
    workflowExecution.setChildPolicy( childPolicy );
    workflowExecution.setTaskList( taskList );
    workflowExecution.setExecutionStartToCloseTimeout( executionStartToCloseTimeout );
    workflowExecution.setTaskStartToCloseTimeout( taskStartToCloseTimeout );
    workflowExecution.setTagList( tags );
    workflowExecution.setCancelRequested( false );
    workflowExecution.setDecisionStatus( DecisionStatus.Pending );
    workflowExecution.setDecisionTimestamp( new Date( ) );
    workflowExecution.setWorkflowHistory( Lists.<WorkflowHistoryEvent>newArrayList( ) );
    for ( final WorkflowEventAttributes attributes : eventAttributes ) {
      workflowExecution.addHistoryEvent( WorkflowHistoryEvent.create( workflowExecution, attributes ) );
    }
    return workflowExecution;
  }

  public Date calculateNextTimeout( ) {
    final Long timeout = CollectionUtils.reduce( Iterables.filter( Lists.newArrayList(
        toTimeout( getCreationTimestamp( ), getExecutionStartToCloseTimeout( ) ),
        toTimeout( getDecisionTimestamp( ), getDecisionStatus( ) != DecisionStatus.Idle ? getTaskStartToCloseTimeout( ) : null )
    ), Predicates.notNull( ) ), Long.MAX_VALUE, CollectionUtils.lmin( ) );
    return timeout == Long.MAX_VALUE ? null : new Date( timeout );
  }

  public boolean isWorkflowTimedOut( final long timestamp,
                                     final long maximumDurationMillis ){
    final Long timeout = toTimeout( getCreationTimestamp( ), getExecutionStartToCloseTimeout( ) );
    return
        ( timeout != null && timeout < timestamp ) ||
        ( maximumDurationMillis > 0 && ( getCreationTimestamp( ).getTime( ) + maximumDurationMillis ) < timestamp );
  }

  private static Long toTimeout( final Date from, final Integer period ) {
    return period == null ?
        null :
        from.getTime( ) + TimeUnit.SECONDS.toMillis( period );
  }

  public static WorkflowExecution exampleWithOwner( final OwnerFullName owner ) {
    return new WorkflowExecution( owner, null );
  }

  public static WorkflowExecution exampleWithName( final OwnerFullName owner, final String name ) {
    return new WorkflowExecution( owner, name );
  }

  public static WorkflowExecution exampleWithPendingDecision( final OwnerFullName owner,
                                                              final String domain,
                                                              final String taskList ) {
    final WorkflowExecution workflowExecution = new WorkflowExecution( owner, null );
    workflowExecution.setDomainName( domain );
    workflowExecution.setTaskList( taskList );
    workflowExecution.setDecisionStatus( DecisionStatus.Pending );
    workflowExecution.setState( ExecutionStatus.Open );
    return workflowExecution;
  }

  public static WorkflowExecution exampleWithUniqueName( final OwnerFullName owner,
                                                         final String domain,
                                                         final String runId ) {
    final WorkflowExecution workflowExecution = new WorkflowExecution( owner, runId );
    workflowExecution.setUniqueName( createUniqueName( owner.getAccountNumber(), domain, runId ) );
    return workflowExecution;
  }

  public static WorkflowExecution exampleForOpenWorkflow( ) {
    return exampleForOpenWorkflow( null, null, null );
  }

  public static WorkflowExecution exampleForOpenWorkflow( final OwnerFullName owner,
                                                          final String domain,
                                                          final String workflowId ) {
    return exampleForOpenWorkflow( owner, domain, workflowId, null );
  }

  public static WorkflowExecution exampleForOpenWorkflow( final OwnerFullName owner,
                                                          final String domain,
                                                          final String workflowId,
                                                          final String runId ) {
    final WorkflowExecution workflowExecution = new WorkflowExecution( owner, runId );
    workflowExecution.setDomainName( domain );
    workflowExecution.setWorkflowId( workflowId );
    workflowExecution.setState( ExecutionStatus.Open );
    workflowExecution.setStateChangeStack( null );
    workflowExecution.setLastState( null );
    return workflowExecution;
  }

  public static WorkflowExecution exampleForClosedWorkflow( ) {
    return exampleForClosedWorkflow( null, null, null );
  }

  public static WorkflowExecution exampleForClosedWorkflow( final OwnerFullName owner,
                                                            final String domain,
                                                            final String workflowId ) {
    final WorkflowExecution workflowExecution = new WorkflowExecution( owner, null );
    workflowExecution.setDomainName( domain );
    workflowExecution.setWorkflowId( workflowId );
    workflowExecution.setState( ExecutionStatus.Closed );
    workflowExecution.setStateChangeStack( null );
    workflowExecution.setLastState( null );
    return workflowExecution;
  }

  @Override
  protected String createUniqueName( ) {
    return createUniqueName( getOwnerAccountNumber(),
        SimpleWorkflowMetadatas.toDisplayName().apply( getDomain() ),
        getDisplayName() );
  }

  private static String createUniqueName( final String accountNumber,
                                          final String domain,
                                          final String runId ) {
    return accountNumber + ":" + domain + ":" + runId;
  }

  public Long addHistoryEvent( final WorkflowHistoryEvent event ) throws WorkflowHistorySizeLimitException {
    // Order would be filled in on save, but we may need the event
    // identifier before the entity is stored
    event.setEventOrder( (long) workflowHistory.size( ) );
    workflowHistory.add( event );
    if ( workflowHistory.size( ) > SimpleWorkflowConfiguration.getWorkflowExecutionHistorySize( ) ) {
      throw new WorkflowHistorySizeLimitException( this );
    }
    updateTimeStamps( ); // ensure workflow version incremented
    return event.getEventId();
  }

  public void closeWorkflow( final CloseStatus closeStatus,
                             final WorkflowHistoryEvent event ) {
    setState( WorkflowExecution.ExecutionStatus.Closed );
    setCloseStatus( closeStatus );
    setCloseTimestamp( new Date( ) );
    setRetentionTimestamp( new Date(
        getCloseTimestamp( ).getTime( ) +
            TimeUnit.DAYS.toMillis( getDomain( ).getWorkflowExecutionRetentionPeriodInDays( ) ) ) );
    addHistoryEvent( event );
  }

  @Override
  public String getPartition( ) {
    return "eucalyptus";
  }

  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" )
        .region( ComponentIds.lookup( SimpleWorkflow.class ).name() )
        .namespace( this.getOwnerAccountNumber() )
        .relativeId(
            "domain", SimpleWorkflowMetadatas.toDisplayName( ).apply( getDomain() ),
            "run-id", getDisplayName() );
  }

  public Domain getDomain() {
    return domain;
  }

  public void setDomain( final Domain domain ) {
    this.domain = domain;
  }

  public WorkflowType getWorkflowType( ) {
    return workflowType;
  }

  public void setWorkflowType( final WorkflowType workflowType ) {
    this.workflowType = workflowType;
  }

  public String getWorkflowId( ) {
    return workflowId;
  }

  public void setWorkflowId( final String workflowId ) {
    this.workflowId = workflowId;
  }

  public String getChildPolicy( ) {
    return childPolicy;
  }

  public void setChildPolicy( final String childPolicy ) {
    this.childPolicy = childPolicy;
  }

  public String getDomainName() {
    return domainName;
  }

  public void setDomainName( final String domainName ) {
    this.domainName = domainName;
  }

  public String getDomainUuid( ) {
    return domainUuid;
  }

  public void setDomainUuid( final String domainUuid ) {
    this.domainUuid = domainUuid;
  }

  public String getTaskList( ) {
    return taskList;
  }

  public void setTaskList( final String taskList ) {
    this.taskList = taskList;
  }

  public Integer getExecutionStartToCloseTimeout( ) {
    return executionStartToCloseTimeout;
  }

  public void setExecutionStartToCloseTimeout( final Integer executionStartToCloseTimeout ) {
    this.executionStartToCloseTimeout = executionStartToCloseTimeout;
  }

  public Integer getTaskStartToCloseTimeout( ) {
    return taskStartToCloseTimeout;
  }

  public void setTaskStartToCloseTimeout( final Integer taskStartToCloseTimeout ) {
    this.taskStartToCloseTimeout = taskStartToCloseTimeout;
  }

  public Boolean getCancelRequested( ) {
    return cancelRequested;
  }

  public void setCancelRequested( final Boolean cancelRequested ) {
    this.cancelRequested = cancelRequested;
  }

  public DecisionStatus getDecisionStatus( ) {
    return decisionStatus;
  }

  public void setDecisionStatus( final DecisionStatus decisionStatus ) {
    this.decisionStatus = decisionStatus;
  }

  public Date getDecisionTimestamp() {
    return decisionTimestamp;
  }

  public void setDecisionTimestamp( final Date decisionTimestamp ) {
    this.decisionTimestamp = decisionTimestamp;
  }

  public CloseStatus getCloseStatus( ) {
    return closeStatus;
  }

  public void setCloseStatus( final CloseStatus closeStatus ) {
    this.closeStatus = closeStatus;
  }

  public Date getCloseTimestamp( ) {
    return closeTimestamp;
  }

  public void setCloseTimestamp( final Date closeTimestamp ) {
    this.closeTimestamp = closeTimestamp;
  }

  public Date getRetentionTimestamp() {
    return retentionTimestamp;
  }

  public void setRetentionTimestamp( final Date retentionTimestamp ) {
    this.retentionTimestamp = retentionTimestamp;
  }

  public List<String> getTagList( ) {
    return tagList;
  }

  public void setTagList( final List<String> tagList ) {
    this.tagList = tagList;
  }

  public Date getLatestActivityTaskScheduled( ) {
    return latestActivityTaskScheduled;
  }

  public void setLatestActivityTaskScheduled( final Date latestActivityTaskScheduled ) {
    this.latestActivityTaskScheduled = latestActivityTaskScheduled;
  }

  public String getLatestExecutionContext( ) {
    return latestExecutionContext;
  }

  public void setLatestExecutionContext( final String latestExecutionContext ) {
    this.latestExecutionContext = latestExecutionContext;
  }

  public Date getTimeoutTimestamp() {
    return timeoutTimestamp;
  }

  public void setTimeoutTimestamp( final Date timeoutTimestamp ) {
    this.timeoutTimestamp = timeoutTimestamp;
  }

  public List<WorkflowHistoryEvent> getWorkflowHistory() {
    return workflowHistory;
  }

  public void setWorkflowHistory( final List<WorkflowHistoryEvent> workflowHistory ) {
    this.workflowHistory = workflowHistory;
  }

  @PreUpdate
  @PrePersist
  protected void updateTimeout( ) {
    updateTimeStamps( );
    setTimeoutTimestamp( calculateNextTimeout( ) );
  }

  public static final class WorkflowHistorySizeLimitException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String accountNumber;
    private final String domain;
    private final String runId;
    private final String workflowId;

    public WorkflowHistorySizeLimitException( final WorkflowExecution workflowExecution ) {
      this(
          workflowExecution.getOwnerAccountNumber( ),
          workflowExecution.getDomainName( ),
          workflowExecution.getDisplayName( ),
          workflowExecution.getWorkflowId( )
      );
    }

    public WorkflowHistorySizeLimitException( final String accountNumber,
                                              final String domain,
                                              final String runId,
                                              final String workflowId ) {
      this.accountNumber = accountNumber;
      this.domain = domain;
      this.runId = runId;
      this.workflowId = workflowId;
    }

    public String getAccountNumber() {
      return accountNumber;
    }

    public String getDomain() {
      return domain;
    }

    public String getRunId() {
      return runId;
    }

    public String getWorkflowId() {
      return workflowId;
    }
  }
}
