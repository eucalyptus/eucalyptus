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
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.config.Handles;
import com.eucalyptus.config.RemoteConfiguration;
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
  public Boolean checkAdd( String name, String host, Integer port ) throws ServiceRegistrationException {
    if ( !testClusterCredentialsDirectory( name ) ) {
      throw new ServiceRegistrationException( "Cluster registration failed because the key directory cannot be created." );
    } else {
      return super.checkAdd( name, host, port );
    }
  }
  
  @Override
  public ClusterConfiguration add( String name, String host, Integer port ) throws ServiceRegistrationException {
    ClusterConfiguration config = this.newInstance( name, host, port );
    removeCredentials( config );
    try {
      /** generate the Component keys **/
      addCredentials( config );
      addAuthorizations( config );
      ServiceConfigurations.getInstance( ).store( config );
    } catch ( EucalyptusCloudException e ) {
      throw new ServiceRegistrationException( e.getMessage( ), e );
    }
    return config;
  }

  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_START, config.getComponent( ).name( ), config.getName( ), config.getUri( ) ).info( );
    try {
      if ( Components.lookup( Components.delegate.eucalyptus ).isLocal( ) ) {
        try {
          Clusters.start( ( ClusterConfiguration ) config );
        } catch ( EucalyptusCloudException ex ) {
          LOG.error( ex, ex );
          throw new ServiceRegistrationException( "Registration failed: " + ex.getMessage( ), ex );
        }
        super.fireStart( config );
      }
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
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
  public ClusterConfiguration remove( ServiceConfiguration removeConfig ) throws ServiceRegistrationException {
    ClusterConfiguration config = ( ClusterConfiguration ) removeConfig;
    removeAuthorizations( config );
    removeKeys( config );
    removeCredentials( config );
    ServiceConfigurations.getInstance( ).remove( config );
    return ( ClusterConfiguration ) config;
  }

  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    Clusters.stop( config.getName( ) );
    super.fireStop( config );
  }
  
  @Override
  public ServiceConfiguration toConfiguration( URI uri ) throws ServiceRegistrationException {
    return new RemoteConfiguration( this.getComponent( ).getPeer( ), uri );
  }

  private static void addAuthorizations( ClusterConfiguration config ) {
    Groups.DEFAULT.addAuthorization( new AvailabilityZonePermission( config.getName( ) ) );
  }
  
  private static void addCredentials( ClusterConfiguration config ) throws ServiceRegistrationException {
    String directory = SubDirectory.KEYS.toString( ) + File.separator + config.getName( );
    File keyDir = new File( directory );
    LOG.info( "creating keys in " + directory );
    if ( !keyDir.mkdir( ) && !keyDir.exists( ) ) {
      throw new ServiceRegistrationException( "Failed to create cluster key directory: " + keyDir.getAbsolutePath( ) );
    } else if ( !keyDir.canWrite( ) ) {
      throw new ServiceRegistrationException( "Cluster key directory is not writeable: " + keyDir.getAbsolutePath( ) );
    }
    
    KeyPair clusterKp = Certs.generateKeyPair( );
    X509Certificate clusterX509 = Certs.generateServiceCertificate( clusterKp, "cc-" + config.getName( ) );
    KeyPair nodeKp = Certs.generateKeyPair( );
    X509Certificate nodeX509 = Certs.generateServiceCertificate( nodeKp, "nc-" + config.getName( ) );
    FileWriter out = null;
    try {
      PEMFiles.write( directory + File.separator + "cluster-pk.pem", clusterKp.getPrivate( ) );
      PEMFiles.write( directory + File.separator + "cluster-cert.pem", clusterX509 );
      PEMFiles.write( directory + File.separator + "node-pk.pem", nodeKp.getPrivate( ) );
      PEMFiles.write( directory + File.separator + "node-cert.pem", nodeX509 );
      X509Certificate systemX509 = SystemCredentialProvider.getCredentialProvider( Component.eucalyptus ).getCertificate( );
      String hexSig = Hmacs.generateSystemToken( "vtunpass".getBytes( ) );
      PEMFiles.write( directory + File.separator + "cloud-cert.pem", systemX509 );
      out = new FileWriter( directory + File.separator + "vtunpass" );
      out.write( hexSig );
      out.flush( );
      out.close( );
    } catch ( IOException ex ) {
      LOG.error( ex, ex );
      throw new ServiceRegistrationException( "Failed to store cluster keys: " + ex.getMessage( ), ex );
    } finally {
      if ( out != null ) {
        try {
          out.close( );
        } catch ( IOException e ) {
          LOG.error( e );
        }
      }
    }
    EntityWrapper<ClusterCredentials> credDb = Authentication.getEntityWrapper( );
    try {
      ClusterCredentials componentCredentials = new ClusterCredentials( config.getName( ) );
      try {
        componentCredentials = credDb.getUnique( componentCredentials );
        componentCredentials.setClusterCertificate( X509Cert.fromCertificate( clusterX509 ) );
        componentCredentials.setNodeCertificate( X509Cert.fromCertificate( nodeX509 ) );
        credDb.merge( componentCredentials );
      } catch ( Exception ex ) {
        componentCredentials.setClusterCertificate( X509Cert.fromCertificate( clusterX509 ) );
        componentCredentials.setNodeCertificate( X509Cert.fromCertificate( nodeX509 ) );
        credDb.add( componentCredentials );
      }
      credDb.commit( );
    } catch ( Exception e ) {
      LOG.error( e, e );
      credDb.rollback( );
    }
  }
  
  private static void removeAuthorizations( ServiceConfiguration config ) {
    for ( Group g : Groups.listAllGroups( ) ) {
      for ( Authorization auth : g.getAuthorizations( ) ) {
        if ( auth instanceof AvailabilityZonePermission && config.getName( ).equals( auth.getValue( ) ) ) {
          g.removeAuthorization( auth );
        }
      }
    }
  }
  
  private static void removeCredentials( ClusterConfiguration config ) {
    EntityWrapper<ClusterCredentials> credDb = EntityWrapper.get( ClusterCredentials.class );
    try {
      for ( ClusterCredentials ccert : credDb.query( new ClusterCredentials( ) ) ) {
        LOG.debug( "Checking cluster certificate: " + ccert.getClusterName( ) + "\n" + X509Cert.toCertificate( ccert.getClusterCertificate( ) ) + "\n" + X509Cert.toCertificate( ccert.getNodeCertificate( ) ) );
        if( config.getName( ).equals( ccert.getClusterName( ) ) ) {
          credDb.recast( X509Cert.class ).delete( ccert.getClusterCertificate( ) );
          credDb.recast( X509Cert.class ).delete( ccert.getNodeCertificate( ) );
          credDb.delete( ccert );
          LOG.debug( "Deleting cluster certificate: " + ccert.getClusterName( ) + "\n" + X509Cert.toCertificate( ccert.getClusterCertificate( ) ) + "\n" + X509Cert.toCertificate( ccert.getNodeCertificate( ) ) );
        }
      }
      credDb.commit( );
    } catch ( Exception e ) {
      LOG.error( e, e );
      credDb.rollback( );
    }
  }
  
  private static synchronized void removeKeys( ServiceConfiguration config ) {
    String directory = SubDirectory.KEYS.toString( ) + File.separator + config.getName( );
    File keyDir = new File( directory );
    if ( keyDir.exists( ) ) {
      for ( File f : keyDir.listFiles( ) ) {
        if ( f.delete( ) ) {
          LOG.info( "Removing cluster key file: " + f.getAbsolutePath( ) );
        } else {
          LOG.info( "Failed to remove cluster key file: " + f.getAbsolutePath( ) );
        }
      }
      if ( keyDir.delete( ) ) {
        LOG.info( "Removing cluster key directory: " + keyDir.getAbsolutePath( ) );
      } else {
        LOG.info( "Failed to remove cluster key directory: " + keyDir.getAbsolutePath( ) );
      }
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
}
