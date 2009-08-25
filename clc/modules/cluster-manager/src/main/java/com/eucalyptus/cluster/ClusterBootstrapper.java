package com.eucalyptus.cluster;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Credentials;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

@Provides( resource = Resource.Clusters )
@Depends( local = Component.eucalyptus )
public class ClusterBootstrapper extends Bootstrapper implements Runnable {
  private static Logger LOG         = Logger.getLogger( ClusterBootstrapper.class );
  private Thread        thread;
  private boolean       initialized = false;
  private boolean       finished    = false;

  @Override
  public boolean load( Resource current ) throws Exception {
    LOG.info( "Creating the cluster bootstrap thread." );
    this.thread = new Thread( this );
    this.run( );
    return true;
  }

  public static Cluster getCluster( ClusterConfiguration c ) throws EucalyptusCloudException {
    ClusterCredentials credentials = null;
    EntityWrapper<ClusterCredentials> credDb = Credentials.getEntityWrapper( );
    try {
      credentials = credDb.getUnique( new ClusterCredentials(c.getName( )) );
    } catch ( EucalyptusCloudException e ) {
      LOG.error("Failed to load credentials for cluster: " + c.getName( ) );
      throw e;
    } finally {
      credDb.rollback( );
    }
    ClusterThreadGroup threadGroup = new ClusterThreadGroup( c, credentials );
    Cluster newCluster = new Cluster( threadGroup, c, credentials );
    return newCluster;
  }

  @Override
  public boolean start( ) throws Exception {
    if( !this.thread.isAlive( ) ) {
      this.thread.start();
    }
    return true;
  }

  public void run( ) {
    while ( !finished ) {
      List<String> clusterNames = Lists.newArrayList( );
      try {
        for ( ClusterConfiguration c : Configuration.getClusterConfigurations( ) ) {
          clusterNames.add( c.getName( ) );
          try {
            Cluster foundCluster = Clusters.getInstance( ).lookup( c.getName( ) );
            if(initialized) {
              foundCluster.getThreadGroup().create( );
            }
          } catch ( NoSuchElementException e ) {
            try {
              Cluster newCluster = ClusterBootstrapper.getCluster( c );
              Clusters.getInstance( ).register( newCluster );
              if(initialized) {
                newCluster.getThreadGroup().create( );
              }
              LOG.info( "Registering cluster: " + newCluster.getName( ) );
            } catch ( Exception e1 ) {
              LOG.error( "Error loading cluster configuration: " + c.getName( ) );
              LOG.error( e1, e1 );
            }
          }
        }
        List<String> registeredClusters = Lists.newArrayList( Clusters.getInstance( ).listKeys( ) );
        registeredClusters.removeAll( clusterNames );
        for ( String removeClusterName : registeredClusters ) {
          try {
            Cluster removeCluster = Clusters.getInstance( ).lookup( removeClusterName );
            removeCluster.getThreadGroup( ).stopThreads( );
            Clusters.getInstance( ).deregister( removeCluster.getName( ) );
          } catch ( NoSuchElementException e ) {
          }
        }
      } catch ( EucalyptusCloudException e ) {
        LOG.error( "Failed to load cluster configurations: " + e.getMessage( ) );
        LOG.error( e, e );
      }
      if ( !initialized ) {
        initialized = true;
        return;
      } else {
        try {
          this.start( );
          Thread.sleep( 5000 );
        } catch ( Exception e ) {
          LOG.error( e,e );
        }
      }
    }
  }

}
