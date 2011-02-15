package com.eucalyptus.cluster;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Property;
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
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.auth.SystemCredentialProvider;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.DeregisterClusterType;
import com.eucalyptus.config.DescribeClustersType;
import com.eucalyptus.config.ModifyClusterAttributeType;
import com.eucalyptus.config.RegisterClusterType;
import com.eucalyptus.config.RemoteConfiguration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Iterables;

@DiscoverableServiceBuilder( com.eucalyptus.component.id.Cluster.class )
@Handles( { RegisterClusterType.class, DeregisterClusterType.class, DescribeClustersType.class, ClusterConfiguration.class, ModifyClusterAttributeType.class } )
public class ClusterBuilder extends DatabaseServiceBuilder<ClusterConfiguration> {
  private static Logger LOG                 = Logger.getLogger( ClusterBuilder.class );
  private static String CLUSTER_KEY_FSTRING = "cc-%s";
  private static String NODE_KEY_FSTRING    = "nc-%s";
  
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
      if ( Components.lookup( Eucalyptus.class ).isLocal( ) ) {
        if ( !Clusters.getInstance( ).contains( config.getName( ) ) ) {
          Cluster newCluster = new Cluster( ( ClusterConfiguration ) config );//TODO:GRZE:fix the type issue here.
          Clusters.getInstance( ).register( newCluster );
          newCluster.start( );
        } else {
          try {
            Cluster newCluster = Clusters.getInstance( ).lookup( config.getName( ) );
          } catch ( NoSuchElementException ex ) {
            Cluster newCluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
            newCluster.start( );
          }
        }
        super.fireStart( config );
      }
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex, ex );
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
    final ClusterConfiguration config = super.add( partition, name, host, port );
    File keyDir = ClusterBuilder.makeKeyDir( config );
    try {
      X509Certificate clusterX509;
      X509Certificate nodeX509;
      
      try {
        List<ClusterConfiguration> otherConfigs = ClusterBuilder.lookupPartition( config );
        ClusterConfiguration otherConfig = otherConfigs.get( 0 );
        clusterX509 = X509CertHelper.toCertificate( otherConfig.getClusterCertificate( ) );
        nodeX509 = X509CertHelper.toCertificate( otherConfig.getNodeCertificate( ) );
      } catch ( NoSuchElementException ex1 ) {
        /** generate the cluster/node keys **/
        KeyPair clusterKp;
        KeyPair nodeKp;
        try {
          clusterKp = Certs.generateKeyPair( );
          clusterX509 = Certs.generateServiceCertificate( clusterKp, String.format( CLUSTER_KEY_FSTRING, config.getName( ) ) );
          nodeKp = Certs.generateKeyPair( );
          nodeX509 = Certs.generateServiceCertificate( nodeKp, String.format( NODE_KEY_FSTRING, config.getName( ) ) );
          ClusterBuilder.writeClusterKeyFiles( config, keyDir, clusterKp, clusterX509, nodeKp, nodeX509 );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          throw new ServiceRegistrationException( "Failed to generate credentials for cluster: " + config, ex );
        }
      }
      
      try {
        final String clusterCert = X509CertHelper.fromCertificate( clusterX509 );
        final String nodeCert = X509CertHelper.fromCertificate( nodeX509 );
        Transactions.one( config, new Tx<ClusterConfiguration>( ) {
          
          @Override
          public void fire( ClusterConfiguration t ) throws Throwable {
            t.setClusterCertificate( clusterCert );
            t.setNodeCertificate( nodeCert );
          }
        } );
      } catch ( Throwable ex ) {
        throw new ServiceRegistrationException( "Failed to store cluster credentials during registration: " + config, ex );
      }
      
    } catch ( ServiceRegistrationException ex ) {
      ClusterBuilder.removeKeyDirectory( config );
      throw ex;
    } catch ( Throwable ex ) {
      ClusterBuilder.removeKeyDirectory( config );
      LOG.error( ex, ex );
      throw new ServiceRegistrationException( String.format( "Unexpected error caused cluster registration to fail for: partition=%s name=%s host=%s port=%d",
                                                             partition, name, host, port ), ex );
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
      LOG.error( ex, ex );
      ClusterBuilder.removeKeyDirectory( config );
      throw new ServiceRegistrationException( "Failed to write cluster credentials to disk: " + config, ex );
    } finally {
      if ( out != null ) try {
        out.close( );
        } catch ( IOException e ) {
        LOG.error( e, e );
        }
    }
  }
  
  private static void removeKeyDirectory( final ClusterConfiguration config ) {
    try {
      String otherClusters = Iterables.transform( ClusterBuilder.lookupPartition( config ), HasFullName.GET_FULLNAME ).toString( );
      LOG.info( String.format( "There still exist clusters within the partition=%s so the keys will not be removed.", config.getPartition( ), otherClusters ) );
    } catch ( NoSuchElementException ex1 ) {
      LOG.info( String.format( "Removing credentials for clusters within the partition=%s.", config.getPartition( ) ) );
      String directory = SubDirectory.KEYS.toString( ) + File.separator + config.getPartition( );
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
  }
  
  private static List<ClusterConfiguration> lookupPartition( final ClusterConfiguration config ) throws NoSuchElementException {
    EntityWrapper<ClusterConfiguration> db = EntityWrapper.get( ClusterConfiguration.class );
    try {
      Example ex = Example.create( new ClusterConfiguration( ) {
        {
          setPartition( config.getPartition( ) );
        }
      } ).enableLike( MatchMode.EXACT );
      Criteria criteria = db.createCriteria( ClusterConfiguration.class ).setCacheable( true ).add( ex ).add( Property.forName( "name" ).ne( config.getName( ) ) );
      List<ClusterConfiguration> configs = criteria.list( );
      if ( configs.size( ) == 0 ) {
        throw new NoSuchElementException( String.format( "Failed to locate any distinct clusters from the partition: partition=%s name!=%s",
                                                         config.getPartition( ), config.getName( ) ) );
      } else {
        return configs;
      }
    } catch ( NoSuchElementException ex ) {
      throw ex;
    } catch ( RuntimeException ex ) {
      LOG.error( ex, ex );
      db.rollback( );
      throw new NoSuchElementException(
                                        String.format( "Error occurred while trying to locate distinct clusters from the partition: partition=%s name!=%s because of %s.",
                                                       config.getPartition( ), config.getName( ), ex.getMessage( ) ) );
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
      ServiceConfigurations.getPartitionConfigurations( StorageControllerConfiguration.class, partition );
      throw new ServiceRegistrationException( "Cannot deregister a cluster controller when there is a storage controller registered." );
    } catch ( PersistenceException ex ) {
      LOG.error( ex, ex );
      return true;
    } catch ( NoSuchElementException ex ) {
      return true;
    }
  }
  
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Tearing down cluster: " + config );
    Cluster cluster = Clusters.getInstance( ).lookup( config.getName( ) );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_STOPPED, config.getComponentId( ).name( ), config.getName( ), config.getUri( ) ).info( );
    Cluster clusterInstance = Clusters.getInstance( ).lookup( config.getName( ) );
    clusterInstance.stop( );
