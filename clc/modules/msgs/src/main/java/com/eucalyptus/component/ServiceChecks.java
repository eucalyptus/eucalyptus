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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.context.ServiceStateException;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Arrays;

public class ServiceChecks {
  
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
    
    public CheckException transform( ServiceConfiguration config, Throwable... ts ) {
      return transform( config, Arrays.asList( ts ) );
    }
    
    public CheckException transform( ServiceConfiguration config, List<Throwable> ts ) {
      List<CheckException> exs = Lists.transform( ts, getExceptionMapper( this, config ) );
      CheckException last = chainCheckExceptions( exs );
      return last;
    }
  };
  
  private static Function<Throwable, CheckException> getExceptionMapper( final Severity severity, final ServiceConfiguration config ) {
    return new Function<Throwable, CheckException>( ) {
      
      @Override
      public CheckException apply( Throwable input ) {
        return newServiceCheckException( severity, config, input );
      }
    };
  }
  
  public static CheckException chainCheckExceptions( List<CheckException> exs ) {
    CheckException last = null;
    for ( CheckException ex : Lists.reverse( exs ) ) {
      if ( last != null ) {
        ex.addOtherException( ex );
      }
      last = ex;
    }
    return last;
  }
  
  public static class Functions {
    private static final Function<CheckException, ServiceCheckRecord> CHECK_EX_TO_RECORD = new Function<CheckException, ServiceCheckRecord>( ) {
                                                                                           
                                                                                           @Override
                                                                                           public ServiceCheckRecord apply( CheckException input ) {
                                                                                             return new ServiceCheckRecord( input );
                                                                                           }
                                                                                           
                                                                                         };
    
    public static Function<ServiceStatusType, List<CheckException>> statusToCheckExceptions( final String correlationId ) {
      return new Function<ServiceStatusType, List<CheckException>>( ) {
        @Override
        public List<CheckException> apply( ServiceStatusType input ) {
          List<CheckException> exs = Lists.newArrayList( );
          for ( String detail : input.getDetails( ) ) {
            ServiceConfiguration config = TypeMappers.transform( input.getServiceId( ), ServiceConfiguration.class );
            CheckException ex = newServiceCheckException( correlationId, Severity.ERROR, config, new ServiceStateException( detail ) );
            exs.add( ex );
          }
          return exs;
        }
      };
    }
    
    public static Function<CheckException, ServiceCheckRecord> checkExToRecord( ) {
      return CHECK_EX_TO_RECORD;
    }
    
    public static Function<ServiceStatusType, List<ServiceCheckRecord>> statusToEvents( final String correlationId ) {
      return new Function<ServiceStatusType, List<ServiceCheckRecord>>( ) {
        
        @Override
        public List<ServiceCheckRecord> apply( ServiceStatusType input ) {
          List<ServiceCheckRecord> events = Lists.newArrayList( );
          for ( CheckException ex : statusToCheckExceptions( correlationId ).apply( input ) ) {
            events.add( new ServiceCheckRecord( ex ) );
          }
          return events;
        }
      };
    }
    
  }
  
  static ServiceCheckRecord createRecord( ServiceConfiguration config, String message ) {
    return createRecord( UUID.randomUUID( ).toString( ), config, new RuntimeException( message ) );
  }
  
  static ServiceCheckRecord createRecord( ServiceConfiguration config, Throwable t ) {
    return createRecord( UUID.randomUUID( ).toString( ), config, t );
  }
  
  public static ServiceCheckRecord createRecord( String correlationId, ServiceConfiguration config, Throwable t ) {//TODO:GRZE:FIX VISIBILITY HERE?!?!?!
    //TODO:GRZE: exception filtering here
    return new ServiceCheckRecord( correlationId, Severity.ERROR.transform( config, t ) );
  }
  
  private static CheckException newServiceCheckException( Severity severity, ServiceConfiguration config, Throwable t ) {
    return newServiceCheckException( null, severity, config, t );
  }
  
  private static CheckException newServiceCheckException( String correlationId, Severity severity, ServiceConfiguration config, Throwable t ) {
    if ( t instanceof Error ) {
      return new CheckException( correlationId, t, Severity.FATAL, config );
    } else if ( Severity.WARNING.ordinal( ) > severity.ordinal( ) && t instanceof RuntimeException ) {
      return new CheckException( correlationId, t, Severity.WARNING, config );
    } else if ( t instanceof CheckException ) {
      return new CheckException( correlationId, t, severity, config );
    } else {
      return new CheckException( correlationId, t, Severity.DEBUG, config );
    }
  }
  
  public static class CheckException extends Exception implements Iterable<CheckException> {
    private static Logger              LOG = Logger.getLogger( CheckException.class );
    private final Severity             severity;
    private final ServiceConfiguration config;
    private final Date                 timestamp;
    private final String               uuid;
    private final String               correlationId;
    private final int                  eventEpoch;
    private final Component.State      eventState;
    private CheckException             other;
    
    private CheckException( String correlationId, Throwable cause, Severity severity, ServiceConfiguration config ) {
      super( cause.getMessage( ) );
      if ( cause instanceof CheckException ) {
        this.setStackTrace( cause.getStackTrace( ) );
      } else {
        this.initCause( cause );
      }
      this.severity = severity;
      this.config = config;
      this.uuid = uuid( cause );
      this.correlationId = ( correlationId == null
        ? this.uuid
        : correlationId );
      this.timestamp = new Date( );
      this.eventState = config.lookupStateMachine( ).getState( );
      this.eventEpoch = Topology.epoch( );
    }
    
    private static String uuid( Throwable cause ) {
      if ( cause != null && cause instanceof CheckException ) {
        return ( ( CheckException ) cause ).getUuid( );
      } else {
        return UUID.randomUUID( ).toString( );
      }
    }
    
    public Severity getSeverity( ) {
      return this.severity;
    }
    
    CheckException addOtherException( CheckException e ) {
      if ( this.other != null ) {
        this.other.addOtherException( e );
        return this;
      } else {
        this.other = e;
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
          return this.curr.other != null;
        }
        
        @Override
        public CheckException next( ) {
          return this.curr.other;
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
  
}
