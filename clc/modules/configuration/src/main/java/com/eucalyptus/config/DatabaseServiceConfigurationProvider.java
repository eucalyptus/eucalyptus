package com.eucalyptus.config;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

public class DatabaseServiceConfigurationProvider<T extends ComponentConfiguration> implements ComponentConfigurationProvider<T> {

  /**
   * TODO: DOCUMENT DatabaseServiceConfigurationProvider.java
   * @param type
   * @return
   * @throws ServiceRegistrationException
   */
  public List<T> list( T type ) throws ServiceRegistrationException {
    EntityWrapper<T> db = Configuration.getEntityWrapper( );
    List<T> existingHosts = null;
    try {
      T searchConfig = ( T ) type.getClass( ).newInstance( );
      existingHosts = db.query( searchConfig );
      db.rollback( );
    } catch ( Exception e ) {
      existingHosts = Lists.newArrayList( );
    }    
    return existingHosts;
  }
  
  /**
   * @see com.eucalyptus.config.ComponentConfigurationProvider#lookupByName(java.lang.String, java.lang.Class)
   * @param name
   * @param type
   * @return
   * @throws EucalyptusCloudException
   */
  public T lookupByName( String name, T type ) throws ServiceRegistrationException {
    EntityWrapper<T> db = Configuration.getEntityWrapper( );
    T existingName = null;
    try {
      T searchConfig = ( T ) type.getClass( ).newInstance( );
      searchConfig.setName( name );
      existingName = db.getUnique( searchConfig );
      db.rollback( );
      return existingName;
    } catch ( Exception e ) {
      throw new ServiceRegistrationException( "Component with name=" + name + " does not exist." );
    }
  }
  
  /**
   * @see com.eucalyptus.config.ComponentConfigurationProvider#lookupByHost(java.lang.String, java.lang.Class)
   * @param host
   * @param type
   * @return
   * @throws EucalyptusCloudException
   */
  public T lookupByHost( String host, T type ) throws ServiceRegistrationException {
    EntityWrapper<T> db = Configuration.getEntityWrapper( );
    T existingHost = null;
    try {
      T searchConfig = ( T ) type.getClass( ).newInstance( );
      searchConfig.setHostName( host );
      existingHost = db.getUnique( searchConfig );
      db.rollback( );
      return existingHost;
    } catch ( Exception e ) {
      throw new ServiceRegistrationException( "Component with host=" + host + " does not exist." );
    }
  }
  
  /**
   * @see com.eucalyptus.config.ComponentConfigurationProvider#lookup(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Class)
   * @param name
   * @param host
   * @param port
   * @param type
   * @return
   * @throws EucalyptusCloudException
   */
  public T lookup( String name, String host, Integer port, T type ) throws ServiceRegistrationException {
    EntityWrapper<T> db = Configuration.getEntityWrapper( );
    T existingName = null;
    try {
      T searchConfig = ( T ) type.getClass( ).newInstance( );
      searchConfig.setName( name );
      searchConfig.setPort( port );
      searchConfig.setHostName( host );
      existingName = db.getUnique( searchConfig );
      db.rollback( );
      return existingName;
    } catch ( Exception e ) {
      throw new ServiceRegistrationException( "Component with name=" + name + " does not exist." );
    }
  }
  
  private static Logger LOG = Logger.getLogger( DatabaseServiceConfigurationProvider.class );
  
  @Override
  public T store( T t ) throws ServiceRegistrationException {
    EntityWrapper<T> db = Configuration.getEntityWrapper( );
    try {
      db.add( t );
      t = db.getUnique( t );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new ServiceRegistrationException( e );
    }
    return t;
  }

  @Override
  public T remove( T t ) throws ServiceRegistrationException {
    EntityWrapper<T> db = Configuration.getEntityWrapper( );
    try {
      T exists = this.lookupByName( t.getName( ), t );
      db.delete( t );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
      LOG.error( e, e );
      throw new ServiceRegistrationException( e );
    }
    return t;
  }

}
