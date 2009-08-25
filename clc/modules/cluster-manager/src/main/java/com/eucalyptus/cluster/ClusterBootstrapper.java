package com.eucalyptus.cluster;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.Configuration;

@Provides(resource=Resource.Clusters)
@Depends(local=Component.eucalyptus)
public class ClusterBootstrapper extends Bootstrapper {

  @Override
  public boolean load( Resource current ) throws Exception {
    for( ClusterConfiguration c : Configuration.getClusterConfigurations( ) ) {
      Cluster newCluster = ClusterBootstrapper.getCluster( c );
      Clusters.getInstance( ).register( newCluster );
    }
    return true;
  }

  public static Cluster getCluster( ClusterConfiguration c ) {
    ClusterThreadGroup threadGroup = new ClusterThreadGroup( c.getName( ), c );
    Cluster newCluster = new Cluster(threadGroup, c );
    return newCluster;
  }

  @Override
  public boolean start( ) throws Exception {
    for( Cluster c : Clusters.getInstance( ).listValues( ) ) {
      c.getThreadGroup( ).startThreads( );
    }
    return true;
  }

}
