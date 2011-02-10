package com.eucalyptus.component;

import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;

public class ServiceConfigurations {
  private static Logger                       LOG       = Logger.getLogger( ServiceConfigurations.class );
  private static ServiceConfigurationProvider singleton = new DatabaseServiceConfigurationProvider( );
  
  public static ServiceConfigurationProvider getInstance( ) {
    return singleton;
  }
  
  public static <T> EntityWrapper<T> getEntityWrapper( ) {
    return new EntityWrapper<T>( "eucalyptus_config" );
  }
  
  //  public static StorageControllerConfiguration lookupSc( final String requestedZone ) throws EucalyptusCloudException {
  //    return getStorageControllerConfiguration( requestedZone );
  //  }
  //  

  public static <T extends ServiceConfiguration> List<T> getConfigurations( Class<T> type ) throws PersistenceException {
    if( ComponentConfiguration.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    EntityWrapper<T> db = EntityWrapper.get( type );
    List<T> componentList;
    try {
      componentList = db.query( type.newInstance( ) );
      db.commit( );
      return componentList;
    } catch ( PersistenceException ex ) {
      LOG.error( ex , ex );
      db.rollback( );
      throw ex;
    } catch ( Throwable ex ) {
      throw new PersistenceException( ex );
    }
  }

  public static <T extends ServiceConfiguration> List<T> getPartitionConfigurations( Class<T> type, String partition ) throws PersistenceException {
    if( ComponentConfiguration.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    EntityWrapper<T> db = EntityWrapper.get( type );
    List<T> componentList;
    try {
      T conf = type.newInstance( );
      conf.setName( partition );
      componentList = db.query( conf );
      db.commit( );
      if( componentList.isEmpty( ) ) {
        throw new NoSuchElementException( "Failed to lookup registration for " + type.getSimpleName( ) + " in partition: " + partition ); 
      }
      return componentList;
    } catch ( NoSuchElementException ex ) {
      throw ex;
    } catch ( PersistenceException ex ) {
      LOG.error( ex , ex );
      db.rollback( );
      throw ex;
    } catch ( Throwable ex ) {
      throw new PersistenceException( ex );
    }
  }

  public static <T extends ServiceConfiguration> T getConfiguration( Class<T> type, String uniqueName ) throws PersistenceException, NoSuchElementException {
    if( ComponentConfiguration.class.isAssignableFrom( type ) ) {
      throw new PersistenceException( "Unknown configuration type passed: " + type.getCanonicalName( ) );
    }
    EntityWrapper<T> db = EntityWrapper.get( type );
    try {
      T conf = type.newInstance( );
      conf.setName( uniqueName );
      T configuration = db.getUnique( conf );
      db.commit( );
      return configuration;
    } catch ( EucalyptusCloudException ex ) {
      LOG.error( ex , ex );
      db.rollback( );
      throw new NoSuchElementException( ex.getMessage( ) );
    } catch ( PersistenceException ex ) {
      LOG.error( ex , ex );
      db.rollback( );
      throw ex;
    } catch ( Throwable ex ) {
      LOG.error( ex , ex );
      db.rollback( );
      throw new PersistenceException( ex );
    }
  }

  
  
//  public static List<ClusterConfiguration> getClusterConfigurations( ) throws EucalyptusCloudException {
//    EntityWrapper<ClusterConfiguration> db = getEntityWrapper( );
//    try {
//      List<ClusterConfiguration> componentList = db.query( new ClusterConfiguration( ) );
//      for ( ClusterConfiguration cc : componentList ) {
//        if ( cc.getMinVlan( ) == null ) cc.setMinVlan( 10 );
//        if ( cc.getMaxVlan( ) == null ) cc.setMaxVlan( 4095 );
//      }
//      db.commit( );
//      return componentList;
//    } catch ( Exception e ) {
//      db.rollback( );
//      LOG.error( e, e );
//      throw new EucalyptusCloudException( e );
//    }
//  }

//  public static List<StorageControllerConfiguration> getStorageControllerConfigurations( ) throws EucalyptusCloudException {
//    EntityWrapper<StorageControllerConfiguration> db = getEntityWrapper( );
//    try {
//      List<StorageControllerConfiguration> componentList = db.query( new StorageControllerConfiguration( ) );
//      db.commit( );
//      return componentList;
//    } catch ( Throwable e ) {
//      db.rollback( );
//      LOG.error( e, e );
//      throw new EucalyptusCloudException( e );
//    }
//  }

//  public static List<WalrusConfiguration> getWalrusConfigurations( ) throws EucalyptusCloudException {
//    EntityWrapper<WalrusConfiguration> db = getEntityWrapper( );
//    try {
//      List<WalrusConfiguration> componentList = db.query( new WalrusConfiguration( ) );
//      db.commit( );
//      return componentList;
//    } catch ( Throwable e ) {
//      db.rollback( );
//      LOG.error( e, e );
//      throw new EucalyptusCloudException( e );
//    }
//  }

//  public static List<VMwareBrokerConfiguration> getVMwareBrokerConfigurations( ) throws EucalyptusCloudException {
//    EntityWrapper<VMwareBrokerConfiguration> db = getEntityWrapper( );
//    try {
//      List<VMwareBrokerConfiguration> componentList = db.query( new VMwareBrokerConfiguration( ) );
//      db.commit( );
//      return componentList;
//    } catch ( Throwable e ) {
//      db.rollback( );
//      LOG.error( e, e );
//      throw new EucalyptusCloudException( e );
//    }
//  }

//  public static List<ArbitratorConfiguration> getArbitratorConfigurations( ) throws EucalyptusCloudException {
//    EntityWrapper<ArbitratorConfiguration> db = getEntityWrapper( );
//    try {
//      List<ArbitratorConfiguration> componentList = db.query( new ArbitratorConfiguration( ) );
//      db.commit( );
//      return componentList;
//    } catch ( Throwable e ) {
//      db.rollback( );
//      LOG.error( e, e );
//      throw new EucalyptusCloudException( e );
//    }
//  }

//  public static StorageControllerConfiguration getStorageControllerConfiguration( String scName ) throws EucalyptusCloudException, NoSuchElementException {
//    List<StorageControllerConfiguration> scs = getConfigurations( StorageControllerConfiguration.class );
//    for ( StorageControllerConfiguration sc : scs ) {
//      if ( sc.getName( ).equals( scName ) ) {
//        return sc;
//      }
//    }
//    throw new NoSuchElementException( StorageControllerConfiguration.class.getSimpleName( ) + " named " + scName );
//  }

//  public static WalrusConfiguration getWalrusConfiguration( String walrusName ) throws EucalyptusCloudException {
//    List<WalrusConfiguration> walri = getConfigurations( WalrusConfiguration.class );
//    for ( WalrusConfiguration w : walri ) {
//      if ( w.getName( ).equals( walrusName ) ) {
//        return w;
//      }
//    }
//    throw new NoSuchComponentException( WalrusConfiguration.class.getSimpleName( ) + " named " + walrusName );
//  }

//  public static ClusterConfiguration getClusterConfiguration( String clusterName ) throws EucalyptusCloudException {
//    List<ClusterConfiguration> clusters = getConfigurations( ClusterConfiguration.class );
//    for ( ClusterConfiguration c : clusters ) {
//      if ( c.getName( ).equals( clusterName ) ) {
//        return c;
//      }
//    }
//    throw new NoSuchComponentException( ClusterConfiguration.class.getSimpleName( ) + " named " + clusterName );
//  }
  
}