//    Clusters.getInstance( ).deregister( config.getName( ) );
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
    return new RemoteConfiguration( this.getComponent( ).getComponentId( ), "cluster", "cluster", uri );
  }
  
  @Override
  public ClusterConfiguration remove( ServiceConfiguration config ) throws ServiceRegistrationException {
    ClusterBuilder.removeKeyDirectory( config );
    return super.remove( config );
  }
  
  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Enabling cluster: " + config );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_ENABLED, config.getComponentId( ).name( ), config.getName( ), config.getUri( ) ).info( );
    try {
      if ( Components.lookup( Eucalyptus.class ).isLocal( ) ) {
        if ( !Clusters.getInstance( ).contains( config.getName( ) ) ) {
          Cluster newCluster = new Cluster( ( ClusterConfiguration ) config );//TODO:GRZE:fix the type issue here.
          Clusters.getInstance( ).register( newCluster );
          newCluster.start( );
        } else {
          try {
            Cluster newCluster = Clusters.getInstance( ).lookup( config.getName( ) );
          } catch ( NoSuchElementException ex ) {
            Cluster newCluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
            Clusters.getInstance( ).enable( newCluster.getName( ) );
            newCluster.start( );
          }
        }
      }
      super.fireEnable( config );
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
    
  }
  
  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {
    LOG.info( "Disabling cluster: " + config );
    EventRecord.here( ClusterBuilder.class, EventType.COMPONENT_SERVICE_DISABLED, config.getComponentId( ).name( ), config.getName( ), config.getUri( ) ).info( );
    try {
      if ( Components.lookup( Eucalyptus.class ).isLocal( ) ) {
        if ( Clusters.getInstance( ).contains( config.getName( ) ) ) {
          try {
            Cluster newCluster = Clusters.getInstance( ).lookup( config.getName( ) );
            Clusters.getInstance( ).disable( newCluster.getName( ) );
            newCluster.stop( );
          } catch ( NoSuchElementException ex ) {
            Cluster newCluster = Clusters.getInstance( ).lookupDisabled( config.getName( ) );
            newCluster.stop( );
          }
        }
      }
      super.fireDisable( config );
    } catch ( NoSuchElementException ex ) {
      LOG.error( ex, ex );
    }
  }
  
  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {
    super.fireCheck( config );
  }
  
}
