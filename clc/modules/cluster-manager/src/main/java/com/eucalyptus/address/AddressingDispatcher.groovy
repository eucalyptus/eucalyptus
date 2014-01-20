package com.eucalyptus.address

import com.eucalyptus.cluster.ClusterConfiguration
import com.eucalyptus.util.Parameters
import com.eucalyptus.util.async.AsyncRequests
import com.eucalyptus.util.async.Request
import edu.ucsb.eucalyptus.msgs.BaseMessage

import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicReference

import static org.hamcrest.Matchers.notNullValue

/**
 *
 */
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
      }

      @Override
      def <Q extends BaseMessage, R extends BaseMessage> R sendSync(
          final Request<Q, R> request,
          final ClusterConfiguration cluster
      ) throws ExecutionException, InterruptedException {
        request.getCallback( ).fire( null )
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
      }

      @Override
      def <Q extends BaseMessage, R extends BaseMessage> R sendSync(
          final Request<Q, R> request,
          final ClusterConfiguration cluster
      ) throws ExecutionException, InterruptedException {
        request.sendSync( cluster )
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

  static void enable( Dispatcher dispatcher ) {
    Parameters.checkParam( "dispatcher", dispatcher, notNullValue( ) )
    AddressingDispatcher.dispatcher.set( dispatcher )
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
