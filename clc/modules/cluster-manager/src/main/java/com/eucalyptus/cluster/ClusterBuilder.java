package com.eucalyptus.cluster;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.DiscoverableServiceBuilder;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.DeregisterClusterType;
import com.eucalyptus.config.DescribeClustersType;
import com.eucalyptus.config.ModifyClusterAttributeType;
import com.eucalyptus.config.RegisterClusterType;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;

@DiscoverableServiceBuilder( ClusterController.class )
@Handles( { RegisterClusterType.class, DeregisterClusterType.class, DescribeClustersType.class, ClusterConfiguration.class, ModifyClusterAttributeType.class } )
public class ClusterBuilder extends AbstractServiceBuilder<ClusterConfiguration> {
  static Logger LOG                 = Logger.getLogger( ClusterBuilder.class );
  
  @Override
  public Boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    if ( !Partitions.testPartitionCredentialsDirectory( partition ) ) {
      throw new ServiceRegistrationException( "Cluster registration failed because the key directory cannot be created." );
    } else {
      return super.checkAdd( partition, name, host, port );
    }
  }
  
  @Override
  public ClusterConfiguration newInstance( ) {
    return new ClusterConfiguration( );
  }
  
  @Override
  public ClusterConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new ClusterConfiguration( partition, name, host, port );
  }
  
  @Override
  public Component getComponent( ) {
    return Components.lookup( ClusterController.class );
  }
  
  @Override
  public ClusterConfiguration add( String partitionName, String name, String host, Integer port ) throws ServiceRegistrationException {
    ClusterConfiguration config = this.newInstance( partitionName, name, host, port );
    try {
      Partition part = Partitions.lookup( config );
      ServiceConfigurations.getInstance( ).store( config );
      part.link( config );
    } catch ( ServiceRegistrationException ex ) {
      Partitions.maybeRemove( config.getPartition( ) );
      throw ex;
    } catch ( Throwable ex ) {
      Partitions.maybeRemove( config.getPartition( ) );
      LOG.error( ex, ex );
      throw new ServiceRegistrationException( String.format( "Unexpected error caused cluster registration to fail for: partition=%s name=%s host=%s port=%d",
                                                             partitionName, name, host, port ), ex );
    }
    return config;
  }
  
  @Override
  public ClusterConfiguration remove( ServiceConfiguration config ) throws ServiceRegistrationException {
    Partition part = Partitions.lookup( config );
    ClusterConfiguration ret = super.remove( config );
    part.unlink( ret );
    return ret;
  }
  
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Starting up cluster: " + config );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_START, config.getComponentId( ).name( ), config.getName( ), config.getUri( ).toASCIIString( ) ).info( );
    try {
      if ( Components.lookup( Eucalyptus.class ).isEnabledLocally( ) ) {
        if ( !Clusters.getInstance( ).contains( config.getName( ) ) ) {
          Cluster newCluster = new Cluster( ( ClusterConfiguration ) config );//TODO:GRZE:fix the type issue here.
          Clusters.getInstance( ).registerDisabled( newCluster );
          newCluster.start( );
        } else {
          try {
            Cluster newCluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
            newCluster.start( );
          } catch ( NoSuchElementException ex ) {
            Cluster newCluster = Clusters.getInstance( ).lookup( config.getName( ) );
            Clusters.getInstance( ).disable( config.getName( ) );
            newCluster.start( );
          }
        }
      }
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
  }
  
  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Enabling cluster: " + config );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_ENABLED, config.getComponentId( ).name( ), config.getName( ), config.getUri( ).toASCIIString( ) ).info( );
    try {
      if ( Components.lookup( Eucalyptus.class ).isEnabledLocally( ) ) {
        try {
          Cluster newCluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
          Clusters.getInstance( ).enable( config.getName( ) );
          newCluster.enable( );
        } catch ( NoSuchElementException ex ) {
          Cluster newCluster = Clusters.getInstance( ).lookup( config.getName( ) );
          newCluster.enable( );
        }
      }
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
    
  }
  
  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Disabling cluster: " + config );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_DISABLED, config.getComponentId( ).name( ), config.getName( ), config.getUri( ).toASCIIString( ) ).info( );
    try {
      if ( Components.lookup( Eucalyptus.class ).isEnabledLocally( ) ) {
        if ( Clusters.getInstance( ).contains( config.getName( ) ) ) {
          try {
            Cluster newCluster = Clusters.getInstance( ).lookup( config.getName( ) );
            Clusters.getInstance( ).disable( newCluster.getName( ) );
            newCluster.disable( );
          } catch ( NoSuchElementException ex ) {
            Cluster newCluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
            newCluster.disable( );
          }
        }
      }
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
  }
  
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    try {
      LOG.info( "Tearing down cluster: " + config );
      Cluster cluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
      EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_STOPPED, config.getComponentId( ).name( ), config.getName( ), config.getUri( ).toASCIIString( ) ).info( );
      cluster.stop( );
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex , ex );
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
    }
  }
  
  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {
    Clusters.lookup( config ).check( );
  }
  
}
