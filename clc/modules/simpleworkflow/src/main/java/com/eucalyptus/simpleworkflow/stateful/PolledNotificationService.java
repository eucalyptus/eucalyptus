/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.eucalyptus.simpleworkflow.common.stateful.PolledNotificationChecker;
import com.eucalyptus.simpleworkflow.common.stateful.PolledNotifications;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.local.LocalChannel;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/**
 *
 */
@ComponentNamed
public class PolledNotificationService {
  private static final Logger logger = Logger.getLogger( PolledNotificationService.class );

  private static final ConcurrentMap<String,Pollers> pollersByChannel = new ConcurrentHashMap<>( );
  private static final ConcurrentMap<String,PendingNotification> pendingNotificationsByChannel =
      new ConcurrentHashMap<>( );
  private static final PolledNotificationChecker checker = new PolledNotificationChecker(){
    @Override
    public boolean apply( final String channel ) {
      return Predicates.or( PolledNotificationCheckerDiscovery.supplier( ).get( ) ).apply( channel );
    }
  };

  public NotifyResponseType submitNotify( final NotifyType notify ) throws EucalyptusCloudException {
    final NotifyResponseType response = notify.getReply( );
    final Context context = Contexts.lookup( );
    if ( context.hasAdministrativePrivileges( ) ) {
      if ( !notifyPollers( notify.getChannel( ), notify.getDetails( ) ) ) {
        pendingNotificationsByChannel.put( notify.getChannel( ), new PendingNotification( notify.getChannel( ), notify.getDetails() ) );
      }
    }
    return response;
  }

  public PollForNotificationResponseType pollForNotification( final PollForNotificationType poll ) throws EucalyptusCloudException {
    final Context context = Contexts.lookup( );
    if ( context.hasAdministrativePrivileges( ) ) {
      final Future<PollForNotificationResponseType> response =
          addPoller( poll.getChannel( ), new Poller( poll.getChannel( ), poll.getCorrelationId( ), poll.getTimeout() ) );
      checkNotify( poll.getChannel( ) );
      try {
        return response.get( );
      } catch ( final Exception e ) {
        return poll.getReply( );
      }
    } else {
      return poll.getReply( );
    }
  }

  private static void checkNotify( final String channel ) {
    final PendingNotification pendingNotification = pendingNotificationsByChannel.remove( channel );
    if ( pendingNotification != null && !pendingNotification.isExpired( System.currentTimeMillis( ) ) ) {
      notifyPollers( channel, pendingNotification.getDetails( ) );
    } else if ( checker.apply( channel ) ) {
      notifyPollers( channel, null );
    }
  }

  private static boolean notifyPollers( final String channel, final String details ) {
    return getPollers( channel ).notifyPollers( details );
  }

  private static Future<PollForNotificationResponseType> addPoller( final String channel, final Poller poller ) {
    getPollers( channel ).addPoller( poller );
    return poller.getFuture();
  }

  private static Pollers getPollers( final String channel ) {
    Pollers pollers = pollersByChannel.get( channel );
    if ( pollers != null ) {
      pollers.touch( );
      // get again to ensure not evicted due to expiry before touched
      pollers = pollersByChannel.get( channel );
    }
    if ( pollers == null ) {
      pollersByChannel.putIfAbsent( channel, new Pollers( channel ) );
      pollers = pollersByChannel.get( channel );
    }
    return pollers;
  }

  static void evacuate( ) {
    timeoutPollers( Long.MAX_VALUE );
  }

  private static void periodicWork( ) {
    final long time = System.currentTimeMillis();
    timeoutPollers( time );
    timeoutPollerMetadata( time );
    timeoutPendingNotifications( time );
  }

  private static void timeoutPollers( final long time ) {
    for ( final Pollers pollers : pollersByChannel.values( ) ) {
      pollers.notifyExpiredPollers( time );
    }
  }

  private static void timeoutPollerMetadata( final long time ) {
    for ( final Pollers pollers : pollersByChannel.values( ) ) {
      if ( pollers.isEmpty( ) && pollers.isExpired( time ) ) {
        pollersByChannel.remove( pollers.getChannel( ), pollers );
      }
    }
  }

  private static void timeoutPendingNotifications( final long time ) {
    for ( final PendingNotification pendingNotification : pendingNotificationsByChannel.values( ) ) {
      if ( pendingNotification.isExpired( time ) ) {
        pendingNotificationsByChannel.remove( pendingNotification.getChannel( ), pendingNotification );
      }
    }
  }

  private static final class Pollers {
    private static final long EXPIRY_MILLIS = TimeUnit.MINUTES.toMillis( 2 );

    private final AtomicLong timestamp = new AtomicLong( System.currentTimeMillis( ) );
    private final BlockingQueue<Poller> pollersQueue = new LinkedBlockingDeque<>();
    private final String channel;

    private Pollers( final String channel ) {
      this.channel = channel;
    }

    public String getChannel( ) {
      return channel;
    }

    public void addPoller( final Poller poller ) {
      touch( );
      pollersQueue.remove( poller );
      pollersQueue.add( poller );
    }

    public boolean notifyPollers( final String details ) {
      return notifyPollers( queuedPollers( ), new Predicate<PollForNotificationResponseType>() {
        @Override
        public boolean apply( final PollForNotificationResponseType response ) {
          response.setNotified( true );
          response.setDetails( details );
          return true;
        }
      } );
    }

