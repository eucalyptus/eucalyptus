/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util.async;

import java.util.concurrent.ExecutionException;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.records.Logs;
import com.google.common.base.Optional;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.CallerContext;

public class AsyncRequests {
  public static <A extends BaseMessage, B extends BaseMessage> CheckedListenableFuture<B> dispatch( final ServiceConfiguration config, final A msg ) throws Exception {
    if ( config.isVmLocal( ) ) {
      final CheckedListenableFuture<B> future = Futures.newGenericeFuture( );
      ServiceContext.<B>send( config.getComponentId( ), msg ).whenComplete( ( B response, Throwable throwable ) -> {
        if ( throwable != null ) {
          future.setException( throwable );
        } else {
          future.set( response );
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
      try {
        return ServiceContext.<B>send( config.getComponentId( ), msg ).get( );
      } catch ( ExecutionException e ) {
        if ( e.getCause( ) instanceof Exception ) {
          throw (Exception) e.getCause( );
        }
        throw e;
      }
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
      } catch ( ExecutionException e ) {
        if ( e.getCause( ) instanceof Exception ) {
          throw (Exception) e.getCause( );
        }
        throw e;
      }
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
