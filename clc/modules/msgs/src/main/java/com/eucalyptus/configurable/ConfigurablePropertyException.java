package com.eucalyptus.configurable;

import com.eucalyptus.util.EucalyptusCloudException;

public class ConfigurablePropertyException extends EucalyptusCloudException {

  public ConfigurablePropertyException( ) {
    super( );
  }

  public ConfigurablePropertyException( String message, Throwable ex ) {
    super( message, ex );
  }

  public ConfigurablePropertyException( String message ) {
    super( message );
  }

  public ConfigurablePropertyException( Throwable ex ) {
    super( ex );
  }

}
