package com.eucalyptus.component;

import com.eucalyptus.util.EucalyptusCloudException;

public class ServiceRegistrationException extends EucalyptusCloudException {

  public ServiceRegistrationException( ) {
    super( );
  }

  public ServiceRegistrationException( String message, Throwable cause ) {
    super( message, cause );
  }

  public ServiceRegistrationException( String message ) {
    super( message );
  }

  public ServiceRegistrationException( Throwable cause ) {
    super( cause );
  }

}
