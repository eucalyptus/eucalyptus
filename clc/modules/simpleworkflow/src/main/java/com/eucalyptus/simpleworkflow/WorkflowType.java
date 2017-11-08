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

import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.WorkflowTypeMetadata;
import java.util.Collection;
import java.util.Date;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadatas;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_simpleworkflow" )
@Table( name = "swf_workflow_type" )
public class WorkflowType extends UserMetadata<WorkflowType.Status> implements WorkflowTypeMetadata {
  private static final long serialVersionUID = 1L;

  public enum Status implements Predicate<UserMetadata<Status>> {
    Registered,
    Deprecated,
    ;

    public String toString( ) {
      return name().toUpperCase( );
    }

    @Override
    public boolean apply( @Nullable final UserMetadata<Status> metadata ) {
      return metadata != null && metadata.getState( ) == this;
    }

    public Function<WorkflowType,WorkflowType> set( ) {
      return new Function<WorkflowType, WorkflowType>() {
        @Nullable
        @Override
        public WorkflowType apply( @Nullable final WorkflowType workflowType ) {
          if ( workflowType != null && !Status.this.apply( workflowType ) ) {
            workflowType.setState( Status.this );
            workflowType.setDeprecationTimestamp( new Date( ) );
          }
          return workflowType;
        }
      };
    }
  }

  @ManyToOne
  @JoinColumn( name = "domain_id", nullable = false, updatable = false )
  private Domain domain;

  @Column( name = "workflow_version", length = 64, nullable = false, updatable = false )
  private String workflowVersion;

  @Column( name = "description", length = 1024, updatable = false )
  private String description;

  @Column( name = "default_task_list", length = 256, updatable = false  )
  private String defaultTaskList;

  @Column( name = "default_task_start_to_close_timeout" )
  private Integer defaultTaskStartToCloseTimeout;

  @Column( name = "default_exec_start_to_close_timeout" )
  private Integer defaultExecutionStartToCloseTimeout;

  @Column( name = "default_child_policy" )
  private String defaultChildPolicy;

  @Column( name = "deprecation_timestamp" )
  @Temporal( TemporalType.TIMESTAMP )
  private Date deprecationTimestamp;

  @OneToMany( fetch = FetchType.LAZY, mappedBy = "workflowType" )
  private Collection<WorkflowExecution> executions;

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
