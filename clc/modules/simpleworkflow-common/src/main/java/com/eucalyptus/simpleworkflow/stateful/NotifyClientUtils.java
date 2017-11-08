/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.simpleworkflow.stateful;

import com.ctc.wstx.exc.WstxEOFException;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.Consumer;
import com.eucalyptus.util.Consumers;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.ConnectionException;
import com.eucalyptus.util.concurrent.ListenableFuture;
import com.eucalyptus.ws.WebServicesException;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import org.apache.log4j.Logger;

import java.net.ConnectException;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

/**
 * Created by ethomas on 10/31/16.
 */
public class NotifyClientUtils {

  private static final Logger logger = Logger.getLogger( NotifyClientUtils.class );

  public static void notifyChannel(final ChannelWrapper channelWrapper) {
    final NotifyType notify = new NotifyType( );
    notify.setChannel(channelWrapper.getChannelName());
    try {
      final ListenableFuture<NotifyResponseType> dispatchFuture =
          AsyncRequests.dispatch(Topology.lookup(PolledNotifications.class), notify);
      dispatchFuture.addListener( new Runnable( ) {
        @Override
        public void run() {
          try {
            dispatchFuture.get( );
          } catch ( final InterruptedException e ) {
            logger.info( "Interrupted while sending notification for " + notify.getChannel(), e );
          } catch ( final ExecutionException e ) {
            logger.error( "Error sending notification for " + notify.getChannel( ), e );
            //TODO:STEVE: should retry notification?
          }
        }
      } );
    } catch ( final Exception e ) {
      logger.error( "Error sending notification for " + notify.getChannel( ), e );
    }
  }

  public static void pollChannel(final ChannelWrapper channelWrapper,
                                 final long timeout,
                                 final Consumer<Boolean> resultConsumer) throws Exception {
    final Consumer<Boolean> consumer = Consumers.once(resultConsumer);
    final PollForNotificationType poll = new PollForNotificationType( );
    poll.setChannel(channelWrapper.getChannelName());
    poll.setTimeout( timeout );

    if ( Bootstrap.isShuttingDown() ) {
      delayedPollFailure( 1000L, consumer );
      return;
    }

    final ServiceConfiguration polledNotificationsConfiguration;
    try {
      polledNotificationsConfiguration = Topology.lookup( PolledNotifications.class );
    } catch ( final NoSuchElementException e ){
      delayedPollFailure( 5000L, consumer );
      return;
    }

    final ListenableFuture<PollForNotificationResponseType> dispatchFuture =
        AsyncRequests.dispatch( polledNotificationsConfiguration, poll );
    dispatchFuture.addListener( new Runnable( ) {
      @Override
      public void run( ) {
        try {
          final PollForNotificationResponseType response = dispatchFuture.get( );
          consumer.accept(Objects.firstNonNull(response.getNotified(), false));
        } catch ( final InterruptedException e ) {
          logger.info( "Interrupted while polling for task " + poll.getChannel( ), e );
        } catch ( final ExecutionException e ) {
          if ( Bootstrap.isShuttingDown( ) ) {
            logger.info( "Error polling for task " + poll.getChannel( ) + ": " + Exceptions.getCauseMessage(e) );
          } else {
            handleExecutionExceptionForPolling(e, poll);
          }
        } catch ( final Exception e ) {
          logger.error( "Error polling for task " + poll.getChannel( ), e );
        } finally {
          consumer.accept( false );
        }
      }
    } );
  }

  private static void delayedPollFailure( final long delay,
                                          final Consumer<Boolean> consumer) {
    try {
      Thread.sleep( delay );
    } catch (InterruptedException e1) {
      Thread.currentThread( ).interrupt( );
    } finally {
      consumer.accept( false );
    }
  }

  private static void handleExecutionExceptionForPolling(ExecutionException e, PollForNotificationType poll) {
    Throwable cause = Throwables.getRootCause(e);
    // The following errors occur when the CLC is down or rebooting.
    // com.eucalyptus.ws.WebServicesException: Failed to marshall response:
    // com.eucalyptus.util.async.ConnectionException: Channel was closed before the response was received.:PollForNotificationType
    //java.net.ConnectException: Connection refused:
    // com.ctc.wstx.exc.WstxEOFException: Unexpected EOF in prolog
    // At this point, we just wait a couple of seconds to allow the CLC to reboot.  It will probably take more than a couple of seconds,
    // but this way we will also slow the rate of log error accrual, as otherwise this method is called again immediately.
    if (cause instanceof WebServicesException || cause instanceof ConnectionException || cause instanceof ConnectException || cause instanceof WstxEOFException) {
      logger.info("Error polling for task " + poll.getChannel() + ", CLC likely down.  Will sleep for 5 seconds");
      logger.info(cause.getClass() + ":" + cause.getMessage());
      try {
        Thread.sleep(5000L);
      } catch (InterruptedException e1) {
        logger.info("Interrupted while polling for task " + poll.getChannel(), e1);
      }
    } else {
      logger.error( "Error polling for task " + poll.getChannel( ), e );
    }
  }

  public interface ChannelWrapper {
    public String getChannelName();
  }
}
