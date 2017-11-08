/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudwatch.service;

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthContext;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.cloudwatch.common.msgs.CloudWatchMessage;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.context.ServiceAdvice;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.reporting.Counter;
import com.eucalyptus.reporting.Counter.Counted;
import com.eucalyptus.reporting.Counter.CounterSnapshot;
import com.eucalyptus.reporting.event.CloudWatchApiUsageEvent;
import com.eucalyptus.util.Internets;

/**
 *
 */
@ComponentNamed
public class CloudWatchServiceSensor extends ServiceAdvice {

  private static final Counter<CloudWatchMessage,Counted> counter =
      new Counter<>( 60_000, 15, CloudWatchServiceSensor::messageToCountedItem );

  @Override
  protected void afterService( @Nonnull final Object request, @Nullable final Object response ) throws Exception {
    if ( request instanceof CloudWatchMessage ) {
      counter.count( (CloudWatchMessage) request );
    }
  }

  private static Counted messageToCountedItem( final CloudWatchMessage message ) {
    final String action = message.getClass( ).getSimpleName( ).replaceAll( "(ResponseType|Type)$", "" );
    try {
      final AuthContext authContext = Contexts.lookup( message.getCorrelationId( ) ).getAuthContext( ).get( );
      return new Counted( authContext.getAccountNumber( ), action );
    } catch ( AuthException | NoSuchContextException ignore ) {
    }
    return null;
  }

  /**
   * Listener that fires periodically to send CloudWatchApiUsageEvents from sensor data.
   */
  public static final class CloudWatchApiEventListener implements EventListener<ClockTick> {
    private static final Logger logger = Logger.getLogger( CloudWatchApiEventListener.class );

    private static final AtomicReference<CounterSnapshot<Counted>> counterSnapshotRef = new AtomicReference<>( );

    private static final long API_USAGE_INTERVAL = 300_000;

    public static void register() {
      Listeners.register( ClockTick.class, new CloudWatchApiEventListener( ) );
    }

    @Override
    public void fireEvent( @Nonnull final ClockTick event ) {
      if ( Bootstrap.isOperational( ) && Topology.isEnabledLocally( CloudWatch.class ) ) {
        final CounterSnapshot<Counted> snapshot = counterSnapshotRef.get( );
        final long lastPeriodEnd = counter.lastPeriodEnd( );
        if ( snapshot == null ) {
          counterSnapshotRef.compareAndSet( null, counter.snapshot( lastPeriodEnd ) );
        } else if ( ( lastPeriodEnd - snapshot.getPeriodEnd( ) ) > API_USAGE_INTERVAL ) {
          final CounterSnapshot<Counted> newSnapshot = counter.snapshot( lastPeriodEnd );
          if ( counterSnapshotRef.compareAndSet( snapshot, newSnapshot ) ) {
            final CounterSnapshot<Counted> diffSnapshot = newSnapshot.since( snapshot );
            diffSnapshot.counts( ).forEach( tuple -> {
              final Counted counted = tuple._1;
              final Integer count = tuple._2;
              CloudWatchApiUsageEvent usageEvent = null;
              if ( count > 0 ) try {
                usageEvent = CloudWatchApiUsageEvent.of(
                    Internets.localHostAddress( ),
                    counted.getAccount( ),
                    counted.getItem( ),
                    count,
                    snapshot.getPeriodEnd( ),
                    diffSnapshot.getPeriodEnd( )
                );
                ListenerRegistry.getInstance( ).fireEvent( usageEvent );
              } catch ( final Exception e ) {
                logger.warn( "Failed to fire cloudwatch api usage event " + usageEvent, e);
              }
            } );
          }
        }
      }
    }
  }
}
