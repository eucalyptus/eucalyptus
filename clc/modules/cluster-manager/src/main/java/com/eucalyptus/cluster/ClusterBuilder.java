package com.eucalyptus.cluster;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.config.DeregisterClusterType;
import com.eucalyptus.config.DescribeClustersType;
import com.eucalyptus.config.ModifyClusterAttributeType;
import com.eucalyptus.config.RegisterClusterType;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;

@ComponentPart( ClusterController.class )
@Handles( { RegisterClusterType.class,
           DeregisterClusterType.class,
           DescribeClustersType.class,
           ModifyClusterAttributeType.class } )
public class ClusterBuilder extends AbstractServiceBuilder<ClusterConfiguration> {
  static Logger LOG = Logger.getLogger( ClusterBuilder.class );
  
  @Override
  public Boolean checkAdd( final String partition, final String name, final String host, final Integer port ) throws ServiceRegistrationException {
    try {
      final Partition part = Partitions.lookup( this.newInstance( partition, name, host, port ) );
      part.syncKeysToDisk( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw new ServiceRegistrationException( String.format( "Unexpected error caused cluster registration to fail for: partition=%s name=%s host=%s port=%d",
                                                             partition, name, host, port ), ex );
    }
    return super.checkAdd( partition, name, host, port );
  }
  
  @Override
  public ClusterConfiguration newInstance( ) {
    return new ClusterConfiguration( );
  }
  
  @Override
  public ClusterConfiguration newInstance( final String partition, final String name, final String host, final Integer port ) {
    return new ClusterConfiguration( partition, name, host, port );
  }
  
  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( ClusterController.class );
  }
  
  @Override
  public void fireStart( final ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Starting cluster: " + config );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_START, config.getComponentId( ).name( ), config.getName( ),
                      ServiceUris.remote( config ).toASCIIString( ) ).info( );
    try {
      if ( !Clusters.getInstance( ).contains( config.getName( ) ) ) {
        final Cluster newCluster = new Cluster( ( ClusterConfiguration ) config );//TODO:GRZE:fix the type issue here.
        newCluster.start( );
      } else {
        try {
          final Cluster newCluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
          Clusters.getInstance( ).deregister( config.getName( ) );
          newCluster.start( );
        } catch ( final NoSuchElementException ex ) {
          final Cluster newCluster = Clusters.getInstance( ).lookup( config.getName( ) );
          Clusters.getInstance( ).deregister( config.getName( ) );
          newCluster.start( );
        }
      }
    } catch ( final NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
  }
  
  @Override
  public void fireEnable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Enabling cluster: " + config );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_ENABLED, config.getComponentId( ).name( ), config.getName( ),
                      ServiceUris.remote( config ).toASCIIString( ) ).info( );
    try {
      try {
        final Cluster newCluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
        newCluster.enable( );
      } catch ( final NoSuchElementException ex ) {
        final Cluster newCluster = Clusters.getInstance( ).lookup( config.getName( ) );
        newCluster.enable( );
      }
    } catch ( final NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
    
  }
  
  @Override
  public void fireDisable( final ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Disabling cluster: " + config );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_DISABLED, config.getComponentId( ).name( ), config.getName( ),
                      ServiceUris.remote( config ).toASCIIString( ) ).info( );
    try {
      if ( Clusters.getInstance( ).contains( config.getName( ) ) ) {
        try {
          final Cluster newCluster = Clusters.getInstance( ).lookup( config.getName( ) );
          newCluster.disable( );
        } catch ( final NoSuchElementException ex ) {
          final Cluster newCluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
          newCluster.disable( );
        }
      }
    } catch ( final NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
  }
  
  @Override
  public void fireStop( final ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      LOG.info( "Tearing down cluster: " + config );
      final Cluster cluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
      EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_STOPPED, config.getComponentId( ).name( ), config.getName( ),
                        ServiceUris.remote( config ).toASCIIString( ) ).info( );
      cluster.stop( );
    } catch ( final NoSuchElementException ex ) {
      LOG.error( ex, ex );
    } catch ( final Exception ex ) {
      LOG.error( ex, ex );
    }
  }
  
  @Override
  public void fireCheck( final ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      Clusters.lookup( config ).check( );
    } catch ( final NoSuchElementException ex ) {
      throw Faults.failure( config, ex );
    } catch ( final IllegalStateException ex ) {
      Logs.exhaust( ).error( ex, ex );
      throw Faults.failure( config, ex );
    } catch ( final Exception ex ) {
      Logs.exhaust( ).error( ex, ex );
      throw Faults.failure( config, ex );
    }
  }
  
}
