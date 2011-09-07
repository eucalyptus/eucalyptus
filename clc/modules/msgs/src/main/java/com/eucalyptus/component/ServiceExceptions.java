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

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.ServiceChecks.CheckException;
import com.eucalyptus.component.ServiceChecks.Severity;
import com.eucalyptus.component.Topology.ServiceKey;
import com.eucalyptus.records.Logs;
import com.google.common.base.Predicate;

public class ServiceExceptions {
  
  private static Logger LOG = Logger.getLogger( ServiceExceptions.class );
  
  enum NoopErrorFilter implements Predicate<Throwable> {
    INSTANCE;
    
    @Override
    public boolean apply( final Throwable input ) {
      Logs.exhaust( ).error( input, input );
      LOG.trace( input.getMessage( ) );
      return true;
    }
    
  }
    
  /**
   * @param parent
   * @param ex
   * @param failureAction
   * @return true if the error is fatal and the transition should be aborted
   */
  public static final boolean filterExceptions( final ServiceConfiguration parent, final Throwable ex, final Predicate<Throwable> failureAction ) {
    if ( ex instanceof CheckException ) {//go through all the exceptions and look for things with Severity greater than or equal to ERROR
      CheckException checkExHead = ( CheckException ) ex;
      LOG.error( "Transition failed on " + parent.lookupComponent( ).getName( ) + " " + checkExHead.getSeverity( ) + " due to " + ex.toString( ), ex );
      for ( CheckException checkEx : checkExHead ) {
        LifecycleEvents.fireExceptionEvent( parent, checkEx.getSeverity( ), checkEx );
      }
      if( checkExHead.getSeverity( ).ordinal( ) >= Severity.ERROR.ordinal( ) ) {
        try {
          failureAction.apply( ex );
        } catch ( Exception ex1 ) {
          LOG.error( ex1, ex1 );
        }
        return true;
      }
      for ( CheckException checkEx : checkExHead ) {
        if( checkEx.getSeverity( ).ordinal( ) >= Severity.ERROR.ordinal( ) ) {
          try {
            failureAction.apply( ex );
          } catch ( Exception ex1 ) {
            LOG.error( ex1, ex1 );
          }
          return true;
        }
      }
      return false;
    } else {//treat generic exceptions as always being Severity.ERROR
      LOG.error( "Transition failed on " + parent.lookupComponent( ).getName( ) + " due to " + ex.toString( ), ex );
      LifecycleEvents.fireExceptionEvent( parent, Severity.ERROR, ex );
      try {
        failureAction.apply( ex );
      } catch ( Exception ex1 ) {
        LOG.error( ex1, ex1 );
      }
      return true;
    }
  }
  
  public static final boolean filterExceptions( final ServiceConfiguration parent, final Throwable ex ) {
    return filterExceptions( parent, ex, NoopErrorFilter.INSTANCE );
  }
  
  public static final Predicate<Throwable> maybeDisableService( final ServiceConfiguration parent ) {
    return new Predicate<Throwable>( ) {
      
      @Override
      public boolean apply( final Throwable ex ) {
        if ( State.ENABLED.equals( parent.lookupState( ) ) && ( parent.isVmLocal( ) || ( BootstrapArgs.isCloudController( ) && parent.isHostLocal( ) ) ) ) {
          try {
            Topology.disable( parent );
          } catch ( ServiceRegistrationException ex1 ) {
            LOG.error( "Transition failed on " + parent.lookupComponent( ).getName( ) + " due to " + ex.toString( ), ex );
          }
        }
        return true;
      }
      
    };
  }
}
