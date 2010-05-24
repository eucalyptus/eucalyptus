package com.eucalyptus.component;

import java.lang.reflect.Modifier;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.Ats;

public class ServiceBuilderDiscovery extends ServiceJarDiscovery {
  
  @Override
  public Double getPriority( ) {
    return 0.2;
  }
  
  @Override
  public boolean processClass( Class candidate ) throws Throwable {
    if( ServiceBuilder.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) ) && !Modifier.isInterface( candidate.getModifiers( ) ) && Ats.from( candidate ).has( DiscoverableServiceBuilder.class ) ) {
      DiscoverableServiceBuilder at = Ats.from( candidate ).get( DiscoverableServiceBuilder.class );
      for( Component c : at.value( ) ) {
        Components.lookup( c ).setBuilder( ( ServiceBuilder ) candidate.newInstance( ) );
      }
      return true;
    } else {
      return false;
    }
  }
  
}
