package com.eucalyptus.cluster;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Authentication;
import com.eucalyptus.auth.crypto.Certs;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.auth.util.PEMFiles;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.DatabaseServiceBuilder;
import com.eucalyptus.component.DiscoverableServiceBuilder;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.auth.SystemCredentialProvider;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.ClusterCredentials;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.DeregisterClusterType;
import com.eucalyptus.config.DescribeClustersType;
import com.eucalyptus.config.ModifyClusterAttributeType;
import com.eucalyptus.config.RegisterClusterType;
import com.eucalyptus.config.RemoteConfiguration;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.EucalyptusCloudException;

@DiscoverableServiceBuilder( com.eucalyptus.component.id.Cluster.class )
@Handles( { RegisterClusterType.class, DeregisterClusterType.class, DescribeClustersType.class, ClusterConfiguration.class, ModifyClusterAttributeType.class } )
public class ClusterBuilder extends DatabaseServiceBuilder<ClusterConfiguration> {
  private static Logger LOG = Logger.getLogger( ClusterBuilder.class );
  private static String         CLUSTER_KEY_FSTRING = "cc-%s";
  private static String         NODE_KEY_FSTRING    = "nc-%s";
  @Override
  public Boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    if ( !testClusterCredentialsDirectory( name ) ) {
      throw new ServiceRegistrationException( "Cluster registration failed because the key directory cannot be created." );
    } else {
      return super.checkAdd( partition, name, host, port );
    }
  }
  
  static boolean testClusterCredentialsDirectory( String name ) {
    String directory = SubDirectory.KEYS.toString( ) + File.separator + name;
    File keyDir = new File( directory );
    if ( !keyDir.exists( ) ) {
      try {
        return keyDir.mkdir( ) && keyDir.canWrite( );
      } catch ( Exception e ) {
        return false;
      }
    } else {
      return keyDir.canWrite( );
    }
  }
  
  /**
   * @see com.eucalyptus.component.AbstractServiceBuilder#fireStart(com.eucalyptus.component.ServiceConfiguration)
   * @param config
   * @throws ServiceRegistrationException
   */
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Starting up cluster: " + config );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_START, config.getComponentId( ).name( ), config.getName( ), config.getUri( ) ).info( );
    try {
      if( Components.lookup( Eucalyptus.class ).isLocal( ) ) {
        try {
          ClusterCredentials credentials = null;//ASAP: fix it.
          if ( !Clusters.getInstance( ).contains( config.getName( ) ) ) {
            EntityWrapper<ClusterCredentials> credDb = EntityWrapper.get( ClusterCredentials.class );
            try {
              credentials = credDb.getUnique( new ClusterCredentials( config.getName( ) ) );
              credDb.commit( );
            } catch ( EucalyptusCloudException e ) {
              LOG.error( "Failed to load credentials for cluster: " + config.getName( ) );
              credDb.rollback( );
              throw e;
            }
            Cluster newCluster = new Cluster( ( ClusterConfiguration ) config, credentials );//TODO:GRZE:fix the type issue here.
            Clusters.getInstance( ).register( newCluster );
            newCluster.start( );
          } 
        } catch ( EucalyptusCloudException ex ) {
          LOG.error( ex , ex );
          throw new ServiceRegistrationException( "Registration failed: " + ex.getMessage( ), ex );
        }
        super.fireStart( config );
      }
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex , ex );
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
    return Components.lookup( com.eucalyptus.component.id.Cluster.class );
  }
  
  @Override
  public ClusterConfiguration add( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    ClusterConfiguration config = super.add( partition, name, host, port );
    /** generate the cluster/node keys **/
    KeyPair clusterKp;
    X509Certificate clusterX509;
    KeyPair nodeKp;
    X509Certificate nodeX509;
    try {
      clusterKp = Certs.generateKeyPair( );
      clusterX509 = Certs.generateServiceCertificate( clusterKp, String.format( CLUSTER_KEY_FSTRING, config.getName( ) ) );
      nodeKp = Certs.generateKeyPair( );
      nodeX509 = Certs.generateServiceCertificate( nodeKp, String.format( NODE_KEY_FSTRING, config.getName( ) ) );
    } catch ( Exception ex ) {
      LOG.error( ex , ex );
      throw new ServiceRegistrationException( "Failed to generate credentials for cluster: " + config, ex );
    }
    File keyDir = ClusterBuilder.makeKeyDir( config );
    try {
      ClusterBuilder.writeClusterKeyFiles( config, keyDir, clusterKp, clusterX509, nodeKp, nodeX509 );
      ClusterBuilder.storeClusterCredentials( config, clusterX509, nodeX509 );
    } catch ( ServiceRegistrationException ex ) {
      ClusterBuilder.removeKeyDirectory( keyDir );
      throw ex;
    } catch ( Throwable ex ) {
      ClusterBuilder.removeKeyDirectory( keyDir );
      LOG.error( ex , ex );
      throw new ServiceRegistrationException( String.format( "Unexpected error caused cluster registration to fail for: partition=%s name=%s host=%s port=%d", partition, name, host, port ), ex );  
    }
    return config;
  }

  private static void writeClusterKeyFiles( ClusterConfiguration config, File keyDir, KeyPair clusterKp, X509Certificate clusterX509, KeyPair nodeKp, X509Certificate nodeX509 ) throws ServiceRegistrationException {
    X509Certificate systemX509 = SystemCredentialProvider.getCredentialProvider( Eucalyptus.class ).getCertificate( );
    FileWriter out = null;
    try {
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "cluster-pk.pem", clusterKp.getPrivate( ) );
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "cluster-cert.pem", clusterX509 );        
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "node-pk.pem", nodeKp.getPrivate( ) );
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "node-cert.pem", nodeX509 );
      
      String hexSig = Hmacs.generateSystemToken( "vtunpass".getBytes( ) );
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "cloud-cert.pem", systemX509 );
      out = new FileWriter( keyDir.getAbsolutePath( ) + File.separator + "vtunpass" );
      out.write( hexSig );
      out.flush( );
      out.close( );
    } catch ( Throwable ex ) {
      LOG.error( ex , ex );
      ClusterBuilder.removeKeyDirectory( keyDir );      
      throw new ServiceRegistrationException( "Failed to write cluster credentials to disk: " + config, ex );
    } finally {
      if ( out != null ) try {
        out.close( );
      } catch ( IOException e ) {
        LOG.error( e, e );
      }
  }
  }

  private static void removeKeyDirectory( File keyDir ) {
    for( File f : keyDir.listFiles( ) ) {
      if( f.isFile( ) && !f.delete( ) ) {
        LOG.error( "Failed to delete file after error in cluster registration: " + f.getAbsolutePath( ) );
      }
    }
    if( !keyDir.delete( ) ) {
      LOG.error( "Failed to delete stale key directory after error in cluster registration: " + keyDir.getAbsolutePath( ) );
    }
  }

  private static void storeClusterCredentials( ClusterConfiguration config, X509Certificate clusterX509, X509Certificate nodeX509 ) throws ServiceRegistrationException {
    EntityWrapper<ClusterCredentials> credDb = EntityWrapper.get( ClusterCredentials.class );
    try {
      List<ClusterCredentials> ccCreds = credDb.query( new ClusterCredentials( config.getName( ) ) );
      for ( ClusterCredentials ccert : ccCreds ) {
        credDb.delete( ccert );
      }
      ClusterCredentials componentCredentials = new ClusterCredentials( config.getName( ) );
      componentCredentials.setClusterCertificate( X509CertHelper.fromCertificate( clusterX509 ) );
      componentCredentials.setNodeCertificate( X509CertHelper.fromCertificate( nodeX509 ) );
      credDb.add( componentCredentials );
      credDb.commit( );
    } catch ( Throwable e ) {
      LOG.error( e, e );
      credDb.rollback( );
      throw new ServiceRegistrationException( "Failed to store cluster credentials during registration: " + config, e );
    }
  }

  private static File makeKeyDir( ClusterConfiguration config ) throws ServiceRegistrationException {
    String directory = SubDirectory.KEYS.toString( ) + File.separator + config.getName( );
    File keyDir = new File( directory );
    LOG.info( "creating keys in " + directory );
    if ( !keyDir.exists( ) && !keyDir.mkdir( ) ) {
      throw new ServiceRegistrationException( "Failed to create cluster key directory: " + keyDir.getAbsolutePath( ) );
    }
    return keyDir;
  }
  
  @Override
  public Boolean checkRemove( String partition, String name ) throws ServiceRegistrationException {
    try {
      Configuration.getStorageControllerConfiguration( name );
      throw new ServiceRegistrationException( "Cannot deregister a cluster controller when there is a storage controller registered." );
    } catch ( EucalyptusCloudException e ) {
      return true;
    }
  }
  
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Tearing down cluster: " + config );
    Cluster cluster = Clusters.getInstance( ).lookup( config.getName( ) );
    EntityWrapper<ClusterCredentials> credDb = Authentication.getEntityWrapper( );
    try {
      List<ClusterCredentials> ccCreds = credDb.query( new ClusterCredentials( config.getName( ) ) );
      for ( ClusterCredentials ccert : ccCreds ) {
        credDb.delete( ccert );
      }
      credDb.commit( );
    } catch ( Exception e ) {
      LOG.error( e, e );
      credDb.rollback( );
    }
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_STOPPED, config.getComponentId( ).name( ), config.getName( ), config.getUri( ) ).info( );
    Cluster clusterInstance = Clusters.getInstance( ).lookup( config.getName( ) );
    clusterInstance.stop( );
    Clusters.getInstance( ).deregister( config.getName( ) );
    String directory = SubDirectory.KEYS.toString( ) + File.separator + config.getName( );
    File keyDir = new File( directory );
    if ( keyDir.exists( ) ) {
      for( File f : keyDir.listFiles( ) ) {
        if( f.delete( ) ) {
          LOG.info( "Removing cluster key file: " + f.getAbsolutePath( ) );
        } else {
          LOG.info( "Failed to remove cluster key file: " + f.getAbsolutePath( ) );
        }        
      }
      if( keyDir.delete( ) ) {
        LOG.info( "Removing cluster key directory: " + keyDir.getAbsolutePath( ) );
      } else {
        LOG.info( "Failed to remove cluster key directory: " + keyDir.getAbsolutePath( ) );
      }
    }    
    super.fireStop( config );
  }

  /**
   * @see com.eucalyptus.component.DatabaseServiceBuilder#add(java.net.URI)
   * @param uri
   * @return
   * @throws ServiceRegistrationException
   */
  @Override
  public ServiceConfiguration toConfiguration( URI uri ) throws ServiceRegistrationException {
    return new RemoteConfiguration( null, this.getComponent( ).getIdentity( ), uri );
  }
  
}
