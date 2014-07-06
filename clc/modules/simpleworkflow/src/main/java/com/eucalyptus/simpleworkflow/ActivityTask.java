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

import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.ActivityTaskMetadata;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
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
@Table( name = "swf_activity_task" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ActivityTask extends AbstractOwnedPersistent implements ActivityTaskMetadata {
  private static final long serialVersionUID = 1L;

  public enum State {
    Pending,
    Active,
  }

  @ManyToOne
  @JoinColumn( name = "workflow_execution_id", nullable = false, updatable = false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private WorkflowExecution workflowExecution;

  @Column( name = "state", nullable = false )
  @Enumerated( EnumType.STRING )
  private State state;

  @Column( name = "domain", nullable = false, updatable = false )
  private String domain;

  @Column( name = "task_list", nullable = false, updatable = false  )
  private String taskList;

  @Column( name = "scheduled_event_id", nullable = false, updatable = false  )
  private Long scheduledEventId;

  @Column( name = "activity_type", nullable = false, updatable = false  )
  private String activityType;

  @Column( name = "activity_version", nullable = false, updatable = false  )
  private String activityVersion;

  @Column( name = "input", updatable = false  )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  private String input;

  protected ActivityTask( ) {
  }

  protected ActivityTask( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static ActivityTask create( final OwnerFullName ownerFullName,
                                     final WorkflowExecution workflowExecution,
                                     final String domain,
                                     final String activityId,
                                     final String activityType,
                                     final String activityVersion,
                                     final String input,
                                     final Long scheduledEventId,
                                     final String taskList ) {
    final ActivityTask activityTask = new ActivityTask( ownerFullName, activityId );
    activityTask.setWorkflowExecution( workflowExecution );
    activityTask.setDomain( domain );
    activityTask.setActivityType( activityType );
    activityTask.setActivityVersion( activityVersion );
    activityTask.setInput( input );
    activityTask.setScheduledEventId( scheduledEventId );
    activityTask.setTaskList( taskList );
    activityTask.setState( State.Pending );
    return activityTask;
  }

  public static ActivityTask exampleWithOwner( final OwnerFullName owner ) {
    return new ActivityTask( owner, null );
  }

  public static ActivityTask exampleWithName( final OwnerFullName owner, final String name ) {
    return new ActivityTask( owner, name );
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
}
