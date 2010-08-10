package com.eucalyptus.cluster;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Authentication;
import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.crypto.Certs;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.AvailabilityZonePermission;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.util.PEMFiles;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.DatabaseServiceBuilder;
import com.eucalyptus.component.DiscoverableServiceBuilder;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.Handles;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.DeregisterClusterType;
import edu.ucsb.eucalyptus.msgs.DescribeClustersType;
import edu.ucsb.eucalyptus.msgs.RegisterClusterType;

@DiscoverableServiceBuilder( com.eucalyptus.bootstrap.Component.cluster )
@Handles( { RegisterClusterType.class, DeregisterClusterType.class, DescribeClustersType.class } )
public class ClusterBuilder extends DatabaseServiceBuilder<ClusterConfiguration> {
  private static Logger LOG = Logger.getLogger( ClusterBuilder.class );
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
        return keyDir.mkdir( ) && keyDir.canWrite( );
      } catch ( Exception e ) {
        return false;
      }
    } else {
      return keyDir.canWrite( );
    }
  }
  
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_START, config.getComponent( ).name( ), config.getName( ), config.getUri( ) ).info( );
    try {
      if( Components.lookup( Components.delegate.eucalyptus ).isLocal( ) ) {
        try {
          Clusters.start( ( ClusterConfiguration ) config );
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
  public ClusterConfiguration newInstance( String name, String host, Integer port ) {
    return new ClusterConfiguration( name, host, port );
  }
  
  @Override
  public com.eucalyptus.component.Component getComponent( ) {
    return Components.lookup( Component.cluster );
  }
  private static String         CLUSTER_KEY_FSTRING = "cc-%s";
  private static String         NODE_KEY_FSTRING    = "nc-%s";
  
  @Override
  public ClusterConfiguration add( String name, String host, Integer port ) throws ServiceRegistrationException {
    ClusterConfiguration config = super.add( name, host, port );
    try {
      /** generate the Component keys **/
      String ccAlias = String.format( CLUSTER_KEY_FSTRING, config.getName( ) );
      String ncAlias = String.format( NODE_KEY_FSTRING, config.getName( ) );
      String directory = SubDirectory.KEYS.toString( ) + File.separator + config.getName( );
      File keyDir = new File( directory );
      LOG.info( "creating keys in " + directory );
      if ( !keyDir.mkdir( ) && !keyDir.exists( ) ) {
        throw new EucalyptusCloudException( "Failed to create cluster key directory: " + keyDir.getAbsolutePath( ) );
      }
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
        PEMFiles.write( directory + File.separator + "cloud-cert.pem", systemX509 );
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
          LOG.error( e, e );
          credDb.rollback( );
        }
      } catch ( Exception eee ) {
        throw new EucalyptusCloudException( eee );
      } finally {
        if ( out != null ) try {
          out.close( );
        } catch ( IOException e ) {
          LOG.error( e );
        }
      }
    } catch ( EucalyptusCloudException e ) {
      throw new ServiceRegistrationException( e.getMessage( ), e );
    }
    Groups.DEFAULT.addAuthorization( new AvailabilityZonePermission( config.getName( ) ) );
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
  
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    Cluster cluster = Clusters.getInstance( ).lookup( config.getName( ) );
    EntityWrapper<ClusterCredentials> credDb = Authentication.getEntityWrapper( );
    try {
      ClusterCredentials creds = credDb.getUnique( new ClusterCredentials( cluster.getName( ) ) );
      credDb.delete( creds );
      credDb.commit( );
    } catch ( EucalyptusCloudException ex ) {
      LOG.error( ex , ex );
      credDb.rollback( );
    }
    Clusters.stop( cluster.getName( ) );
    for( Group g : Groups.listAllGroups( ) ) {
      for( Authorization auth : g.getAuthorizations( ) ) {
        if( auth instanceof AvailabilityZonePermission && config.getName( ).equals( auth.getValue() ) ) {
          g.removeAuthorization( auth );
        }
      }
    }
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
  
}
