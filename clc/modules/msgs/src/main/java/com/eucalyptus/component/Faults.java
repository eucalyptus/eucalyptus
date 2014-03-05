/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.google.common.base.Joiner;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Component.Transition;
import com.eucalyptus.component.fault.FaultBuilderImpl;
import com.eucalyptus.component.fault.FaultSubsystemManager;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.empyrean.ServiceStatusDetail;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.records.Logs;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Emails;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.fsm.TransitionRecord;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@ConfigurableClass( root = "bootstrap.notifications",
                    description = "Parameters controlling the handling of service state notifications." )
public class Faults {
  private static Logger      LOG                    = Logger.getLogger( Faults.class );
  @ConfigurableField( description = "Email address where notifications are to be delivered." )
  public static String       EMAIL_TO;
  @ConfigurableField( description = "From email address used for notification delivery." )
  public static String       EMAIL_FROM             = "notification@eucalyptus";
  @ConfigurableField( description = "From email name used for notification delivery." )
  public static String       EMAIL_FROM_NAME        = "Eucalyptus Notifications";
  @ConfigurableField( description = "Email subject used for notification delivery." )
  public static final String EMAIL_SUBJECT_PREFIX   = "[eucalyptus-notifications] ";
  @ConfigurableField( description = "Interval (in seconds) during which a notification will be delayed to allow for batching events for delivery." )
  public static Integer      BATCH_DELAY_SECONDS    = 60;
  @ConfigurableField( description = "Send a system state digest periodically." )
  public static Boolean      DIGEST                 = Boolean.FALSE;
  @ConfigurableField( description = "If sending system state digests is set to true, then only send the digest when the system has failures to report." )
  public static Boolean      DIGEST_ONLY_ON_ERRORS  = Boolean.TRUE;
  @ConfigurableField( description = "Period (in hours) with which a system state digest will be delivered." )
  public static Integer      DIGEST_FREQUENCY_HOURS = 24;
  @ConfigurableField( description = "Period (in hours) with which a system state digest will be delivered." )
  public static Boolean      INCLUDE_FAULT_STACK    = Boolean.FALSE;
  
  enum NoopErrorFilter implements Predicate<Throwable> {
    INSTANCE;
    
    @Override
    public boolean apply( final Throwable input ) {
      Logs.exhaust( ).error( input, input );
      return true;
    }
    
  }
  
  @Entity
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
    @Column( name = "fault_service_full_name" )
    private final String          serviceFullName;
    @Column( name = "fault_timestamp" )
    private final Date            timestamp;
    @Column( name = "fault_msg_correlation_id" )
    private final String          correlationId;
    @Column( name = "fault_event_epoch" )
    private final Integer         eventEpoch;
    @Column( name = "fault_service_state" )
    private final Component.State eventState;
    @Column( name = "fault_stack_trace" )
    @Lob
    @Type(type="org.hibernate.type.StringClobType")
    private String                stackString;
    @Transient
    private CheckException        other;
    
    @SuppressWarnings( "unused" )
    public CheckException( ) {
      this( null );
    }
    
    private CheckException( final String serviceName ) {
      this.serviceName = serviceName;
      this.serviceFullName = null;
      this.severity = null;
      this.timestamp = null;
      this.correlationId = null;
      this.eventEpoch = null;
      this.eventState = null;
      this.stackString = null;
    }
    
