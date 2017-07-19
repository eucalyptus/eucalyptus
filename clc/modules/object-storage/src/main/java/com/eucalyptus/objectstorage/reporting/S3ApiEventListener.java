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
package com.eucalyptus.objectstorage.reporting;

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.Topology;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.reporting.Counter;
import com.eucalyptus.reporting.Counter.CountedS3;
import com.eucalyptus.reporting.Counter.CounterSnapshot;
import com.eucalyptus.reporting.event.S3ApiUsageEvent;
import com.eucalyptus.reporting.event.S3ApiAccumulatedUsageEvent;


/**
 * Listener that fires periodically to send S3 API accumulated events from individually counted events.
 */
public class S3ApiEventListener implements EventListener<ClockTick> {

  private static final Logger LOG = Logger.getLogger( S3ApiEventListener.class );

  private static final Counter<S3ApiUsageEvent,CountedS3> s3ApiCountsCounter =
      new Counter<>( 60_000, 15, S3ApiUsageCounter::countApiCountEvent );

  private static final Counter<S3ApiUsageEvent,CountedS3> s3ApiBytesCounter =
      new Counter<>( 60_000, 15, S3ApiUsageCounter::countApiBytesEvent,
          S3ApiUsageCounter::countApiBytesValue);

  private static final AtomicReference<CounterSnapshot<CountedS3>> s3ApiCountsSnapshotRef = new AtomicReference<>( );
  private static final AtomicReference<CounterSnapshot<CountedS3>> s3ApiBytesSnapshotRef = new AtomicReference<>( );

  private static final long S3_API_USAGE_INTERVAL = 300_000;

  public static void count( S3ApiUsageEvent usageEvent) {
    s3ApiCountsCounter.count(usageEvent);
    if (usageEvent.getSize() != null && usageEvent.getSize() > 0) {
      s3ApiBytesCounter.count(usageEvent);
    }
  }
  
  public static void register() {
    Listeners.register( ClockTick.class, new S3ApiEventListener( ) );
  }

  @Override
  public void fireEvent( @Nonnull final ClockTick event ) {
    // Fires up to two events:
    // - The accumulated counts of the S3 API calls (if any)
    // - The accumulated bytes transferred for all S3 API calls that transfer bytes (if any)
    if ( Bootstrap.isOperational( ) && Topology.isEnabledLocally( ObjectStorage.class ) ) {
      fireEvent(s3ApiCountsCounter, s3ApiCountsSnapshotRef, S3ApiAccumulatedUsageEvent.ValueType.Counts);
      fireEvent(s3ApiBytesCounter, s3ApiBytesSnapshotRef, S3ApiAccumulatedUsageEvent.ValueType.Bytes);
    }
  }
  
  private static void fireEvent( Counter<S3ApiUsageEvent,CountedS3> counter,
      AtomicReference<CounterSnapshot<CountedS3>> snapshotRef,
      S3ApiAccumulatedUsageEvent.ValueType valueType) {
    final CounterSnapshot<CountedS3> snapshot = snapshotRef.get( );
    final long lastPeriodEnd = counter.lastPeriodEnd( );
    if ( snapshot == null ) {
      snapshotRef.compareAndSet( null, counter.snapshot( lastPeriodEnd ) );
    } else if ( ( lastPeriodEnd - snapshot.getPeriodEnd( ) ) > S3_API_USAGE_INTERVAL ) {
      final CounterSnapshot<CountedS3> newSnapshot = counter.snapshot( lastPeriodEnd );
      if ( snapshotRef.compareAndSet( snapshot, newSnapshot ) ) {
        final CounterSnapshot<CountedS3> diffSnapshot = newSnapshot.since( snapshot );
        if ( diffSnapshot != null ) {
          diffSnapshot.counts( ).forEach( tuple -> {
            final CountedS3 counted = tuple._1;
            final Long value = tuple._2;
            S3ApiAccumulatedUsageEvent event = null;
            if ( value > 0 ) try {
              event = S3ApiAccumulatedUsageEvent.with(
                  counted.getAccount( ),
                  counted.getAction( ),
                  counted.getBucketName( ),
                  value,
                  valueType,
                  snapshot.getPeriodEnd( ),
                  diffSnapshot.getPeriodEnd( )
                  );
              ListenerRegistry.getInstance( ).fireEvent( event );
            } catch ( final Exception e ) {
              LOG.warn( "Failed to fire S3 API usage event " + event, e);
            }
          } );
        }
      }
    }
  }
  
}
