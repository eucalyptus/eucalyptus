package com.eucalyptus.config;

import java.util.List;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.DiscoverableServiceBuilder;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.util.LogUtil;


@DiscoverableServiceBuilder( Storage.class )
@Handles( { RegisterStorageControllerType.class, DeregisterStorageControllerType.class, DescribeStorageControllersType.class, StorageControllerConfiguration.class, ModifyStorageControllerAttributeType.class } )
public class StorageControllerBuilder extends AbstractServiceBuilder<StorageControllerConfiguration> {
  private static Logger LOG = Logger.getLogger( StorageControllerBuilder.class );

  @Override
  public Component getComponent( ) {
    return Components.lookup( Storage.class );
  }
  
  @Override
  public StorageControllerConfiguration newInstance( ) {
    return new StorageControllerConfiguration( );
  }
  
  @Override
  public StorageControllerConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new StorageControllerConfiguration( partition, name, host, port );
  }
  
  @Override
  public Boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    if ( !Partitions.testPartitionCredentialsDirectory( name ) ) {
      throw new ServiceRegistrationException( "Storage Controller registration failed because the key directory cannot be created." );
    } else {
      return super.checkAdd( partition, name, host, port );
    }
  }

  @Override
  public List<StorageControllerConfiguration> list( ) throws ServiceRegistrationException {
    try {
      return ServiceConfigurations.list( Storage.class );
    } catch ( PersistenceException e ) {
      return super.list( );
    }
  }

  @Override
  public Boolean checkRemove( String partition, String name ) throws ServiceRegistrationException {
    return super.checkRemove( partition, name );
  }

  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( config.isVmLocal( ) ) {
      java.lang.System.setProperty( "euca.storage.name", config.getName( ) );
      LOG.info( LogUtil.subheader( "Setting euca.storage.name=" + config.getName( ) + " for: " + LogUtil.dumpObject( config ) ) );
    }
  }

  @Override
  public StorageControllerConfiguration add( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    StorageControllerConfiguration config = this.newInstance( partition, name, host, port );
    try {
      Partition part = Partitions.lookup( config );
      ServiceConfigurations.store( config );
      part.syncKeysToDisk( );
    } catch ( ServiceRegistrationException ex ) {
      Partitions.maybeRemove( config.getPartition( ) );
      throw ex;
    } catch ( Throwable ex ) {
      Partitions.maybeRemove( config.getPartition( ) );
      LOG.error( ex, ex );
      throw new ServiceRegistrationException( String.format( "Unexpected error caused cluster registration to fail for: partition=%s name=%s host=%s port=%d",
                                                             partition, name, host, port ), ex );
    }
    return config;
  }

  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
}
