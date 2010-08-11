package com.eucalyptus.component;

import java.lang.reflect.Modifier;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;

public class DispatcherFactoryDiscovery extends ServiceJarDiscovery {
  
  @Override
  public Double getPriority( ) {
    return 0.31;
  }
  
  @Override
  public boolean processClass( Class candidate ) throws Throwable {
    if( DispatcherFactory.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) ) && !Modifier.isInterface( candidate.getModifiers( ) ) ) {
      DispatcherFactory.setFactory( ( DispatcherFactory ) candidate.newInstance( ) );
      return true;
    } else {
      return false;
    }
  }
  
}
