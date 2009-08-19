package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.AbstractNamedRegistry;
import edu.ucsb.eucalyptus.cloud.entities.ClusterInfo;
import edu.ucsb.eucalyptus.msgs.RegisterClusterType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.KeyTool;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.SubDirectory;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class Clusters extends AbstractNamedRegistry<Cluster> {

  private static Logger   LOG       = Logger.getLogger( Clusters.class );

  private static Clusters singleton = getInstance( );

  public static Clusters getInstance( ) {
    synchronized ( Clusters.class ) {
      if ( singleton == null ) singleton = new Clusters( );
    }
    return singleton;
  }



  public void update( List<RegisterClusterType> clusterChanges ) {
    EntityWrapper<ClusterInfo> db = new EntityWrapper<ClusterInfo>( );
    // :: remove old non-existant clusters :://
//    List<ClusterInfo> clusterList = db.query( new ClusterInfo( ) );
//    for ( ClusterInfo c : clusterList ) {
//      boolean found = false;
//      for ( RegisterClusterType newClusterInfo : clusterChanges ) {
//        if ( c.getName( ).equals( newClusterInfo.getName( ) ) ) {
//          found = true;
//          break;
//        }
//      }
//      if ( !found ) {
//        Cluster cluster = null;
//        try {
//          cluster = this.lookup( c.getName( ) );
//          db.delete( c );
//          this.deregister( cluster.getName( ) );
//          cluster.stop( );
//        } catch ( Exception e ) {
//          LOG.error( e, e );
//        }
//      }
//    }
//    db.commit( );

    // :: add the new entries / modify existing entries :://
//    for ( RegisterClusterType c : clusterChanges ) {
//      db = new EntityWrapper<ClusterInfo>( );
//      try {
//        ClusterInfo clusterInfo = db.getUnique( new ClusterInfo( c.getName( ) ) );
//        if ( !clusterInfo.getHost( ).equals( c.getHost( ) ) || clusterInfo.getPort( ) != c.getPort( ) ) {
//          clusterInfo.setHost( c.getHost( ) );
//          clusterInfo.setPort( c.getPort( ) );
//          Cluster cluster = this.lookup( c.getName( ) );
//          this.deregister( c.getName( ) );
//          cluster.stop( );
//          Cluster newCluster = new Cluster( clusterInfo );
//          this.register( newCluster );
//          db.commit( );
//          newCluster.start( );
//        } else db.rollback( );
//      } catch ( Exception e ) {
//        ClusterInfo clusterInfo = setupCluster( c );
//        db.add( clusterInfo );
//        db.commit( );
//      }
//    }
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
      list.add( c.getClusterInfo( ).getHostName( ) + ":" + c.getClusterInfo( ).getPort( ) );
    return list;
  }

}