    public void notifyExpiredPollers( long time ) {
      notifyPollers( expiredPollers( time ), new Predicate<PollForNotificationResponseType>() {
        @Override
        public boolean apply( final PollForNotificationResponseType response ) {
          response.setNotified( false );
          return true;
        }
      } );
    }

    public boolean isEmpty( ) {
      return pollersQueue.isEmpty( );
    }

    public boolean isExpired( final long time ) {
      return ( timestamp.get( ) + EXPIRY_MILLIS ) < time;
    }

    private boolean notifyPollers( final Iterable<Poller> pollers,
                                   final Predicate<PollForNotificationResponseType> responsePredicate ) {
      touch( );
      boolean notified = false;
      for ( final Poller poller : pollers ) try {
        final PollForNotificationResponseType response = new PollForNotificationResponseType( );
        response.setCorrelationId( poller.getCorrelationId( ) );
        if ( responsePredicate.apply( response ) ) {
          poller.response( response );
          notified = true;
        }
      } catch ( final Exception e ){
        logger.error( "Error notifying poller " + poller, e );
      }
      return notified;
    }

    private Iterable<Poller> queuedPollers( ) {
      final List<Poller> pollers = Lists.newArrayList( );
      pollersQueue.drainTo( pollers );
      return pollers;
    }

    private Iterable<Poller> expiredPollers( final long time ) {
      final List<Poller> pollers = Lists.newArrayList();
      for ( final Poller poller : pollersQueue ) {
        if ( poller.isExpired( time ) && pollersQueue.remove( poller ) ) {
          pollers.add( poller );
        }
      }
      return pollers;
    }

    private void touch( ) {
      timestamp.set(  System.currentTimeMillis( ) );
    }

    public String toString( ) {
      return MoreObjects.toStringHelper( this )
          .add( "channel", getChannel( ) )
          .add( "pollers", pollersQueue )
          .add( "timestamp", timestamp.get() )
          .toString();
    }
  }

  private static final class PendingNotification {
    private static final long EXPIRY_MILLIS = TimeUnit.MINUTES.toMillis( 1 );
    private final long timestamp;
    private final String channel;
    private final String details;

    private PendingNotification( final String channel,
                                 final String details ) {
      this.timestamp = System.currentTimeMillis( );
      this.channel = channel;
      this.details = details;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public String getChannel() {
      return channel;
    }

    public String getDetails() {
      return details;
    }

    public boolean isExpired( final long time ) {
      return ( timestamp + EXPIRY_MILLIS ) < time;
    }

    public String toString( ) {
      return MoreObjects.toStringHelper( this )
          .add( "channel", getChannel() )
          .add( "details", getDetails() )
          .add( "timestamp", getTimestamp() )
          .toString( );
    }
  }

  private static final class Poller {
    private static final long EXPIRY_MILLIS = TimeUnit.SECONDS.toMillis( 30 );

    private long timeout;
    private final String channel;
    private final String correlationId;
    private final CheckedListenableFuture<PollForNotificationResponseType> future;

    private Poller( final String channel,
                    final String correlationId,
                    Long timeout) {
      if (timeout == null)
        this.timeout = System.currentTimeMillis() +  EXPIRY_MILLIS;
      else
        this.timeout = timeout;
      this.channel = channel;
      this.correlationId = correlationId;
      this.future = Futures.newGenericeFuture( );
      try { //TODO:STEVE: clean up async for local transport
        if ( !(Contexts.lookup( correlationId ).getChannel( ) instanceof LocalChannel) ) {
          future.set( null );
        }
      } catch ( NoSuchContextException e ) {
        // leave future unset
      }
    }

    public String getChannel() {
      return channel;
    }

    public String getCorrelationId() {
      return correlationId;
    }

    public Future<PollForNotificationResponseType> getFuture() {
      return future;
    }

    public void response( final PollForNotificationResponseType response ) {
      if ( !future.isDone( ) ) {
        future.set( response );
      } else {
        Contexts.response( response );
      }
    }

    public boolean isExpired( final long time ) {
      return timeout < time;
    }

    public String toString( ) {
      return MoreObjects.toStringHelper( this )
        .add( "channel", getChannel( ) )
        .add( "correlationId", getCorrelationId( ) )
        .toString();
    }

    @Override
    public boolean equals(final Object other) {
      if (other == null)
        return false;
      if (!(other instanceof Poller))
        return false;
      final Poller otherPoller = (Poller) other;
      if (this.getChannel()!= null) {
        if (! this.getChannel().equals(otherPoller.getChannel())) {
          return false;
        }
      }
      if (this.getCorrelationId()!=null) {
        if (! this.getCorrelationId().equals(otherPoller.getCorrelationId())) {
          return false;
        }
      }
      return true;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode( );
      result = prime * result + ( (this.channel == null )  ? 0 : this.channel.hashCode() );
      result = prime * result + ( (this.correlationId == null) ? 0 : this.correlationId.hashCode() );
      return result;
    }
  }

  public static class PollerClockTickEventListener implements EventListener<ClockTick> {
    public static void register( ) {
      Listeners.register( ClockTick.class, new PollerClockTickEventListener() );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if ( Bootstrap.isOperational( ) ) {
        if ( !Topology.isEnabledLocally( PolledNotifications.class ) ) {
          PolledNotificationService.evacuate();
        }
        PolledNotificationService.periodicWork( );
      }
    }
  }
}
