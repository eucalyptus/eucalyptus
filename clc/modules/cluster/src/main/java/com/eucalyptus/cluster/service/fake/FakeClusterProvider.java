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
package com.eucalyptus.cluster.service.fake;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cluster.common.internal.Cluster;
import com.eucalyptus.cluster.common.internal.ClusterRegistry;
import com.eucalyptus.cluster.common.internal.spi.ClusterProvider;
import com.eucalyptus.cluster.common.msgs.NodeType;
import com.eucalyptus.cluster.proxy.node.Nodes;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.SubjectMessageCallback;
import com.google.common.base.Function;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
@SuppressWarnings( { "WeakerAccess", "unused" } )
public class FakeClusterProvider implements ClusterProvider {
  private static Logger LOG = Logger.getLogger( FakeClusterProvider.class );

  private Cluster cluster;
  private final ClusterRegistry registry;
  private final FakeClusterConfiguration configuration;

  public FakeClusterProvider(
      final ClusterRegistry registry,
      final FakeClusterConfiguration configuration
  ) {
    this.configuration = configuration;
    this.registry = registry;
  }

  @Override
  public String getName( ) {
    return configuration.getName( );
  }

  @Override
  public String getPartition() {
    return configuration.getPartition( );
  }

  @Override
  public String getHostName( ) {
    return configuration.getHostName( );
  }

  @Override
  public Partition lookupPartition( ) {
    return Partitions.lookup( configuration );
  }

  @Override
  public ServiceConfiguration getConfiguration( ) {
    return configuration;
  }

  @Override
  public void init( final Cluster cluster ) {
    this.cluster = cluster;
  }

  @Override
  public void check( ) {
    //TODO: fail check on errors from refresh callbacks?
  }

  @Override
  public void start( ) throws ServiceRegistrationException {
    disable( );
    FakeClusterState.reload( );
  }

  @Override
  public void stop( ) throws ServiceRegistrationException {
    registry.deregister( cluster.getName( ) );
  }

  @Override
  public void enable( ) throws ServiceRegistrationException {
    registry.register( cluster );
  }

  @Override
  public void disable( ) throws ServiceRegistrationException {
    registry.registerDisabled( cluster );
  }

  @Override
  public void refreshResources( ) {
    Refresh.RESOURCES.accept( cluster );
  }

  @Override
  public void updateNodeInfo( final List<NodeType> nodes ) {
    Nodes.updateNodeInfo( getConfiguration(), nodes );
  }

  @Override
  public boolean hasNode( final String sourceHost ) {
    try {
      Nodes.lookup( getConfiguration( ), sourceHost );
      return true;
    } catch ( NoSuchElementException e ) {
      return false;
    }
  }

  @Override
  public void cleanup( final Cluster cluster, final Exception ex ) {
    Nodes.clusterCleanup( cluster, ex );
  }

  private enum Refresh implements Consumer<Cluster> {
    RESOURCES( "com.eucalyptus.cluster.callback.ResourceStateCallback" ), //TODO: revert back to class references
    INSTANCES( "com.eucalyptus.cluster.callback.VmStateCallback" ),
    ;
    Class<? extends SubjectMessageCallback<Cluster,?,?>>  refresh;

    Refresh( final String refresh ) {
      try {
        //noinspection unchecked
        this.refresh = (Class<? extends SubjectMessageCallback<Cluster,?,?>> ) Class.forName( refresh );
      } catch ( ClassNotFoundException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }

    public void accept( final Cluster cluster ) {
      try {
        final SubjectMessageCallback<Cluster,?,?> messageCallback = refresh.newInstance( );
        messageCallback.setSubject( cluster );
        BaseMessage baseMessage = AsyncRequests.newRequest( messageCallback ).sendSync( cluster.getConfiguration( ) );
        if ( Logs.extreme( ).isDebugEnabled( ) ) {
          Logs.extreme( ).debug( "Response to " + messageCallback + ": " + baseMessage );
        }
      } catch ( CancellationException ex ) {
        //do nothing
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex );
        throw Exceptions.toUndeclared( ex );
      }
    }

    @Override
    public String toString( ) {
      return this.name( ) + ":" + this.refresh.getSimpleName( );
    }
  }

  public static class FakeClusterRefreshEventListener implements EventListener<ClockTick> {
    private final AtomicInteger refreshOrdinal = new AtomicInteger( 0 );

    public static void register( ) {
      Listeners.register( ClockTick.class, new FakeClusterRefreshEventListener( ) );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      if ( Topology.isEnabledLocally( Eucalyptus.class ) &&
          Hosts.isCoordinator( ) &&
          Bootstrap.isOperational( ) &&
          !Databases.isVolatile( ) ) {
        final Refresh refresh = Refresh.values( )[ refreshOrdinal.getAndIncrement( ) % Refresh.values( ).length ];
        for ( final Cluster cluster : ClusterRegistry.getInstance( ).listValues( ) ) {
          final ServiceConfiguration configuration = cluster.getConfiguration( );
          if ( configuration instanceof FakeClusterConfiguration ) {
            refresh.accept( cluster ); // TODO: threading
          }
        }
      }
    }
  }

  @TypeMapper
  public enum FakeClusterConfigurationToClusterSpi implements Function<FakeClusterConfiguration,ClusterProvider> {
    @SuppressWarnings( "unused" )
    INSTANCE;

    @Nullable
    @Override
    public ClusterProvider apply( @Nullable final FakeClusterConfiguration configuration ) {
      return new FakeClusterProvider( ClusterRegistry.getInstance( ), configuration );
    }
  }
}
