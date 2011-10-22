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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.component.Faults.NoopErrorFilter;
import com.eucalyptus.component.Faults.Severity;
import com.eucalyptus.component.ServiceChecks.CheckException;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class Faults {
  
  enum NoopErrorFilter implements Predicate<Throwable> {
    INSTANCE;
    
    @Override
    public boolean apply( final Throwable input ) {
      Logs.exhaust( ).error( input, input );
      return true;
    }
    
  }
  
  public static class CheckException extends RuntimeException implements Iterable<CheckException> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static Logger              LOG = Logger.getLogger( CheckException.class );
    private final Severity             severity;
    private final ServiceConfiguration config;
    private final Date                 timestamp;
    private final String               uuid;
    private final String               correlationId;
    private final int                  eventEpoch;
    private final Component.State      eventState;
    private CheckException             other;
    
    CheckException( final ServiceConfiguration config, final Severity severity, final String message ) {
      this( config, severity, message, null );
    }
    
    CheckException( final ServiceConfiguration config, final Severity severity, final String message, final Throwable cause ) {
      this( config, severity, message, cause, null );
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
      this.config = config;
      this.uuid = uuid( cause );
      this.correlationId = ( correlationId == null
        ? this.uuid
        : correlationId );
      this.timestamp = new Date( );
      this.eventState = config.lookupState( );
      this.eventEpoch = Topology.epoch( );
    }
    
    private static String uuid( final Throwable cause ) {
      if ( ( cause != null ) && ( cause instanceof CheckException ) ) {
        return ( ( CheckException ) cause ).getUuid( );
      } else {
        return UUID.randomUUID( ).toString( );
      }
    }
    
    public Severity getSeverity( ) {
      return this.severity;
    }
    
    static CheckException wrap( final CheckException ex, final Throwable throwable ) {
      return ( CheckException ) ( throwable instanceof CheckException
        ? throwable
        : new CheckException( ex.correlationId, throwable, ex.severity, ex.config ) );
    }
    
    CheckException append( final Throwable ex ) {
      if ( this.other != null ) {
        this.other.append( ex );
        return this;
      } else {
        this.other = wrap( this, ex );
        return this;
      }
    }
    
    @Override
    public Iterator<CheckException> iterator( ) {
      return new Iterator<CheckException>( ) {
        CheckException curr;
        {
          this.curr = CheckException.this.other;
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
    
    protected ServiceConfiguration getConfig( ) {
      return this.config;
    }
    
    public Date getTimestamp( ) {
      return this.timestamp;
    }
    
    public String getUuid( ) {
      return this.uuid;
    }
    
    public String getCorrelationId( ) {
      return this.correlationId;
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
      final CheckException checkExHead = ( CheckException ) ex;
      for ( final CheckException checkEx : checkExHead ) {
//        ServiceEvents.fireExceptionEvent( parent, checkEx.getSeverity( ), checkEx );
      }
      if ( checkExHead.getSeverity( ).ordinal( ) >= Severity.ERROR.ordinal( ) ) {
        try {
          failureAction.apply( ex );
        } catch ( final Exception ex1 ) {
          Logs.extreme( ).error( ex1, ex1 );
        }
        return true;
      }
      for ( final CheckException checkEx : checkExHead ) {
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
//      ServiceEvents.fireExceptionEvent( parent, Severity.ERROR, ex );
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
    STORE, LOG, DESCRIBE, UI, NOTIFY, ALERT
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
  
  private static CheckException chain( final ServiceConfiguration config, final Severity severity, final List<Throwable> exs ) {
    CheckException last = null;
    for ( final Throwable ex : Lists.reverse( exs ) ) {
      if ( ( last != null ) && ( ex instanceof CheckException ) ) {
        last.append( ex );
      } else if ( last == null ) {
        last = new CheckException( config, severity, ex );
      }
    }
    return last != null
      ? last
      : new CheckException( config, severity, new NullPointerException( ) );
  }
  
  public static CheckException failure( final ServiceConfiguration config, final Throwable... exs ) {
    return failure( config, Arrays.asList( exs ) );
  }
  
  public static CheckException failure( final ServiceConfiguration config, final List<? extends Throwable> exs ) {
    return chain( config, Severity.ERROR, ( List<Throwable> ) exs );
  }
  
  public static CheckException advisory( final ServiceConfiguration config, final List<Throwable> exs ) {
    return chain( config, Severity.INFO, exs );
  }
  
  public static CheckException advisory( final ServiceConfiguration config, final Throwable... exs ) {
    return advisory( config, Arrays.asList( exs ) );
  }
  
}
