/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow.stateful;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
import com.google.common.base.Objects;
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
          addPoller( poll.getChannel( ), new Poller( poll.getChannel( ), poll.getCorrelationId( ) ) );
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
      return Objects.toStringHelper( this )
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
      return Objects.toStringHelper( this )
          .add( "channel", getChannel() )
          .add( "details", getDetails() )
          .add( "timestamp", getTimestamp() )
          .toString( );
    }
  }

  private static final class Poller {
    private static final long EXPIRY_MILLIS = TimeUnit.SECONDS.toMillis( 30 );

    private final long timestamp;
    private final String channel;
    private final String correlationId;
    private final CheckedListenableFuture<PollForNotificationResponseType> future;

    private Poller( final String channel,
                    final String correlationId ) {
      this.timestamp = System.currentTimeMillis( );
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

    public long getTimestamp() {
      return timestamp;
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
      return ( timestamp + EXPIRY_MILLIS ) < time;
    }

    public String toString( ) {
      return Objects.toStringHelper( this )
        .add( "channel", getChannel( ) )
        .add( "correlationId", getCorrelationId( ) )
        .toString();
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
