package com.eucalyptus.config;

import com.eucalyptus.bootstrap.Component;
import edu.ucsb.eucalyptus.msgs.DeregisterStorageControllerType;
import edu.ucsb.eucalyptus.msgs.DescribeStorageControllersType;
import edu.ucsb.eucalyptus.msgs.RegisterComponentType;
import edu.ucsb.eucalyptus.msgs.RegisterStorageControllerType;

@Handles( { RegisterStorageControllerType.class, DeregisterStorageControllerType.class, DescribeStorageControllersType.class } )
public class StorageControllerBuilder extends AbstractServiceBuilder<StorageControllerConfiguration> {
  
  @Override
  public Boolean isLocal( ) {
    return Component.storage.isLocal( );
  }
  
  @Override
  public StorageControllerConfiguration newInstance( ) {
    return new StorageControllerConfiguration( );
  }
  
  @Override
  public StorageControllerConfiguration newInstance( String name, String host, Integer port, RegisterComponentType request ) {
    return new StorageControllerConfiguration( name, host, port );
  }

  /**
   * @see com.eucalyptus.config.AbstractServiceBuilder#check(java.lang.String, java.lang.String, java.lang.Integer)
   * @param name
   * @param host
   * @param port
   * @return
   * @throws ServiceRegistrationException
   */
  @Override
  public Boolean checkAdd( String name, String host, Integer port ) throws ServiceRegistrationException {
    try {
      Configuration.getClusterConfiguration( name );
    } catch ( Exception e1 ) {
      throw new ServiceRegistrationException( "Storage controllers may only be registered with a corresponding Cluster of the same name.  No cluster found with the name: "
                                              + name );
    }
    return super.checkAdd( name, host, port );
  }

  @Override
  public void fireStop( ComponentConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public Boolean checkRemove( String name ) throws ServiceRegistrationException {
    return true;
  }

  
}
