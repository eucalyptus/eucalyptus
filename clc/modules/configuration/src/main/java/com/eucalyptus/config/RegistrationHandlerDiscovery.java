package com.eucalyptus.config;

import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.system.Ats;

public class RegistrationHandlerDiscovery extends ServiceJarDiscovery {
  
  @Override
  public Double getPriority( ) {
    return 1.25;
  }
  
  @Override
  public boolean processClass( Class candidate ) throws Throwable {
    if( Ats.from( candidate ).has( Handles.class ) ) {
      for( Class c : Ats.from( candidate ).get( Handles.class ).value( ) ) {
        Configuration.addBuilder( c, ( ServiceBuilder ) candidate.newInstance( ) );
      }
      return true;
    } else {
      return false;
    }
  }
  
}
