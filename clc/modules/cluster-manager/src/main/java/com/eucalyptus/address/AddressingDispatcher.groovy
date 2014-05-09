/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.address

import com.eucalyptus.cluster.ClusterConfiguration
import com.eucalyptus.util.Parameters
import com.eucalyptus.util.async.AsyncRequests
import com.eucalyptus.util.async.Request
import edu.ucsb.eucalyptus.msgs.BaseMessage

import javax.annotation.Nonnull
import javax.annotation.Nullable
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicReference

import static org.hamcrest.Matchers.notNullValue

@SuppressWarnings("UnnecessaryQualifiedReference")
class AddressingDispatcher {

  enum Dispatcher {
    /**
     * Shortcut dispatcher does not send any messages.
     */
    SHORTCUT {
      @Override
      def <Q extends BaseMessage, R extends BaseMessage> void dispatch(
          final Request<Q, R> request,
          final String clusterOrPartition
      ) {
        request.getCallback( ).fire( (R) request.getRequest().getReply( ) )
        AddressingDispatcher.interceptor.get().onDispatch( request, clusterOrPartition )
      }

      @Override
      def <Q extends BaseMessage, R extends BaseMessage> R sendSync(
          final Request<Q, R> request,
          final ClusterConfiguration cluster
      ) throws ExecutionException, InterruptedException {
        request.getCallback( ).fire( null )
        AddressingDispatcher.interceptor.get().onSendSync( request, cluster )
        null
      }
    },

    /**
     * Standard dispatcher sends messages to back end
     */
    STANDARD {
      @Override
      def <Q extends BaseMessage, R extends BaseMessage> void dispatch(
          final Request<Q, R> request,
          final String clusterOrPartition
      ) {
        AsyncRequests.dispatchSafely( request, clusterOrPartition )
        AddressingDispatcher.interceptor.get().onDispatch( request, clusterOrPartition )
      }

      @Override
      def <Q extends BaseMessage, R extends BaseMessage> R sendSync(
          final Request<Q, R> request,
          final ClusterConfiguration cluster
      ) throws ExecutionException, InterruptedException {
        R result = request.sendSync( cluster )
        AddressingDispatcher.interceptor.get().onSendSync( request, cluster )
        result
      }
    }

    def <Q extends BaseMessage, R extends BaseMessage> void dispatch(
        final Request<Q, R> request,
        final String clusterOrPartition
    ) { }

    def <Q extends BaseMessage, R extends BaseMessage> R sendSync(
        final Request<Q, R> request,
        final ClusterConfiguration cluster
    ) throws ExecutionException, InterruptedException {
      null
    }
  }

  private static final AtomicReference<Dispatcher> dispatcher = new AtomicReference<Dispatcher>( Dispatcher.STANDARD )
  private static final AtomicReference<AddressingInterceptor> interceptor = new AtomicReference<AddressingInterceptor>( new AddressingInterceptorSupport() )

  public static interface AddressingInterceptor {
    void onDispatch( Request<? extends BaseMessage,? extends BaseMessage> request, String clusterOrPartition )
    void onSendSync( Request<? extends BaseMessage,? extends BaseMessage> request, ClusterConfiguration cluster )
  }

  public static class AddressingInterceptorSupport implements AddressingInterceptor {
    void onDispatch( Request<? extends BaseMessage,? extends BaseMessage> request, String clusterOrPartition  ){ onMessage( request, clusterOrPartition ) }
    void onSendSync( Request<? extends BaseMessage,? extends BaseMessage> request, ClusterConfiguration cluster ){ onMessage( request, cluster.partition ) }
    protected void onMessage( Request<? extends BaseMessage,? extends BaseMessage> request, String partition ) {  }
  }

  static void configure( @Nonnull Dispatcher dispatcher,
                         @Nullable AddressingInterceptor interceptor ) {
    Parameters.checkParam( "dispatcher", dispatcher, notNullValue( ) )
    AddressingDispatcher.dispatcher.set( dispatcher )
    AddressingDispatcher.interceptor.set( interceptor?:new AddressingInterceptorSupport( ) )
  }

  static <Q extends BaseMessage, R extends BaseMessage> void dispatch(
      final Request<Q, R> request,
      final String clusterOrPartition ) {
    dispatcher.get().dispatch( request, clusterOrPartition )
  }

  static <Q extends BaseMessage, R extends BaseMessage> R sendSync(
      final Request<Q, R> request,
      final ClusterConfiguration cluster
  ) throws ExecutionException, InterruptedException {
    dispatcher.get().sendSync( request, cluster )
  }

}
