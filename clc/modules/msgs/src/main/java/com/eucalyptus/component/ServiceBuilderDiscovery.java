package com.eucalyptus.component;

import java.lang.reflect.Modifier;
import com.eucalyptus.bootstrap.Handles;
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
      /** GRZE: this implies that service builder is a singleton **/
      ServiceBuilder b = ( ServiceBuilder ) candidate.newInstance( );
      DiscoverableServiceBuilder at = Ats.from( candidate ).get( DiscoverableServiceBuilder.class );
      for( Class c : at.value( ) ) {
        ComponentId compId = (ComponentId) c.newInstance( );
        ServiceBuilderRegistry.addBuilder( compId, b );
      }
      if( Ats.from( candidate ).has( Handles.class ) ) {
        for( Class c : Ats.from( candidate ).get( Handles.class ).value( ) ) {
          ServiceBuilderRegistry.addBuilder( c, b );
        }
      }
      return true;
    } else {
      return false;
    }
  }
  
}
