package com.eucalyptus.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Credentials;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.crypto.Certs;
import com.eucalyptus.auth.crypto.Crypto;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.auth.util.PEMFiles;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.sysinfo.SubDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.ConfigurationMessage;
import edu.ucsb.eucalyptus.msgs.RegisterComponentType;

public class ConfigurationUtil {
  static Map<String, Class> typeMap = new HashMap<String, Class>( ) {
    {
      put( "Cluste", ClusterConfiguration.class );
      put( "Storag", StorageControllerConfiguration.class );
      put( "Walrus", WalrusConfiguration.class );
    }
  };

  static ComponentConfiguration getConfigurationInstance( ConfigurationMessage request, Object... args ) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Class[] classList = new Class[args.length];
    for ( int i = 0; i < args.length; i++ ) {
      classList[i] = args[i].getClass( );
    }
    String cname = request.getComponentName( );
    Class cclass = typeMap.get( cname );
    ComponentConfiguration configInstance = ( ComponentConfiguration ) cclass.getConstructor( classList ).newInstance( args );
    return configInstance;
  }

  static boolean testClusterCredentialsDirectory( String name ) {
    String directory = SubDirectory.KEYS.toString( ) + File.separator + name;
    File keyDir = new File( directory );
    if( !keyDir.exists( ) ) {
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
  
  static void setupClusterCredentials( ComponentConfiguration newComponent ) throws EucalyptusCloudException {
    /** generate the Component keys **/
    String ccAlias = String.format( Configuration.CLUSTER_KEY_FSTRING, newComponent.getName( ) );
    String ncAlias = String.format( Configuration.NODE_KEY_FSTRING, newComponent.getName( ) );
    String directory = SubDirectory.KEYS.toString( ) + File.separator + newComponent.getName( );
    File keyDir = new File( directory );
    Configuration.LOG.info( "creating keys in " + directory );
    if ( !keyDir.mkdir( ) && !keyDir.exists( ) ) { throw new EucalyptusCloudException( "Failed to create cluster key directory: " + keyDir.getAbsolutePath( ) ); }
    FileWriter out = null;
    try {
      KeyPair clusterKp = Certs.generateKeyPair( );
      X509Certificate clusterX509 = Certs.generateServiceCertificate( clusterKp, "cc-" + newComponent.getName( ) );
      PEMFiles.writePem( directory + File.separator + "cluster-pk.pem", clusterKp.getPrivate( ) );
      PEMFiles.writePem( directory + File.separator + "cluster-cert.pem", clusterX509 );
  
      KeyPair nodeKp = Certs.generateKeyPair( );
      X509Certificate nodeX509 = Certs.generateServiceCertificate( nodeKp, "nc-" + newComponent.getName( ) );
      PEMFiles.writePem( directory + File.separator + "node-pk.pem", nodeKp.getPrivate( ) );
      PEMFiles.writePem( directory + File.separator + "node-cert.pem", nodeX509 );
  
      X509Certificate systemX509 = SystemCredentialProvider.getCredentialProvider( Component.eucalyptus ).getCertificate( );
      String hexSig = Hmacs.generateSystemToken( "vtunpass".getBytes( ) );
      PEMFiles.writePem( SubDirectory.KEYS.toString( ) + File.separator + "cloud-cert.pem", systemX509 );
      out = new FileWriter( directory + File.separator + "vtunpass" );
      out.write( hexSig );
      out.flush( );
      out.close( );
  
      EntityWrapper<ClusterCredentials> credDb = Credentials.getEntityWrapper( );
      ClusterCredentials componentCredentials = new ClusterCredentials( newComponent.getName( ) );
      try {
        List<ClusterCredentials> ccCreds = credDb.query( componentCredentials );
        for ( ClusterCredentials ccert : ccCreds ) {
          credDb.delete( ccert );
        }
        componentCredentials.setClusterCertificate( X509Cert.fromCertificate( ccAlias, clusterX509 ) );
        componentCredentials.setNodeCertificate( X509Cert.fromCertificate( ncAlias, nodeX509 ) );
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
  }

  static boolean checkComponentExists( RegisterComponentType request ) throws EucalyptusCloudException {
    EntityWrapper<ComponentConfiguration> db = Configuration.getEntityWrapper( );
  
    ComponentConfiguration existingName = null;
    ComponentConfiguration existingHost = null;
  
    try {
      ComponentConfiguration searchConfig = getConfigurationInstance( request, request.getName( ), request.getHost( ), request.getPort( ) );
      existingName = db.getUnique( searchConfig );
      db.rollback( );
      return true;
    } catch ( Exception e ) {
      try {
        ComponentConfiguration searchConfig = getConfigurationInstance( request );
        searchConfig.setName( request.getName( ) );
        existingName = db.getUnique( searchConfig );
        db.rollback( );
      } catch ( Exception e1 ) {
        try {
          ComponentConfiguration searchConfig = getConfigurationInstance( request );
          searchConfig.setHostName( request.getHost( ) );
          existingHost = db.getUnique( searchConfig );
          db.rollback( );
        } catch ( Exception e2 ) {
          db.rollback( );
        }
      }
    }
    if ( existingName != null ) {
      throw new EucalyptusCloudException( "Component with name=" + request.getName( ) + " already exists at host=" + existingName.getHostName( ) );
    } else if ( existingHost != null ) { throw new EucalyptusCloudException( "Component at host=" + existingHost.getHostName( ) + " already exists with name=" + request.getName( ) ); }
    return false;
  }

  static void removeClusterCredentials( String clusterName ) {
    EntityWrapper<ClusterCredentials> credDb = Credentials.getEntityWrapper( );
    try {
      ClusterCredentials clusterCredentials = credDb.getUnique( new ClusterCredentials( clusterName ) );
      credDb.recast( X509Cert.class ).delete( clusterCredentials.getClusterCertificate( ) );
      credDb.recast( X509Cert.class ).delete( clusterCredentials.getNodeCertificate( ) );
      credDb.delete( clusterCredentials );
      credDb.commit( );
    } catch ( Exception e ) {
      Configuration.LOG.error( e, e );
      credDb.rollback( );
    }
    try {
      String directory = SubDirectory.KEYS.toString( ) + File.separator + clusterName;
      File keyDir = new File( directory );
      for ( File f : keyDir.listFiles( ) ) {
        if ( !f.delete( ) ) {
          Configuration.LOG.warn( "Failed to delete key file: " + f.getAbsolutePath( ) );
        }
      }
      if ( !keyDir.delete( ) ) {
        Configuration.LOG.warn( "Failed to delete key directory: " + keyDir.getAbsolutePath( ) );
      }
    } catch ( Exception e ) {
    }
  }

}
