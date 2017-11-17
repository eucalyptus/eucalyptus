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
package com.eucalyptus.cluster.service.node;

import java.lang.ref.WeakReference;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.common.ClusterRegistry;
import com.eucalyptus.cluster.common.msgs.ClusterBundleRestartInstanceResponseType;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.util.Assert;
import com.google.common.base.MoreObjects;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 *
 */
@ComponentNamed
public class ClusterNodeActivities {

  private static final Logger logger = Logger.getLogger( ClusterNodeActivities.class );

  private final ClusterNodes clusterNodes;
  private final Clock clock;

  private volatile ClusterNodeSensorConfig sensorConfig = ClusterNodeSensorConfig.none( );
  private volatile boolean enabled = false;

  public ClusterNodeActivities(
      final ClusterNodes clusterNodes,
      final Clock clock
  ) {
    this.clusterNodes = Assert.notNull( clusterNodes, "clusterNodes" );
    this.clock =  Assert.notNull( clock, "clock" );

    ClusterNodeActivitiesEventListener.registerActivities( this );
  }

  @Inject
  public ClusterNodeActivities(
      final ClusterNodes clusterNodes
  ) {
    this( clusterNodes, Clock.systemDefaultZone( ) );
  }

  public boolean enable( final boolean enabled ) {
    final boolean updated = this.enabled != enabled;
    if ( updated ) {
      this.enabled = enabled;
    }
    return updated;
  }

  public boolean configureSensorPolling( final int historySize, final int collectionIntervalMs ) {
    final ClusterNodeSensorConfig config = ClusterNodeSensorConfig.of( historySize, collectionIntervalMs );
    final boolean updated = !this.sensorConfig.equals( config );
    if ( updated ) {
      this.sensorConfig = config;
    }
    return updated;
  }

  @SuppressWarnings( "WeakerAccess" )
  public void doActivities( ) {
    if ( enabled || ClusterRegistry.hasEnabledLocalCluster( ) ) {
      final ClusterNodeActivityContext context = ClusterNodeActivityContext.of( clusterNodes, sensorConfig, clock );
      Stream.of( ClusterNodeActivity.values( ) ).forEach( activity -> activity.doActivity( context ) );
    }
  }

  private enum ClusterNodeActivity {
    RefreshResources( context -> context.getClusterNodes( ).refreshResources( ) ),
    RefreshVms( context -> context.getClusterNodes( ).refreshVms( context.getClock( ).millis( ) ) ),
    RefreshSensors( context -> {
      if ( !ClusterNodeSensorConfig.none( ).equals( context.getSensorConfig( ) ) ) {
        context.getClusterNodes( ).refreshSensors(
            context.getClock( ).millis( ),
            context.getSensorConfig( ).getHistorySize( ),
            context.getSensorConfig( ).getCollectionIntervalMs( ) );
      }
    } ),
    ReportStatus( context -> context.getClusterNodes( ).logStatus( ) ),
    ;

    @SuppressWarnings( "NonSerializableFieldInSerializableClass" )
    private final Consumer<ClusterNodeActivityContext> activity;

    ClusterNodeActivity( final Consumer<ClusterNodeActivityContext> activity ) {
      this.activity = activity;
    }

    void doActivity( ClusterNodeActivityContext context ) {
      try {
        activity.accept( context );
      } catch ( final Throwable t ) {
        logger.error( "Error in node activity " + name( ), t );
      }
    }
  }

  private static final class ClusterNodeActivityContext {
    private final ClusterNodes clusterNodes;
    private final ClusterNodeSensorConfig sensorConfig;
    private final Clock clock;

    private ClusterNodeActivityContext(
        final ClusterNodes clusterNodes,
        final ClusterNodeSensorConfig sensorConfig,
        final Clock clock
    ) {
      this.clusterNodes = clusterNodes;
      this.sensorConfig = sensorConfig;
      this.clock = clock;
    }

    static ClusterNodeActivityContext of(
        final ClusterNodes clusterNodes,
        final ClusterNodeSensorConfig sensorConfig,
        final Clock clock
    ) {
      return new ClusterNodeActivityContext( clusterNodes, sensorConfig, clock );
    }

    ClusterNodes getClusterNodes() {
      return clusterNodes;
    }

    ClusterNodeSensorConfig getSensorConfig() {
      return sensorConfig;
    }

    Clock getClock() {
      return clock;
    }
  }

  private static final class ClusterNodeSensorConfig {
    private final int historySize;
    private final int collectionIntervalMs;

    private ClusterNodeSensorConfig( final int historySize, final int collectionIntervalMs ) {
      this.historySize = historySize;
      this.collectionIntervalMs = collectionIntervalMs;
    }

    static ClusterNodeSensorConfig of( final int historySize, final int collectionIntervalMs ) {
      return new ClusterNodeSensorConfig( historySize, collectionIntervalMs );
    }

    static ClusterNodeSensorConfig none( ) {
      return of( 0, 0 );
    }

    int getHistorySize( ) {
      return historySize;
    }

    int getCollectionIntervalMs( ) {
      return collectionIntervalMs;
    }

    public String toString( ) {
      return MoreObjects.toStringHelper( this )
          .add( "history-size", getHistorySize( ) )
          .add( "collection-interval-ms", getCollectionIntervalMs( ) )
          .toString( );
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final ClusterNodeSensorConfig that = (ClusterNodeSensorConfig) o;
      return getHistorySize( ) == that.getHistorySize( ) &&
          getCollectionIntervalMs( ) == that.getCollectionIntervalMs( );
    }

    @Override
    public int hashCode() {
      return Objects.hash( getHistorySize( ), getCollectionIntervalMs( ) );
    }
  }

  public static class ClusterNodeActivitiesEventListener implements EventListener<ClockTick> {
    private static final CopyOnWriteArrayList<WeakReference<ClusterNodeActivities>> activities = new CopyOnWriteArrayList<>( );

    @SuppressWarnings( "unused" )
    public static void register( ) {
      Listeners.register( ClockTick.class, new ClusterNodeActivitiesEventListener( ) );
    }

    public static void registerActivities( final ClusterNodeActivities nodeActivities ) {
      activities.add( new WeakReference<>( nodeActivities ) );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
        activities.forEach( ref -> Option.of( ref.get( ) ).forEach( ClusterNodeActivities::doActivities ) );
    }
  }
}
