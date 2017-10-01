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
