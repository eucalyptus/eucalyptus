package com.eucalyptus.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import com.eucalyptus.auth.Authentication;
import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.crypto.Certs;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.auth.util.PEMFiles;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.component.DatabaseServiceBuilder;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.DiscoverableServiceBuilder;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.DeregisterClusterType;
import edu.ucsb.eucalyptus.msgs.DescribeClustersType;
import edu.ucsb.eucalyptus.msgs.RegisterClusterType;

@DiscoverableServiceBuilder(com.eucalyptus.bootstrap.Component.cluster)
@Handles( { RegisterClusterType.class, DeregisterClusterType.class, DescribeClustersType.class } )
public class ClusterBuilder extends DatabaseServiceBuilder<ClusterConfiguration> {
  @Override
  public Boolean checkAdd( String name, String host, Integer port ) throws ServiceRegistrationException {
    if ( !testClusterCredentialsDirectory( name ) ) {
      throw new ServiceRegistrationException( "Cluster registration failed because the key directory cannot be created." );
    } else {
      return super.checkAdd( name, host, port );
    }
  }
  
  static boolean testClusterCredentialsDirectory( String name ) {
    String directory = SubDirectory.KEYS.toString( ) + File.separator + name;
    File keyDir = new File( directory );
    if ( !keyDir.exists( ) ) {
      try {
        keyDir.mkdir( );
        keyDir.delete( );
        return true;
      } catch ( Exception e ) {
        return false;
      }
    } else {
      return keyDir.canWrite( );
    }
  }
  
  @Override
  public ClusterConfiguration newInstance( ) {
    return new ClusterConfiguration( );
  }
  
  @Override
  public ClusterConfiguration newInstance( String name, String host, Integer port ) {
    return new ClusterConfiguration( name, host, port );
  }
  
  @Override
  public com.eucalyptus.component.Component getComponent( ) {
    return Components.lookup( Component.cluster );
  }
  
  @Override
  public ClusterConfiguration add( String name, String host, Integer port ) throws ServiceRegistrationException {
    ClusterConfiguration config = super.add( name, host, port );
    try {
      /** generate the Component keys **/
      String ccAlias = String.format( Configuration.CLUSTER_KEY_FSTRING, config.getName( ) );
      String ncAlias = String.format( Configuration.NODE_KEY_FSTRING, config.getName( ) );
      String directory = SubDirectory.KEYS.toString( ) + File.separator + config.getName( );
      File keyDir = new File( directory );
      Configuration.LOG.info( "creating keys in " + directory );
      if ( !keyDir.mkdir( ) && !keyDir.exists( ) ) { throw new EucalyptusCloudException( "Failed to create cluster key directory: " + keyDir.getAbsolutePath( ) ); }
      FileWriter out = null;
      try {
        KeyPair clusterKp = Certs.generateKeyPair( );
        X509Certificate clusterX509 = Certs.generateServiceCertificate( clusterKp, "cc-" + config.getName( ) );
        PEMFiles.write( directory + File.separator + "cluster-pk.pem", clusterKp.getPrivate( ) );
        PEMFiles.write( directory + File.separator + "cluster-cert.pem", clusterX509 );
    
        KeyPair nodeKp = Certs.generateKeyPair( );
        X509Certificate nodeX509 = Certs.generateServiceCertificate( nodeKp, "nc-" + config.getName( ) );
        PEMFiles.write( directory + File.separator + "node-pk.pem", nodeKp.getPrivate( ) );
        PEMFiles.write( directory + File.separator + "node-cert.pem", nodeX509 );
    
        X509Certificate systemX509 = SystemCredentialProvider.getCredentialProvider( Component.eucalyptus ).getCertificate( );
        String hexSig = Hmacs.generateSystemToken( "vtunpass".getBytes( ) );
        PEMFiles.write( SubDirectory.KEYS.toString( ) + File.separator + "cloud-cert.pem", systemX509 );
        out = new FileWriter( directory + File.separator + "vtunpass" );
        out.write( hexSig );
        out.flush( );
        out.close( );
    
        EntityWrapper<ClusterCredentials> credDb = Authentication.getEntityWrapper( );
        ClusterCredentials componentCredentials = new ClusterCredentials( config.getName( ) );
        try {
          List<ClusterCredentials> ccCreds = credDb.query( componentCredentials );
          for ( ClusterCredentials ccert : ccCreds ) {
            credDb.delete( ccert );
          }
          componentCredentials.setClusterCertificate( X509Cert.fromCertificate( clusterX509 ) );
          componentCredentials.setNodeCertificate( X509Cert.fromCertificate( nodeX509 ) );
          credDb.add( componentCredentials );
          credDb.commit( );
        } catch ( Exception e ) {
          Configuration.LOG.error( e, e );
          credDb.rollback( );
        }
      } catch ( Exception eee ) {
        throw new EucalyptusCloudException( eee );
      } finally {
        if(out != null)
        try {
          out.close();
        } catch (IOException e) {
          Configuration.LOG.error(e);
        }
      }
    } catch ( EucalyptusCloudException e ) {
      throw new ServiceRegistrationException( e.getMessage( ), e );
    }
    return config;
  }

  @Override
  public Boolean checkRemove( String name ) throws ServiceRegistrationException {
    try {
      Configuration.getStorageControllerConfiguration( name );
      throw new ServiceRegistrationException( "Cannot deregister a cluster controller when there is a storage controller registered." );
    } catch ( EucalyptusCloudException e ) {
      return true;
    }
  }


}
