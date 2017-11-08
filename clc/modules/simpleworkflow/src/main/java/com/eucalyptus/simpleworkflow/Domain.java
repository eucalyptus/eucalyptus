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

import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.DomainMetadata;
import java.util.Collection;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Predicate;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_simpleworkflow" )
@Table( name = "swf_domain" )
public class Domain extends UserMetadata<Domain.Status> implements DomainMetadata {
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
  }

  @Column( name = "description", length = 1024, updatable = false )
  private String description;

  @Column( name = "workflow_retention_days", nullable = false, updatable = false  )
  private Integer workflowExecutionRetentionPeriodInDays;

  @OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "domain" )
  private Collection<ActivityType> activityTypes;

  @OneToMany( fetch = FetchType.LAZY, mappedBy = "domain" )
  private Collection<WorkflowType> workflowTypes;

  protected Domain( ) {
  }

  protected Domain( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static Domain create( final OwnerFullName owner,
                               final String name,
                               final String description,
                               final int workflowExecutionRetentionPeriodInDays) {
    final Domain domain = new Domain( owner, name );
    domain.setState( Status.Registered );
    domain.setDescription( description );
    domain.setWorkflowExecutionRetentionPeriodInDays( workflowExecutionRetentionPeriodInDays );
    return domain;
  }

  public static Domain exampleWithOwner( final OwnerFullName owner ) {
    return new Domain( owner, null );
  }

  public static Domain exampleWithName( final OwnerFullName owner, final String name ) {
    return new Domain( owner, name );
  }

  public static Domain exampleWithUuid( final OwnerFullName owner, final String uuid ) {
    final Domain domain = new Domain( owner, null );
    domain.setNaturalId( uuid );
    return domain;
  }

  public static Domain exampleWithStatus( final Status status ) {
    final Domain domain = new Domain( null, null );
    domain.setState( status );
    domain.setStateChangeStack( null );
    domain.setLastState( null );
    return domain;
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
        .relativeId( "domain", getDisplayName() );
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( final String description ) {
    this.description = description;
  }

  public Integer getWorkflowExecutionRetentionPeriodInDays( ) {
    return workflowExecutionRetentionPeriodInDays;
  }

  public void setWorkflowExecutionRetentionPeriodInDays( final Integer workflowExecutionRetentionPeriodInDays ) {
    this.workflowExecutionRetentionPeriodInDays = workflowExecutionRetentionPeriodInDays;
  }
}
