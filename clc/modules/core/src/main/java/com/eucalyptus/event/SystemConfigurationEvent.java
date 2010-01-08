package com.eucalyptus.event;

import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;

public class SystemConfigurationEvent extends GenericEvent<SystemConfiguration> {

  public SystemConfigurationEvent( SystemConfiguration message ) {
    super( message );
  }
  
}
