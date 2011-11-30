/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.EntityTransaction;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.context.ServiceStateException;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Faults {
  private static Logger LOG = Logger.getLogger( Faults.class );
  
  enum NoopErrorFilter implements Predicate<Throwable> {
    INSTANCE;
    
    @Override
    public boolean apply( final Throwable input ) {
      Logs.exhaust( ).error( input, input );
      return true;
    }
    
  }
  
  @Entity
  @javax.persistence.Entity
  @PersistenceContext( name = "eucalyptus_faults" )
  @Table( name = "faults_records" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  public static class CheckException extends RuntimeException implements Iterable<CheckException> {
    @Id
    @GeneratedValue( generator = "system-uuid" )
    @GenericGenerator( name = "system-uuid",
                       strategy = "uuid" )
    @Column( name = "id" )
    String                        id;
    @Version
    @Column( name = "version" )
    Integer                       version;
    @Temporal( TemporalType.TIMESTAMP )
    @Column( name = "creation_timestamp" )
    Date                          creationTimestamp;
    @Temporal( TemporalType.TIMESTAMP )
    @Column( name = "last_update_timestamp" )
    Date                          lastUpdateTimestamp;
    @NaturalId
    @Column( name = "metadata_perm_uuid",
             unique = true,
             updatable = false,
             nullable = false )
    private String                naturalId;
    @Transient
    private static final long     serialVersionUID = 1L;
    @Enumerated( EnumType.STRING )
    private final Severity        severity;
    @Column( name = "fault_service_name" )
    private final String          serviceName;
    @Column( name = "fault_timestamp" )
    private final Date            timestamp;
    @Column( name = "fault_msg_correlation_id" )
    private final String          correlationId;
    @Column( name = "fault_event_epoch" )
    private final Integer         eventEpoch;
    @Column( name = "fault_service_state" )
    private final Component.State eventState;
    @Column( name = "fault_stack_trace" )
    private final String          stackString;
    @Transient
    private CheckException        other;
    
    private CheckException( final String serviceName ) {
      this.serviceName = serviceName;
      this.severity = null;
      this.timestamp = null;
      this.correlationId = null;
      this.eventEpoch = null;
      this.eventState = null;
      this.stackString = null;
    }
    
    CheckException( final ServiceConfiguration config, final Severity severity, final Throwable cause ) {
      this( config, severity, cause.getMessage( ), cause, null );
    }
    
    CheckException( final String correlationId, final Throwable cause, final Severity severity, final ServiceConfiguration config ) {
      this( config, severity, cause.getMessage( ), cause, correlationId );
    }
    
    CheckException( final ServiceConfiguration config, final Severity severity, final String message, final Throwable cause, final String correlationId ) {
      super( message != null
        ? message
        : ( cause != null
          ? cause.getMessage( )
          : Threads.currentStackRange( 1, 5 ) ) );
      if ( cause instanceof CheckException ) {
        this.setStackTrace( cause.getStackTrace( ) );
      } else if ( cause != null ) {
        this.initCause( cause );
      }
      this.severity = severity;
      this.serviceName = config.getName( );
      this.correlationId = ( correlationId == null
        ? UUID.randomUUID( ).toString( )
        : correlationId );
      this.timestamp = new Date( );
      this.eventState = config.lookupState( );
      this.eventEpoch = Topology.epoch( );
      this.stackString = Exceptions.string( this );
    }
    
    @PreUpdate
    @PrePersist
    public void updateTimeStamps( ) {
      this.lastUpdateTimestamp = new Date( );
      if ( this.creationTimestamp == null ) {
        this.creationTimestamp = new Date( );
      }
      if ( this.naturalId == null ) {
        this.naturalId = UUID.randomUUID( ).toString( );
      }
    }
    
    public Severity getSeverity( ) {
      return this.severity;
    }
    
    @Override
    public Iterator<CheckException> iterator( ) {
      return new Iterator<CheckException>( ) {
        CheckException curr;
        {
          this.curr = CheckException.this;
        }
        
        @Override
        public boolean hasNext( ) {
          return ( this.curr != null ) && ( this.curr.other != null );
        }
        
        @Override
        public CheckException next( ) {
          return this.curr = this.curr.other;
        }
        
        @Override
        public void remove( ) {
          LOG.error( "ServiceCheckException iterator does not support remove()" );
        }
      };
    }
    
    public Date getTimestamp( ) {
      return this.timestamp;
    }
    
    public String getCorrelationId( ) {
      return this.correlationId;
    }
    
    public String getServiceName( ) {
      return this.serviceName;
    }
    
    public int getEventEpoch( ) {
      return this.eventEpoch;
    }
    
    public Component.State getEventState( ) {
      return this.eventState;
    }
    
    public String getId( ) {
      return this.id;
    }
    
    public void setId( final String id ) {
      this.id = id;
    }
    
    public Integer getVersion( ) {
      return this.version;
    }
    
    public void setVersion( final Integer version ) {
      this.version = version;
    }
    
    public Date getCreationTimestamp( ) {
      return this.creationTimestamp;
    }
    
    public void setCreationTimestamp( final Date creationTimestamp ) {
      this.creationTimestamp = creationTimestamp;
    }
    
    public Date getLastUpdateTimestamp( ) {
      return this.lastUpdateTimestamp;
    }
    
    public void setLastUpdateTimestamp( final Date lastUpdateTimestamp ) {
      this.lastUpdateTimestamp = lastUpdateTimestamp;
    }
    
    public String getNaturalId( ) {
      return this.naturalId;
    }
    
    public void setNaturalId( final String naturalId ) {
      this.naturalId = naturalId;
    }
    
    private CheckException getOther( ) {
      return this.other;
    }
    
    private void setOther( final CheckException other ) {
      this.other = other;
    }
    
    public String getStackString( ) {
      return this.stackString;
    }
    
  }
  
  /**
   * @param parent
   * @param ex
   * @param failureAction
   * @return true if the error is fatal and the transition should be aborted
   */
  public static final boolean filter( final ServiceConfiguration parent, final Throwable ex, final Predicate<Throwable>... filters ) {
    Predicate<Throwable> failureAction;
    if ( ( filters != null ) && ( filters.length > 0 ) ) {
      failureAction = filters[0];
    } else {
      failureAction = NoopErrorFilter.INSTANCE;
    }
    if ( ex instanceof CheckException ) {//go through all the exceptions and look for things with Severity greater than or equal to ERROR
      for ( final CheckException checkEx : ( CheckException ) ex ) {
        if ( checkEx.getSeverity( ).ordinal( ) >= Severity.ERROR.ordinal( ) ) {
          try {
            failureAction.apply( ex );
          } catch ( final Exception ex1 ) {
            Logs.extreme( ).error( ex1, ex1 );
          }
          return true;
        }
      }
      return false;
    } else {//treat generic exceptions as always being Severity.ERROR
      try {
        failureAction.apply( ex );
      } catch ( final Exception ex1 ) {
        Logs.extreme( ).error( ex1, ex1 );
      }
      return true;
    }
  }
  
  public static final boolean filter( final ServiceConfiguration parent, final Throwable ex ) {
    return filter( parent, ex, NoopErrorFilter.INSTANCE );
  }
  
  /**
   * The possible actions are:
   * - store: for later review, e.g., log analyzer
   * - log: write to log files at the primary CLC
   * - describe: make available in euca-describe-*
   * - ui: notification is presented in the ui at next login (note: this is different than filtering
   * check-exception history)
   * - notify: basic notifcation is delivered (i.e., email)
   * - alert: recurrent/urgent notification is delivered until disabled
   * 
   * TODO:GRZE: this behaviour should be @Configurable
   */
  public enum Actions {
    STORE,
    LOGGING,
    DESCRIBE,
    UI,
    NOTIFY,
    ALERT
  }
  
  /**
   * Severity levels which can be used to express the system's reaction to exceptions thrown by
   * either {@link Bootstrapper#check()} or {@link ServiceBuilder#fireCheck(ServiceConfiguration)}.
   * The default severity used for unchecked exceptions is {@link Severity#ERROR}. Environmentally
   * triggered changes to system topology are reported as {@link Severity#URGENT}.
   * 
   * Severity of the exception determines:
   * 1. The way the system responds in terms of changing service state/system topology
   * 2. The length of time for which the record is stored
   * 3. The means used to deliver notifications to the admin
   * 
   * TODO:GRZE: this behaviour should be @Configurable
   */
  public enum Severity {
    DEBUG, //default: store
    INFO, //default: store, describe
    WARNING, //default: store, describe, ui, notification
    ERROR, //default: store, describe, ui, notification
    URGENT, //default: store, describe, ui, notification, alert
    FATAL;
    
  }
  
  public enum Scope {
    SERVICE,
    HOST,
    NETWORK;
  }
  
  private static CheckException chain( final ServiceConfiguration config, final Severity severity, final List<? extends Throwable> exs ) {
    CheckException last = null;
    for ( final Throwable ex : Lists.reverse( exs ) ) {
      if ( ( last != null ) && ( ex instanceof CheckException ) ) {
        last.other = ( CheckException ) ex;
      } else if ( last == null ) {
        last = new CheckException( config, severity, ex );
      }
    }
    last = ( last != null
      ? last
      : new CheckException( config, Severity.DEBUG, new NullPointerException( "Faults.chain called w/ empty list: " + exs ) ) );
    return last;
  }
  
  public static CheckException failure( final ServiceConfiguration config, final Throwable... exs ) {
    return failure( config, Arrays.asList( exs ) );
  }
  
  public static CheckException failure( final ServiceConfiguration config, final List<? extends Throwable> exs ) {
    return chain( config, Severity.ERROR, ( List<Throwable> ) exs );
  }
  
  public static CheckException advisory( final ServiceConfiguration config, final List<? extends Throwable> exs ) {
    return chain( config, Severity.INFO, ( List<Throwable> ) exs );
  }
  
  public static CheckException advisory( final ServiceConfiguration config, final Throwable... exs ) {
    return advisory( config, Arrays.asList( exs ) );
  }
  
  public static CheckException fatal( final ServiceConfiguration config, final List<? extends Throwable> exs ) {
    return chain( config, Severity.FATAL, ( List<Throwable> ) exs );
  }
  
  enum StatusToCheckException implements Function<ServiceStatusType, CheckException> {
    INSTANCE;
    
    @Override
    public CheckException apply( final ServiceStatusType input ) {
      final List<CheckException> exs = Lists.newArrayList( );
      final ServiceConfiguration config = TypeMappers.transform( input.getServiceId( ), ServiceConfiguration.class );
      final Component.State serviceState = Component.State.valueOf( input.getLocalState( ) );
      final Component.State localState = config.lookupState( );
      if ( Component.State.ENABLED.equals( localState ) && !localState.equals( serviceState ) ) {
        exs.add( failure( config, new IllegalStateException( "State mismatch: local state is " + localState + " and remote state is: " + serviceState ) ) );
      }
      for ( final String detail : input.getDetails( ) ) {
        final CheckException ex = failure( config, new ServiceStateException( detail ) );
        exs.add( ex );
      }
      return !exs.isEmpty( ) ? Faults.chain( config, Severity.ERROR, exs )
        : new CheckException( config, Severity.DEBUG, new Exception( input.toString( ) ) );
    }
    
  }
  
  public static Function<ServiceStatusType, CheckException> transformToExceptions( ) {
    return StatusToCheckException.INSTANCE;
  }
  
  /**
   * @param config
   * @return
   */
  public static Collection<CheckException> lookup( final ServiceConfiguration config ) {
    final EntityTransaction db = Entities.get( CheckException.class );
    try {
      final List<CheckException> res = Entities.query( new CheckException( config.getName( ) ) );
      db.commit( );
      return res;
    } catch ( final Exception ex ) {
      LOG.error( "Failed to lookup error information for: " + config.getFullName( ), ex );
      db.rollback( );
    }
    return null;
  }
  
  public static void persist( final CheckException errors ) {
    if ( errors != null && Hosts.isCoordinator( ) ) {
      try {
        for ( final CheckException e : errors ) {
          final EntityTransaction db = Entities.get( CheckException.class );
          try {
            Entities.persist( e );
            db.commit( );
          } catch ( final Exception ex ) {
            LOG.error( "Failed to persist error information for: " + ex, ex );
            db.rollback( );
          }
        }
      } catch ( Exception ex ) {
        Logs.extreme( ).error( ex , ex );
      }
    }
  }
}
