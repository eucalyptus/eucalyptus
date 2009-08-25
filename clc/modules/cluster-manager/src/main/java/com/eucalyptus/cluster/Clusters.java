package com.eucalyptus.cluster;

import java.util.ArrayList;
import java.util.List;

import edu.ucsb.eucalyptus.cloud.AbstractNamedRegistry;
import edu.ucsb.eucalyptus.msgs.RegisterClusterType;

public class Clusters extends AbstractNamedRegistry<Cluster> {
  private static Clusters singleton = getInstance( );

  public static Clusters getInstance( ) {
    synchronized ( Clusters.class ) {
      if ( singleton == null ) singleton = new Clusters( );
    }
    return singleton;
  }

  public List<RegisterClusterType> getClusters( ) {
    List<RegisterClusterType> list = new ArrayList<RegisterClusterType>( );
    for ( Cluster c : this.listValues( ) )
      list.add( c.getWeb( ) );
    return list;
  }

  public List<String> getClusterAddresses( ) {
    List<String> list = new ArrayList<String>( );
    for ( Cluster c : this.listValues( ) )
      list.add( c.getConfiguration( ).getHostName( ) + ":" + c.getConfiguration( ).getPort( ) );
    return list;
  }

}