    CheckException( final ServiceConfiguration config, final Severity severity, final Throwable cause ) {
      this( config, severity, cause, null );
    }
    
//    CheckException( final String correlationId, final Throwable cause, final Severity severity, final ServiceConfiguration config ) {
//      this( config, severity, cause, correlationId );
//    }
//    
    CheckException( final ServiceConfiguration config, final Severity severity, final Throwable cause, final String correlationId ) {
      super( cause != null ? cause.getMessage( )
                          : Exceptions.causeString( cause ) );
      if ( cause instanceof CheckException ) {
        this.initCause( cause );
        this.setStackTrace( cause.getStackTrace( ) );
      } else if ( cause != null ) {
        this.initCause( cause );
        this.fillInStackTrace( );
      }
      this.severity = severity;
      this.serviceName = config.getName( );
      this.serviceFullName = config.getFullName( ).toString( );
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
        CheckException next;
        {
          this.next = CheckException.this;
        }
        
        @Override
        public boolean hasNext( ) {
          return this.next != null;
        }
        
        @Override
        public CheckException next( ) {
          CheckException ret = this.next;
          this.next = ( ret != null ? ret.other
                                   : null );
          return ret;
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
    
    private String getServiceFullName( ) {
      return this.serviceFullName;
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
  public enum Severity implements Predicate<CheckException> {
    TRACE, //ignored
    DEBUG, //default: store
    INFO, //default: store, describe
    WARNING, //default: store, describe, ui, notification
    ERROR, //default: store, describe, ui, notification
    URGENT, //default: store, describe, ui, notification, alert
    FATAL;
    
    @Override
    public boolean apply( CheckException input ) {
      if ( input == null ) {
        return false;
      } else {
        for ( CheckException ex : input ) {
          if ( this.equals( ex.getSeverity( ) ) ) {
            return true;
          }
        }
        return false;
      }
    }
  }
  
  public enum Scope {
    SERVICE,
    HOST,
    NETWORK;
  }
  
  private static CheckException chain( final ServiceConfiguration config, final Severity severity, final List<? extends Throwable> exs ) {
    if ( exs == null || exs.isEmpty( ) ) {
      return new CheckException( config, Severity.TRACE, new NullPointerException( "Faults.chain called w/ empty list: " + exs ) );
    } else {
      try {
        CheckException last = null;
        for ( final Throwable ex : Lists.reverse( exs ) ) {
          if ( ( last != null ) && ( ex instanceof CheckException ) ) {
            last.other = ( CheckException ) ex;
          } else if ( ( last != null ) && !( ex instanceof CheckException ) ) {
            last.other = new CheckException( config, severity, ex );
          } else if ( last == null && ( ex instanceof CheckException ) ) {
            last = ( CheckException ) ex;
          } else {
            last = new CheckException( config, severity, ex );
          }
        }
        last = ( last != null
                             ? last
                             : new CheckException( config, Severity.TRACE, new NullPointerException( "Faults.chain called w/ empty list: " + exs ) ) );
        return last;
      } catch ( Exception ex ) {
        LOG.error( "Faults: error in processing previous error: " + ex );
        Logs.extreme( ).error( ex, ex );
        return new CheckException( config, Severity.ERROR, ex );
      }
    }
  }

  public static CheckException failure( final ServiceConfiguration config, final String... messages ) {
    return failure( config, new RuntimeException( Joiner.on( "\n" ).join( Arrays.asList( messages ) ) ) );
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

  public static CheckException advisory( final ServiceConfiguration config, final String... messages ) {
    return advisory( config, new RuntimeException( Joiner.on( "\n" ).join( Arrays.asList( messages ) ) ) );
  }

  public static CheckException advisory( final ServiceConfiguration config, final Throwable... exs ) {
    return advisory( config, Arrays.asList( exs ) );
  }
  
  public static CheckException fatal( final ServiceConfiguration config, final List<? extends Throwable> exs ) {
    return chain( config, Severity.FATAL, ( List<Throwable> ) exs );
  }
  
  @TypeMapper
  public enum StatusDetailExceptionRecordMapper implements Function<ServiceStatusDetail, CheckException> {
    INSTANCE;
    @Override
    public CheckException apply( final ServiceStatusDetail input ) {
      ServiceConfiguration config = null;
      final String serviceName = Strings.emptyToNull( input.getServiceName() );
      try {
        config = ServiceConfigurations.lookupByName( serviceName );
      } catch ( RuntimeException e ) {
        for ( Component c : Components.list( ) ) {
          for ( ServiceConfiguration s : c.services() ) {
            if ( serviceName.equals( s.getName() ) ) {
              config = s;
              break;
            }
          }
        }
        if(config==null){
          throw e;
        }
      }
      Severity severity = Severity.DEBUG;
      if ( input.getSeverity( ) != null ) {
        severity = Severity.valueOf( input.getSeverity( ) );
      }
      CheckException ex = new CheckException( config, severity, new Exception( input.toString( ) ), input.getUuid( ) );
      ex.stackString = input.getStackTrace( );
      return ex;
    }
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
      for ( final ServiceStatusDetail detail : input.getStatusDetails( ) ) {
        final CheckException ex = TypeMappers.transform( detail, CheckException.class );
        exs.add( ex );
      }
      if ( exs.isEmpty( ) ) {
        return new CheckException( config, Severity.DEBUG, new Exception( input.toString( ) ) );
      } else {
        return Faults.chain( config, Severity.ERROR, exs );
      }
    }
  }
  
  public static Function<ServiceStatusType, CheckException> transformToExceptions( ) {
    return StatusToCheckException.INSTANCE;
  }
  
  private static final ConcurrentMap<ServiceConfiguration, FaultRecord> serviceExceptions = Maps.newConcurrentMap( );
  private static final BlockingQueue<FaultRecord>                       errorQueue        = new LinkedTransferQueue<FaultRecord>( );
  
  private static class FaultRecord {
    private final ServiceConfiguration                                      serviceConfiguration;
    private final TransitionRecord<ServiceConfiguration, State, Transition> transitionRecord;
    private final CheckException                                            error;
    private final Component.State                                           finalState;
    
    private FaultRecord( ServiceConfiguration serviceConfiguration, TransitionRecord<ServiceConfiguration, State, Transition> transitionRecord,
                         CheckException error ) {
      super( );
      this.serviceConfiguration = serviceConfiguration;
      this.finalState = serviceConfiguration.lookupState( );
      this.transitionRecord = transitionRecord;
      this.error = error;
    }
    
    public ServiceConfiguration getServiceConfiguration( ) {
      return this.serviceConfiguration;
    }
    
    public TransitionRecord<ServiceConfiguration, State, Transition> getTransitionRecord( ) {
      return this.transitionRecord;
    }
    
    public CheckException getError( ) {
      return this.error;
    }
    
    private Component.State getFinalState( ) {
      return this.finalState;
    }
    
  }
  
  public static void flush( final ServiceConfiguration config ) {
    serviceExceptions.remove( config );
  }
  
  public static Collection<CheckException> lookup( final ServiceConfiguration config ) {
    FaultRecord record = serviceExceptions.get( config );
    if ( record != null ) {
      return Lists.newArrayList( record.getError( ) );
    } else {
      return Lists.newArrayList( );
    }
  }
  
  public static void submit( final ServiceConfiguration parent, TransitionRecord<ServiceConfiguration, State, Transition> transitionRecord, final CheckException errors ) {
    FaultRecord record = new FaultRecord( parent, transitionRecord, errors );
    serviceExceptions.put( parent, record );
    if ( errors != null && BootstrapArgs.isCloudController( ) && Bootstrap.isFinished( ) ) {
      errorQueue.offer( record );
    }
  }
  
  public static class FaultNotificationHandler implements EventListener<Hertz>, Callable<Boolean> {
    private static final AtomicBoolean ready      = new AtomicBoolean( true );
    private static final AtomicLong    lastDigest = new AtomicLong( System.currentTimeMillis( ) );
    
    public static void register( ) {
      Listeners.register( Hertz.class, new FaultNotificationHandler( ) );
    }
    
    @Override
    public void fireEvent( final Hertz event ) {
      if ( Bootstrap.isOperational( ) && event.isAsserted( Faults.BATCH_DELAY_SECONDS ) && ready.compareAndSet( true, false ) ) {
        try {
          Threads.enqueue( Eucalyptus.class, Faults.class, this );
        } catch ( final Exception ex ) {
          ready.set( true );
        }
      }
    }
    
    @Override
    public Boolean call( ) throws Exception {
      try {
        sendFaults( );
        sendDigest( );
      } finally {
        ready.set( true );
      }
      return true;
    }
    
    private static void sendDigest( ) {
      if ( Hosts.isCoordinator( ) && Faults.DIGEST ) {
        long lastTime = lastDigest.getAndSet( System.currentTimeMillis( ) );
        if ( ( lastDigest.get( ) - lastTime ) > Faults.DIGEST_FREQUENCY_HOURS * 60 * 60 * 1000 ) {
          Date digestDate = new Date( lastDigest.get( ) );
          if ( !serviceExceptions.isEmpty( ) || !Faults.DIGEST_ONLY_ON_ERRORS ) {
            LOG.debug( "Fault notifications: preparing digest for " + digestDate + "." );
            try {
              String subject = Faults.EMAIL_SUBJECT_PREFIX + " system state for " + digestDate;
              String result = Groovyness.run( SubDirectory.SCRIPTS, "notifications_digest" );
              if ( !Strings.isNullOrEmpty( result ) ) {
                dispatchEmail( subject, result );
              }
            } catch ( Exception ex ) {
              LOG.error( "Fault notifications: rendering digest failed: " + ex.getMessage( ) );
              Logs.extreme( ).error( ex, ex );
            }
          } else {
            LOG.debug( "Fault notifications: skipping digest for " + digestDate + "." );
          }
        } else {
          lastDigest.set( lastTime );
        }
      }
    }
    
    private static void sendFaults( ) {
      LOG.debug( "Fault notifications: waking up to service error queue." );
      final List<FaultRecord> pendingFaults = Lists.newArrayList( );
      errorQueue.drainTo( pendingFaults );
      if ( pendingFaults.isEmpty( ) ) {
        LOG.debug( "Fault notifications: service error queue is empty... going back to sleep." );
      } else {
        if ( Hosts.isCoordinator( ) ) {
          String subject = Faults.EMAIL_SUBJECT_PREFIX;
          List<FaultRecord> noStateChange = Lists.newArrayList( );
          List<FaultRecord> stateChange = Lists.newArrayList( );
          for ( FaultRecord f : pendingFaults ) {
            TransitionRecord<ServiceConfiguration, State, Transition> tr = f.getTransitionRecord( );
            if ( tr.getRule( ).getFromState( ).equals( f.getFinalState( ) ) ) {
              noStateChange.add( f );
            } else {
              stateChange.add( f );
              subject += " " + f.getServiceConfiguration( ).getName( ) + "->" + f.getFinalState( );
            }
          }
          if ( stateChange.isEmpty( ) ) {
            LOG.debug( "Fault notifications: no state changes pending, discarding pending faults" );
          } else {
            try {
              String result = Groovyness.run( SubDirectory.SCRIPTS, "notifications", new HashMap( ) {
                {
                  this.put( "faults", pendingFaults );
                }
              } );
              if ( !Strings.isNullOrEmpty( result ) ) {
                dispatchEmail( subject, result );
              }
            } catch ( Exception ex ) {
              LOG.error( "Fault notifications: rendering notification failed: " + ex.getMessage( ) );
              Logs.extreme( ).error( ex, ex );
            }
          }
        }
      }
    }
    
    public static void dispatchEmail( String subject, String result ) {
      LOG.debug( "From: " + Faults.EMAIL_FROM_NAME + " <" + Faults.EMAIL_FROM + ">" );
      LOG.debug( "To: " + Faults.EMAIL_TO );
      LOG.debug( "Subject: " + subject );
      LOG.debug( result );
      if ( !Strings.isNullOrEmpty( Faults.EMAIL_TO ) ) {
        Emails.send( Faults.EMAIL_FROM, Faults.EMAIL_FROM_NAME, Faults.EMAIL_TO, subject, result );
      }
    }
  }
  
  private static final ConcurrentMap<ServiceConfiguration, CheckException> failstopExceptions = Maps.newConcurrentMap( );
  
  public static void flush( ) {
    failstopExceptions.clear( );
  }
  
  public static void failstop( ServiceConfiguration key, CheckException checkEx ) {
    for ( CheckException ex : checkEx ) {
      if ( Severity.FATAL.equals( ex.getSeverity( ) ) ) {
        LOG.warn( "FAILSTOP: " + key.getFullName( ) + "=> " + checkEx.getMessage( ) );
        failstopExceptions.put( key, checkEx );
        return;
      }
    }
  }
  
  public static boolean isFailstop( ) {
    return !failstopExceptions.isEmpty( );
  }
  
  private static final FaultSubsystemManager faultSubsystemManager = new FaultSubsystemManager();
  public static void init() {
		faultSubsystemManager.init();
  }
  public static FaultBuilder forComponent(Class <? extends ComponentId> componentIdClass) {
    return new FaultBuilderImpl(faultSubsystemManager, componentIdClass);
  }
  public interface FaultBuilder {
		public FaultBuilder withVar(String name, String value);
		public FaultBuilder havingId(int faultId);
		public void log();
	}
}
