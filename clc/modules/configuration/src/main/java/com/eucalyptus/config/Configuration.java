package com.eucalyptus.config;

import java.io.File;
import java.io.FileWriter;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Credentials;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.Hashes;
import com.eucalyptus.auth.util.KeyTool;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.SubDirectory;

import edu.ucsb.eucalyptus.msgs.ClusterInfoType;
import edu.ucsb.eucalyptus.msgs.RegisterClusterResponseType;
import edu.ucsb.eucalyptus.msgs.RegisterClusterType;
import edu.ucsb.eucalyptus.msgs.DeregisterClusterResponseType;
import edu.ucsb.eucalyptus.msgs.DeregisterClusterType;
import edu.ucsb.eucalyptus.msgs.DescribeClustersResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeClustersType;

public class Configuration {
  private static Logger LOG                 = Logger.getLogger( Configuration.class );
  private static String DB_NAME             = "eucalyptus_config";
  private static String CLUSTER_KEY_FSTRING = "cc-%s";
  private static String NODE_KEY_FSTRING    = "nc-%s";

  public static <T> EntityWrapper<T> getEntityWrapper( ) {
    return new EntityWrapper<T>( Configuration.DB_NAME );
  }

  public RegisterClusterResponseType registerCluster( RegisterClusterType request ) throws EucalyptusCloudException {
    RegisterClusterResponseType  reply = ( RegisterClusterResponseType ) request.getReply( );
    reply.set_return( true );
    if( this.checkClusterExists( request ) ) {
      return reply;
    }

    EntityWrapper<ClusterConfiguration> db = Configuration.getEntityWrapper( );
    ClusterConfiguration newCluster;
    try {
      newCluster = new ClusterConfiguration( request.getName( ), request.getHost( ), request.getPort( ) );
      db.add( newCluster );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
      LOG.error( e,e );
      throw new EucalyptusCloudException( e );
    }

    /** generate the cluster keys **/
    String ccAlias = String.format( CLUSTER_KEY_FSTRING, newCluster.getClusterName( ) );
    String ncAlias = String.format( NODE_KEY_FSTRING, newCluster.getClusterName( ) );
    String directory = SubDirectory.KEYS.toString( ) + File.separator + newCluster.getClusterName( );
    File keyDir = new File( directory );
    keyDir.mkdir( );
    LOG.info( "creating keys in " + directory );
    EntityWrapper<ClusterCredentials> credDb = Credentials.getEntityWrapper( );
    try {      
      // TODO: move this!!!!
      KeyTool keyTool = new KeyTool( );
      KeyPair clusterKp = keyTool.getKeyPair( );
      X509Certificate clusterX509 = keyTool.getCertificate( clusterKp, EucalyptusProperties.getDName( "cc-" + newCluster.getClusterName( ) ) );
      keyTool.writePem( directory + File.separator + "cluster-pk.pem", clusterKp.getPrivate( ) );
      keyTool.writePem( directory + File.separator + "cluster-cert.pem", clusterX509 );

      KeyPair nodeKp = keyTool.getKeyPair( );
      X509Certificate nodeX509 = keyTool.getCertificate( nodeKp, EucalyptusProperties.getDName( "nc-" + newCluster.getClusterName( ) ) );
      keyTool.writePem( directory + File.separator + "node-pk.pem", nodeKp.getPrivate( ) );
      keyTool.writePem( directory + File.separator + "node-cert.pem", nodeX509 );

      X509Certificate systemX509 = SystemCredentialProvider.getCredentialProvider( Component.Name.eucalyptus ).getCertificate( ); 
      keyTool.writePem( directory + File.separator + "cloud-cert.pem", systemX509 );
      Signature signer = Signature.getInstance( "SHA256withRSA" );
      signer.initSign( SystemCredentialProvider.getCredentialProvider( Component.Name.eucalyptus ).getPrivateKey( ) );
      signer.update( "eucalyptus".getBytes( ) );
      byte[] sig = signer.sign( );
      FileWriter out = new FileWriter( directory + File.separator + "vtunpass" );
      String hexSig = Hashes.bytesToHex(sig);
      out.write( hexSig );  
      out.flush( );
      out.close( );
      
      ClusterCredentials clusterCredentials = new ClusterCredentials( newCluster.getClusterName( ) );
      clusterCredentials.setClusterCertificate( X509Cert.fromCertificate( ccAlias, clusterX509 ) );
      clusterCredentials.setNodeCertificate( X509Cert.fromCertificate( ncAlias, nodeX509 ) );
      credDb.add( clusterCredentials );
      credDb.commit( );
    } catch ( Exception eee ) {
      credDb.rollback( );
      throw new EucalyptusCloudException( eee );
    }
    /*
     * TODO:
     * 1. try to add to db
     * 2. generate key pairs
     * 3. update registry
     */
    return reply;
  }

