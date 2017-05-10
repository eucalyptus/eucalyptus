/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.objectstorage;

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
import com.eucalyptus.reporting.Counter.CountedS3;
import com.eucalyptus.reporting.Counter.CounterSnapshot;
import com.eucalyptus.reporting.event.CloudWatchApiUsageEvent;
import com.eucalyptus.reporting.event.S3ApiCountedEvent;
import com.eucalyptus.reporting.event.S3ApiBytesEvent;
import com.eucalyptus.util.Internets;


/**
 * Listener that fires periodically to send S3 API accumulated events from sensor data.
 */
public class S3ApiEventListener implements EventListener<ClockTick> {

  private static final Counter<S3ApiUsageEvent,CountedS3> counter =
      new Counter<>( 60_000, 15, S3ApiEventListener::eventToCountedItem );

  public static CountedS3 eventToCountedItem( S3ApiCountedEvent event ) {
    final String action = message.getClass( ).getSimpleName( ).replaceAll( "(ResponseType|Type)$", "" );
    try {
      final AuthContext authContext = Contexts.lookup( message.getCorrelationId( ) ).getAuthContext( ).get( );
      return new Counted( authContext.getAccountNumber( ), action );
    } catch ( AuthException | NoSuchContextException ignore ) {
    }
    return null;
  }

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
