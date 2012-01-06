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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import org.mule.util.queue.QueueConfiguration;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentId.ServiceOperation;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.context.ServiceDispatchException;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Timers;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.util.RequestQueue;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class ServiceOperations {
  private static Logger                                                  LOG               = Logger.getLogger( ServiceOperations.class );
  private static final Map<Class<? extends BaseMessage>, Function<?, ?>> serviceOperations = Maps.newHashMap( );
  private static Boolean                                                 ASYNCHRONOUS      = Boolean.FALSE;                               //TODO:GRZE: @Configurable  
  @SuppressWarnings( "unchecked" )
  public static <T extends BaseMessage, I, O> Function<I, O> lookup( final Class<T> msgType ) {
    return ( Function<I, O> ) serviceOperations.get( msgType );
  }
  
  public static class ServiceOperationDiscovery extends ServiceJarDiscovery {
    
    public ServiceOperationDiscovery( ) {
      super( );
    }
    
    @SuppressWarnings( { "synthetic-access", "unchecked" } )
    @Override
    public boolean processClass( final Class candidate ) throws Exception {
      if ( Ats.from( candidate ).has( ServiceOperation.class ) && Function.class.isAssignableFrom( candidate ) ) {
        final ServiceOperation opInfo = Ats.from( candidate ).get( ServiceOperation.class );
        final Function<?, ?> op = ( Function<?, ?> ) Classes.newInstance( candidate );
        final List<Class> msgTypes = Classes.genericsToClasses( op );
        LOG.info( "Registered @ServiceOperation:       " + msgTypes.get( 0 ).getSimpleName( )
                  + ","
                  + msgTypes.get( 1 ).getSimpleName( )
                  + " => "
                  + candidate );
        serviceOperations.put( msgTypes.get( 0 ), op );
        return true;
      } else {
        return false;
      }
    }
    
    @Override
    public Double getPriority( ) {
      return 0.3d;
    }
    
  }
  
  @SuppressWarnings( "unchecked" )
  public static <I extends BaseMessage, O extends BaseMessage> void dispatch( final I request ) {
    if ( !serviceOperations.containsKey( request.getClass( ) ) || !StackConfiguration.OOB_INTERNAL_OPERATIONS ) {
      try {
        ServiceContext.dispatch( RequestQueue.ENDPOINT, request );
      } catch ( Exception ex ) {
        Contexts.responseError( request.getCorrelationId( ), ex );
      }
    } else {
      try {
        final Context ctx = Contexts.lookup( request.getCorrelationId( ) );
        final Function<I, O> op = ( Function<I, O> ) serviceOperations.get( request.getClass( ) );
        Timers.loggingWrapper( new Callable( ) {
          
          @Override
          public Object call( ) throws Exception {
            if ( StackConfiguration.ASYNC_INTERNAL_OPERATIONS ) {
              Threads.enqueue( Empyrean.class, ServiceOperations.class, new Callable<Boolean>( ) {
                
                @Override
                public Boolean call( ) {
                  executeOperation( request, ctx, op );
                  return Boolean.TRUE;
                }
              } );
            } else {
              executeOperation( request, ctx, op );
            }
            return null;
          }

          @Override
          public String toString( ) {
            return super.toString( );
          }
          
        } ).call( );
      } catch ( final Exception ex ) {
        Logs.extreme( ).error( ex, ex );
        Contexts.responseError( request.getCorrelationId( ), ex );
      }
    }
  }
  
  private static <I extends BaseMessage, O extends BaseMessage> void executeOperation( final I request, final Context ctx, final Function<I, O> op ) {
    Contexts.threadLocal( ctx );
    try {
      final O reply = op.apply( request );
      Contexts.response( reply );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      Contexts.responseError( request.getCorrelationId( ), ex );
    } finally {
      Contexts.removeThreadLocal( );
    }
  }
  
}
