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
package com.eucalyptus.portal.capacity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloudwatch.common.CloudWatchClient;
import com.eucalyptus.cloudwatch.common.msgs.Dimension;
import com.eucalyptus.cloudwatch.common.msgs.Dimensions;
import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import com.eucalyptus.cloudwatch.common.msgs.MetricDatum;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataResponseType;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.portal.BillingProperties;
import com.eucalyptus.reporting.Capacity;
import com.eucalyptus.reporting.Capacity.CapacityEntry;
import com.eucalyptus.reporting.Capacity.CapacitySnapshot;
import com.eucalyptus.util.async.AsyncProxy;
import com.eucalyptus.util.async.CheckedListenableFuture;
import javaslang.collection.Stream;

/**
 *
 */
@SuppressWarnings( { "WeakerAccess", "unused" } )
public class CapacityMetricEventListener implements EventListener<ClockTick> {

  private static final Logger logger = Logger.getLogger( CapacityMetricEventListener.class );
  private static final long interval = 300_000L;
  private static final AtomicLong lastPutAttempt = new AtomicLong( ( System.currentTimeMillis( ) / interval ) * interval );
  private static final CloudWatchClient client = AsyncProxy.privilegedClient( CloudWatchClient.class );

  private enum CapacityMetric implements Predicate<CapacityEntry>, Function<CapacityEntry, MetricDatum> {
    ComputeCoreAvailable( "Count", "Core" ),
    ComputeCoreTotal( "Count", "Core" ),
    ComputeDiskAvailable( "Gigabytes", "Disk" ),
    ComputeDiskTotal( "Gigabytes", "Disk" ),
    ComputeMemoryAvailable( "Megabytes", "Memory" ),
    ComputeMemoryTotal( "Megabytes", "Memory" ),
    InstanceTypeAvailable( "Count", "Instance", "vm-type" ),
    InstanceTypeTotal( "Count", "Instance", "vm-type" ),
    PublicIpAvailable( "Count", "Address" ),
    PublicIpTotal( "Count", "Address" ),
    StorageObjectAvailable( "Gigabytes", "StorageObject" ),
    StorageObjectTotal( "Gigabytes", "StorageObject" ),
    StorageEbsAvailable( "Gigabytes", "StorageEBS" ),
    StorageEbsTotal( "Gigabytes", "StorageEBS" ),
    ;

    private final String unit;
    private final String type;
    private final String subtype;

    CapacityMetric( String unit, String type ) {
      this( unit, type, null );
    }

    CapacityMetric( String unit, String type, String subtype ) {
      this.unit = unit;
      this.type = type;
      this.subtype = subtype;
    }

    boolean isTotal( ) {
      return name( ).contains( "Total" );
    }

    String metric( final CapacityEntry capacityEntry ) {
      return name( ) + ( subtype == null ? "" : "." + capacityEntry.getSubtypes( ).get( subtype ) );
    }

    @Override
    public boolean test( final CapacityEntry capacityEntry ) {
      return capacityEntry.getTotal( ) > 0 &&
          type.equals( capacityEntry.getType( ) ) && (
          subtype == null && capacityEntry.getSubtypes( ).isEmpty( ) ||
          subtype != null && capacityEntry.getSubtypes( ).keySet( ).equals( Collections.singleton( subtype ) )
      );
    }

    @Override
    public MetricDatum apply( final CapacityEntry capacityEntry ) {
      final MetricDatum datum = new MetricDatum( );
      datum.setMetricName( metric( capacityEntry ) );
      if ( !capacityEntry.getDimensions( ).isEmpty( ) ) {
        final Dimensions dimensions = new Dimensions( );
        dimensions.setMember( Stream.ofAll( capacityEntry.getDimensions( ).entrySet( ) )
            .map( entry -> new Dimension( entry.getKey( ), entry.getValue( ) ) )
            .toJavaList( ArrayList::new ) );
        datum.setDimensions( dimensions );
      }
      datum.setUnit( unit );
      datum.setValue( (double)(isTotal( ) ? capacityEntry.getTotal( ) : capacityEntry.getAvailable( )) );
      return datum;
    }
  }

  public static void register( ) {
    Listeners.register( ClockTick.class, new CapacityMetricEventListener( ) );
  }

  @Override
  public void fireEvent( final ClockTick event ) {
    final long last = lastPutAttempt.get( );
    final long now = ( System.currentTimeMillis( ) / interval ) * interval;
    final Date dateNow = new Date( now );
    if ( BillingProperties.ENABLED && Bootstrap.isOperational( ) &&
        ( now - interval ) >= last && lastPutAttempt.compareAndSet( last, now ) ) {
      final CapacitySnapshot snapshot = Capacity.snapshot( );
      final MetricData metricData = new MetricData( );
      for ( final CapacityMetric metric : CapacityMetric.values( ) ) {
        metricData.getMember( ).addAll( snapshot.getCapacities( )
            .filter( metric )
            .map( metric )
            .map( datum -> {
              datum.setTimestamp( dateNow );
              return datum;
            } )
            .toJavaList( ) );
      }
      if ( !metricData.getMember( ).isEmpty( ) ) {
        final PutMetricDataType putMetricData = new PutMetricDataType( );
        putMetricData.setNamespace( "AWS/EucalyptusCapacity" );
        putMetricData.setMetricData( metricData );
        try {
          final CheckedListenableFuture<PutMetricDataResponseType> response = client.putMetricDataAsync( putMetricData );
          response.addListener( () -> {
            try {
              response.get( );
            } catch ( Exception e ) {
              logger.error(
                  "Unable to put capacity metric data: " + e.getMessage( ),
                  logger.isDebugEnabled( ) ? e : null );
            }
          } );
        } catch ( NoSuchElementException ignore ) {
          // cloudwatch not available, try again later
        } catch ( final Exception e ) {
          logger.error(
              "Unable to put capacity metric data: " + e.getMessage( ),
              logger.isDebugEnabled( ) ? e : null );
        }
      }
    }
  }
}
