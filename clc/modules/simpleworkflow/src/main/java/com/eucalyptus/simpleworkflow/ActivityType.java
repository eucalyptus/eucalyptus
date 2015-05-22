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

import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.ActivityTypeMetadata;
import java.util.Date;
import javax.annotation.Nullable;
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
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_simpleworkflow" )
@Table( name = "swf_activity_type" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ActivityType extends UserMetadata<ActivityType.Status> implements ActivityTypeMetadata {
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

    public Function<ActivityType,ActivityType> set( ) {
      return new Function<ActivityType, ActivityType>() {
        @Nullable
        @Override
        public ActivityType apply( @Nullable final ActivityType activityType ) {
          if ( activityType != null && !Status.this.apply( activityType ) ) {
            activityType.setState( Status.this );
            activityType.setDeprecationTimestamp( new Date( ) );
          }
          return activityType;
        }
      };
    }
  }

  @ManyToOne
  @JoinColumn( name = "domain_id", nullable = false, updatable = false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Domain domain;

  @Column( name = "activity_version", length = 64, nullable = false, updatable = false )
  private String activityVersion;

  @Column( name = "description", length = 1024, updatable = false )
  private String description;

  @Column( name = "default_task_list", length = 256, updatable = false  )
  private String defaultTaskList;

  @Column( name = "default_task_heartbeat_timeout", updatable = false  )
  private Integer defaultTaskHeartbeatTimeout;

  @Column( name = "default_task_schedule_to_close_timeout", updatable = false  )
  private Integer defaultTaskScheduleToCloseTimeout;

  @Column( name = "default_task_schedule_to_start_timeout", updatable = false  )
  private Integer defaultTaskScheduleToStartTimeout;

  @Column( name = "default_task_start_to_close_timeout", updatable = false  )
  private Integer defaultTaskStartToCloseTimeout;

  @Column( name = "deprecation_timestamp" )
  @Temporal( TemporalType.TIMESTAMP )
  private Date deprecationTimestamp;

  protected ActivityType( ) {
  }

  protected ActivityType( final OwnerFullName owner, final String displayName ) {
    super( owner, displayName );
  }

  public static ActivityType create( final OwnerFullName owner,
                                     final String name,
                                     final String version,
                                     final Domain domain,
                                     final String description,
                                     final String defaultTaskList,
                                     final Integer defaultTaskHeartbeatTimeout,
                                     final Integer defaultTaskScheduleToCloseTimeout,
                                     final Integer defaultTaskScheduleToStartTimeout,
                                     final Integer defaultTaskStartToCloseTimeout ) {
    final ActivityType activityType = new ActivityType( owner, name );
    activityType.setState( Status.Registered );
    activityType.setActivityVersion( version );
    activityType.setDomain( domain );
    activityType.setDescription( description );
    activityType.setDefaultTaskList( defaultTaskList );
    activityType.setDefaultTaskHeartbeatTimeout( defaultTaskHeartbeatTimeout );
    activityType.setDefaultTaskScheduleToCloseTimeout( defaultTaskScheduleToCloseTimeout );
    activityType.setDefaultTaskScheduleToStartTimeout( defaultTaskScheduleToStartTimeout );
    activityType.setDefaultTaskStartToCloseTimeout( defaultTaskStartToCloseTimeout );
    return activityType;
  }

  public static ActivityType exampleWithOwner( final OwnerFullName owner ) {
    return new ActivityType( owner, null );
  }

  public static ActivityType exampleWithName( final OwnerFullName owner, final String name ) {
    return new ActivityType( owner, name );
  }

  public static ActivityType exampleWithUniqueName( final OwnerFullName owner,
                                                    final String domain,
                                                    final String name,
                                                    final String version ) {
    final ActivityType activityType = new ActivityType( owner, name );
    activityType.setUniqueName( createUniqueName( owner.getAccountNumber( ), domain, name, version ) );
    return activityType;
  }

  @Override
  protected String createUniqueName( ) {
    return createUniqueName( getOwnerAccountNumber( ),
        SimpleWorkflowMetadatas.toDisplayName( ).apply( getDomain() ),
        getDisplayName( ),
        getActivityVersion( ) );
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
            "activity-type", getDisplayName( ),
            "version", getActivityVersion( ) );
  }

  public Domain getDomain( ) {
    return domain;
  }

  public void setDomain( final Domain domain ) {
    this.domain = domain;
  }

  public String getActivityVersion( ) {
    return activityVersion;
  }

  public void setActivityVersion( final String activityVersion ) {
    this.activityVersion = activityVersion;
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

  public Integer getDefaultTaskHeartbeatTimeout( ) {
    return defaultTaskHeartbeatTimeout;
  }

  public void setDefaultTaskHeartbeatTimeout( final Integer defaultTaskHeartbeatTimeout ) {
    this.defaultTaskHeartbeatTimeout = defaultTaskHeartbeatTimeout;
  }

  public Integer getDefaultTaskScheduleToCloseTimeout( ) {
    return defaultTaskScheduleToCloseTimeout;
  }

  public void setDefaultTaskScheduleToCloseTimeout( final Integer defaultTaskScheduleToCloseTimeout ) {
    this.defaultTaskScheduleToCloseTimeout = defaultTaskScheduleToCloseTimeout;
  }

  public Integer getDefaultTaskScheduleToStartTimeout( ) {
    return defaultTaskScheduleToStartTimeout;
  }

  public void setDefaultTaskScheduleToStartTimeout( final Integer defaultTaskScheduleToStartTimeout ) {
    this.defaultTaskScheduleToStartTimeout = defaultTaskScheduleToStartTimeout;
  }

  public Integer getDefaultTaskStartToCloseTimeout( ) {
    return defaultTaskStartToCloseTimeout;
  }

  public void setDefaultTaskStartToCloseTimeout( final Integer defaultTaskStartToCloseTimeout ) {
    this.defaultTaskStartToCloseTimeout = defaultTaskStartToCloseTimeout;
  }

  public Date getDeprecationTimestamp( ) {
    return deprecationTimestamp;
  }

  public void setDeprecationTimestamp( final Date deprecationTimestamp ) {
    this.deprecationTimestamp = deprecationTimestamp;
  }
}
