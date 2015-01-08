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

package com.eucalyptus.util.async;

import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.google.common.base.Optional;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.CallerContext;

public class AsyncRequests {
  
  private static Logger LOG = Logger.getLogger( AsyncRequests.class );
  
  public static <A extends BaseMessage, B extends BaseMessage> CheckedListenableFuture<B> dispatch( final ServiceConfiguration config, final A msg ) throws Exception {
    if ( config.isVmLocal( ) ) {
      final CheckedListenableFuture<B> future = Futures.newGenericeFuture( );
      Threads.enqueue( Empyrean.class, AsyncRequests.class, new Callable<B>( ) {
        public B call( ) {
          try {
            B ret = ServiceContext.send( config.getComponentId( ), msg );
            future.set( ret );
          } catch ( Exception ex ) {
            future.setException( ex );
          }
          return null;
        }
      } );
      return future;
      
    } else {
      Request<A, B> req = newRequest( new MessageCallback<A, B>( ) {
        {
          this.setRequest( msg );
        }
        
        @Override
        public void fire( B msg ) {
          Logs.extreme( ).debug( msg.toSimpleString( ) );
        }
      } );
      return req.dispatch( config );
    }
  }

  public static <A extends BaseMessage, B extends BaseMessage> B sendSync( Class<? extends ComponentId> target, final A msg ) throws Exception {
    return sendSync( Topology.lookup( target ), Optional.<CallerContext>absent( ), msg );
  }

  public static <A extends BaseMessage, B extends BaseMessage> B sendSync( ServiceConfiguration config, final A msg ) throws Exception {
    return sendSync( config, Optional.<CallerContext>absent( ), msg );
  }

  public static <A extends BaseMessage, B extends BaseMessage> B sendSyncWithCurrentIdentity(
      final ServiceConfiguration config,
      final A msg
  ) throws Exception {
    return sendSync( config, Optional.of( new CallerContext( Contexts.lookup( ) ) ), msg );
  }

  private static <A extends BaseMessage, B extends BaseMessage> B sendSync(
      final ServiceConfiguration config,
      final Optional<CallerContext> callerContext,
      final A msg ) throws Exception {
    if ( callerContext.isPresent( ) ) {
      callerContext.get( ).apply( msg );
    }

    if ( config.isVmLocal( ) ) {
      return ServiceContext.send( config.getComponentId( ), msg );
    } else {
      try {
        Request<A, B> req = newRequest( new MessageCallback<A, B>( ) {
          {
            this.setRequest( msg );
          }
          
          @Override
          public void fire( B msg ) {
            Logs.extreme( ).debug( msg.toSimpleString( ) );
          }
        } );
        return req.sendSync( config );
      } catch ( InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
        throw ex;
      }
    }
  }

  /**
   * Calls {@link AsyncRequest#dispatch(String) dispatch} safely.
   *
   * <P>This method guarantees that either the success or failure methods of
   * the requests callback will be invoked.</P>
   *
   * @param request The request to dispatch
   * @param clusterOrPartition The cluster or partition to dispatch to
   * @deprecated
   * @see AsyncRequest#dispatch(String)
   */
  @Deprecated
  public static void dispatchSafely( final Request<?,?> request, final String clusterOrPartition ) {
    try {
      request.dispatch( clusterOrPartition );
    } catch ( NoSuchElementException e ) {
      // request not dispatched, invoke failure handler
      request.getCallback().fireException( e );
    }
  }  
  
  public static <A extends BaseMessage, B extends BaseMessage> Request<A, B> newRequest( final RemoteCallback<A, B> msgCallback ) {
    return new AsyncRequest<A,B>( msgCallback ) {
      {
        setRequest( msgCallback.getRequest( ) );
      }
    };
  }
  
}
