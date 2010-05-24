package com.eucalyptus.event;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;

public class EventListenerDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( EventListenerDiscovery.class );
  
  public EventListenerDiscovery( ) {}
  
  @Override
  public Double getPriority( ) {
    return 0.5;
  }
  
  @Override
  public boolean processClass( Class candidate ) throws Throwable {
    Class listener = this.getEventListener( candidate );
    return true;
  }
  
  @SuppressWarnings( "unchecked" )
  private Class getEventListener( Class candidate ) throws Exception {
    if ( !EventListener.class.isAssignableFrom( candidate ) ) {
      throw new InstantiationException( candidate + " does not conform to " + EventListener.class );
    }
    LOG.trace( "Candidate event listener: " + candidate.getName( ) );
    Method factory;
    factory = candidate.getDeclaredMethod( "register", new Class[] {} );
    if ( !Modifier.isStatic( factory.getModifiers( ) ) || !Modifier.isPublic( factory.getModifiers( ) ) ) {
      throw new InstantiationException( candidate.getCanonicalName( ) + " does not declare public static register()V" );
    }
    LOG.trace( "-> Registered event listener: " + candidate.getName( ) );
    factory.invoke( null, new Object[] {} );
    return candidate;
  }
  
}
