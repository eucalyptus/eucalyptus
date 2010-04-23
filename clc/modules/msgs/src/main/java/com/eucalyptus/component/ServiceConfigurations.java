package com.eucalyptus.component;

import com.eucalyptus.entities.EntityWrapper;


public class ServiceConfigurations {
  
  private static ServiceConfigurationProvider singleton = new DatabaseServiceConfigurationProvider( );

  public static ServiceConfigurationProvider getInstance( ) {
    return singleton;
  }

  public static <T> EntityWrapper<T> getEntityWrapper( ) {
    return new EntityWrapper<T>( "eucalyptus_config" );
  }
  
}
