package com.eucalyptus.config;

import java.io.File;
import com.eucalyptus.auth.Authentication;
import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.system.SubDirectory;

public class ConfigurationUtil {


  static void removeClusterCredentials( String clusterName ) {
    EntityWrapper<ClusterCredentials> credDb = Authentication.getEntityWrapper( );
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
