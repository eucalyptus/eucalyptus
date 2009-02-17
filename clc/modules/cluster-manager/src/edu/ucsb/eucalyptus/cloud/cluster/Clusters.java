package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.keys.*;
import edu.ucsb.eucalyptus.util.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;

public class Clusters extends AbstractNamedRegistry<Cluster> {

  private static Logger LOG = Logger.getLogger( Clusters.class );

  private static Clusters singleton = getInstance();

  public static Clusters getInstance()
  {
    synchronized ( Clusters.class )
    {
      if ( singleton == null )
        singleton = new Clusters();
    }
    return singleton;
  }

  public Clusters()
  {
    this.init();
  }

  public void list()
  {

  }

  public void update( List<ClusterStateType> clusterChanges )
  {
    EntityWrapper<ClusterInfo> db = new EntityWrapper<ClusterInfo>();
    //:: remove old non-existant clusters :://
    List<ClusterInfo> clusterList = db.query( new ClusterInfo() );
    for( ClusterInfo c : clusterList ) {
      boolean found = false;
      for( ClusterStateType newClusterInfo : clusterChanges ) {
        if( c.getName().equals( newClusterInfo.getName() ) ) {
          found = true;
          break;
        }
      }
      if( !found ) {
        Cluster cluster = null;
        try {
          cluster = this.lookup( c.getName() );
          db.delete( c );
          this.deregister( cluster.getName() );
          cluster.stop();
        } catch ( Exception e ) {
          LOG.error( e, e );
        }
      }
    }
    db.commit();

    //:: add the new entries / modify existing entries :://
    for ( ClusterStateType c : clusterChanges )
    {
      db = new EntityWrapper<ClusterInfo>();
      try
      {
        ClusterInfo clusterInfo = db.getUnique( new ClusterInfo( c.getName() ) );
        if ( !clusterInfo.getHost().equals( c.getHost() ) || clusterInfo.getPort() != c.getPort() )
        {
          clusterInfo.setHost( c.getHost() );
          clusterInfo.setPort( c.getPort() );
          Cluster cluster = this.lookup( c.getName() );
          this.deregister( c.getName() );
          cluster.stop();
          Cluster newCluster = new Cluster( clusterInfo );
          this.register( newCluster );
          db.commit();
          newCluster.start();
        }
        else
          db.rollback();
      }
      catch ( Exception e )
      {
        ClusterInfo clusterInfo = setupCluster( c );
        db.add( clusterInfo );
        db.commit();
      }
    }
  }

  private ClusterInfo setupCluster( final ClusterStateType c )
  {
    ClusterInfo clusterInfo = new ClusterInfo( c.getName(), c.getHost(), c.getPort() );
    try
    {
      Clusters.addClusterKeys( clusterInfo.getName() );
    }
    catch ( Exception e1 )
    {
      LOG.error( e1, e1 );
    }
    this.register( new Cluster( clusterInfo ) );
    return clusterInfo;
  }

  private void init()
  {
    EntityWrapper<ClusterInfo> db = new EntityWrapper<ClusterInfo>();
    List<ClusterInfo> clusterList = db.query( new ClusterInfo() );
    //:: adding any new clusters :://
    for ( ClusterInfo info : clusterList )
    {
      Cluster newCluster = new Cluster( info );
      this.register( newCluster );
      newCluster.start();
    }
    db.commit();
  }


  public List<ClusterStateType> getClusters()
  {
    List<ClusterStateType> list = new ArrayList<ClusterStateType>();
    for ( Cluster c : this.listValues() )
      list.add( c.getWeb() );
    return list;
  }

  private static String CLUSTER_KEY_FSTRING = "cc-%s";
  private static String NODE_KEY_FSTRING = "nc-%s";

  private static void addClusterKeys( String name ) throws IOException, GeneralSecurityException
  {
    /** generate the cluster keys **/
    LOG.info( "creating keys in " + SubDirectory.KEYS.toString() );
    KeyTool keyTool = new KeyTool();
    AbstractKeyStore serviceKs = ServiceKeyStore.getInstance();

    String ccAlias = String.format( CLUSTER_KEY_FSTRING, name );
    String ncAlias = String.format( NODE_KEY_FSTRING, name );

    if ( !serviceKs.containsEntry( ccAlias ) || !serviceKs.containsEntry( ncAlias ) )
    {
      KeyPair clusterKp = keyTool.getKeyPair();
      X509Certificate clusterX509 = keyTool.getCertificate( clusterKp, EucalyptusProperties.getDName( "cc-" + name ) );
      keyTool.writePem( SubDirectory.KEYS.toString() + File.separator + "cluster-pk.pem", clusterKp.getPrivate() );
      keyTool.writePem( SubDirectory.KEYS.toString() + File.separator + "cluster-cert.pem", clusterX509 );
      serviceKs.addKeyPair( ccAlias, clusterX509, clusterKp.getPrivate(), ccAlias );

      KeyPair nodeKp = keyTool.getKeyPair();
      X509Certificate nodeX509 = keyTool.getCertificate( nodeKp, EucalyptusProperties.getDName( "nc-" + name ) );
      keyTool.writePem( SubDirectory.KEYS.toString() + File.separator + "node-pk.pem", nodeKp.getPrivate() );
      keyTool.writePem( SubDirectory.KEYS.toString() + File.separator + "node-cert.pem", nodeX509 );
      serviceKs.addKeyPair( ncAlias, nodeX509, nodeKp.getPrivate(), ncAlias );

      serviceKs.store();
    }
  }
}
