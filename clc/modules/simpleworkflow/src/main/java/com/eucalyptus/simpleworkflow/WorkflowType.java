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

import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.WorkflowTypeMetadata;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadatas;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.OwnerFullName;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_simpleworkflow" )
@Table( name = "swf_workflow_type" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class WorkflowType extends UserMetadata<WorkflowType.Status> implements WorkflowTypeMetadata {
  private static final long serialVersionUID = 1L;

  public enum Status {
    Registered,
    Deprecated,
    ;

    public String toString( ) {
      return name().toUpperCase( );
    }
  }

  @ManyToOne
  @JoinColumn( name = "domain_id", nullable = false, updatable = false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Domain domain;

  @Column( name = "workflow_version", nullable = false, updatable = false )
  private String workflowVersion;

  @Column( name = "description", updatable = false )
  private String description;

  @Column( name = "default_task_list", updatable = false  )
  private String defaultTaskList;

  @Column( name = "default_task_start_to_close_timeout" )
  private Integer defaultTaskStartToCloseTimeout;

  @Column( name = "default_exec_start_to_close_timeout" )
  private Integer defaultExecutionStartToCloseTimeout;

  @Column( name = "default_child_policy" )
  private String defaultChildPolicy;

  @Column( name = "deprecation_date" )
  @Temporal( TemporalType.TIMESTAMP )
  private Date deprecationTimestamp;

  protected WorkflowType( ) {
  }

  protected WorkflowType( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static WorkflowType create( final OwnerFullName owner,
                                     final String name,
                                     final String version,
                                     final Domain domain,
                                     final String description,
                                     final String defaultTaskList,
                                     final String defaultChildPolicy,
                                     final Integer defaultExecutionStartToCloseTimeout,
                                     final Integer defaultTaskStartToCloseTimeout ) {
    final WorkflowType workflowType = new WorkflowType( owner, name );
    workflowType.setState( Status.Registered );
    workflowType.setWorkflowVersion( version );
    workflowType.setDomain( domain );
    workflowType.setDescription( description );
    workflowType.setDefaultTaskList( defaultTaskList );
    workflowType.setDefaultChildPolicy( defaultChildPolicy );
    workflowType.setDefaultExecutionStartToCloseTimeout( defaultExecutionStartToCloseTimeout );
    workflowType.setDefaultTaskStartToCloseTimeout( defaultTaskStartToCloseTimeout );
    return workflowType;
  }

  public static WorkflowType exampleWithOwner( final OwnerFullName owner ) {
    return new WorkflowType( owner, null );
  }

  public static WorkflowType exampleWithName( final OwnerFullName owner, final String name ) {
    return new WorkflowType( owner, name );
  }

  public static WorkflowType exampleWithUniqueName( final OwnerFullName owner,
                                                    final String domain,
                                                    final String name,
                                                    final String version ) {
    final WorkflowType workflowType = new WorkflowType( owner, name );
    workflowType.setUniqueName( createUniqueName( owner.getAccountNumber(), domain, name, version ) );
    return workflowType;
  }

  @Override
  protected String createUniqueName( ) {
    return createUniqueName( getOwnerAccountNumber( ),
        SimpleWorkflowMetadatas.toDisplayName( ).apply( getDomain( ) ),
        getDisplayName( ),
        getWorkflowVersion( ) );
  }

  private static String createUniqueName( final String accountNumber,
                                          final String domain,
                                          final String name,
                                          final String version ) {
    return accountNumber + ":" + domain + ":" + name + ":" + version;
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
            "domain", SimpleWorkflowMetadatas.toDisplayName( ).apply( getDomain( ) ),
            "workflow-type", getDisplayName( ),
            "version", getWorkflowVersion( ) );
  }

  public Domain getDomain( ) {
    return domain;
  }

  public void setDomain( final Domain domain ) {
    this.domain = domain;
  }

  public String getWorkflowVersion() {
    return workflowVersion;
  }

  public void setWorkflowVersion( final String workflowVersion ) {
    this.workflowVersion = workflowVersion;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( final String description ) {
    this.description = description;
  }

  public String getDefaultTaskList( ) {
    return defaultTaskList;
  }

  public void setDefaultTaskList( final String defaultTaskList ) {
    this.defaultTaskList = defaultTaskList;
  }

  public Integer getDefaultTaskStartToCloseTimeout() {
    return defaultTaskStartToCloseTimeout;
  }

  public void setDefaultTaskStartToCloseTimeout( final Integer defaultTaskStartToCloseTimeout ) {
    this.defaultTaskStartToCloseTimeout = defaultTaskStartToCloseTimeout;
  }

  public Integer getDefaultExecutionStartToCloseTimeout() {
    return defaultExecutionStartToCloseTimeout;
  }

  public void setDefaultExecutionStartToCloseTimeout( final Integer defaultExecutionStartToCloseTimeout ) {
    this.defaultExecutionStartToCloseTimeout = defaultExecutionStartToCloseTimeout;
  }

  public String getDefaultChildPolicy() {
    return defaultChildPolicy;
  }

  public void setDefaultChildPolicy( final String defaultChildPolicy ) {
    this.defaultChildPolicy = defaultChildPolicy;
  }

  public Date getDeprecationTimestamp( ) {
    return deprecationTimestamp;
  }

  public void setDeprecationTimestamp( final Date deprecationTimestamp ) {
    this.deprecationTimestamp = deprecationTimestamp;
  }
}