  private boolean checkClusterExists( RegisterClusterType request ) throws EucalyptusCloudException {
    EntityWrapper<ClusterConfiguration> db = Configuration.getEntityWrapper( );    
    ClusterConfiguration existingName = null;
    ClusterConfiguration existingHost = null;
    try {
      existingName = db.getUnique( new ClusterConfiguration( request.getName( ), request.getHost(), request.getPort() ) );
      return true;
    } catch ( Exception e1 ) {
      try {
        existingName = db.getUnique( ClusterConfiguration.byClusterName( request.getName( ) ) );
        existingHost = db.getUnique( ClusterConfiguration.byHostName( request.getHost( ) ) );
      } catch ( Exception e ) {
        if ( existingHost != null ) {
          throw new EucalyptusCloudException( "Cluster at host=" + existingHost.getHostName( ) + " already exists with name=" + request.getName() );
        } else if ( existingName != null ) {
          throw new EucalyptusCloudException( "Cluster with name=" + request.getName( ) + " already exists at host=" + existingName.getHostName( ) );
        }
      }
    } finally {
      db.rollback( );
    }
    return false;
  }

  public DeregisterClusterResponseType deregisterCluster( DeregisterClusterType request ) throws EucalyptusCloudException {
    EntityWrapper<ClusterConfiguration> db = Configuration.getEntityWrapper( );
    EntityWrapper<ClusterCredentials> credDb = Credentials.getEntityWrapper( );
    try {
      ClusterConfiguration clusterConfig = db.getUnique( ClusterConfiguration.byClusterName( request.getName( ) ) );
      ClusterCredentials clusterCredentials = new ClusterCredentials( clusterConfig.getClusterName( ) );
      String directory = SubDirectory.KEYS.toString( ) + File.separator + clusterConfig.getClusterName( );
      db.delete( clusterConfig );
      credDb.delete( clusterCredentials );
      File keyDir = new File( directory );
      for ( File f : keyDir.listFiles( ) ) {
        if ( !f.delete( ) ) {
          LOG.warn( "Failed to delete key file: " + f.getAbsolutePath( ) );
        }
      }
      if ( !keyDir.delete( ) ) {
        LOG.warn( "Failed to delete key directory: " + keyDir.getAbsolutePath( ) );
      }
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      credDb.rollback( );
      throw e;
    }
    DeregisterClusterResponseType reply = (DeregisterClusterResponseType) request.getReply( );
    reply.set_return( true );
    return reply;
  }

  public DescribeClustersResponseType listClusters( DescribeClustersType request ) {
    EntityWrapper<ClusterConfiguration> db = Configuration.getEntityWrapper( );
    try {
      List<ClusterConfiguration> clusterList = db.query( new ClusterConfiguration( ) );
      DescribeClustersResponseType reply = ( DescribeClustersResponseType ) request.getReply( );
      for ( ClusterConfiguration c : clusterList ) {
        reply.getClusters( ).add( new ClusterInfoType( c.getClusterName( ), c.getHostName( ) ) );
      }
      return reply;
    } finally {
      db.commit( );
    }
  }
}
