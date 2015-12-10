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
