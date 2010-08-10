package com.eucalyptus.component;

import org.apache.log4j.Logger;

public abstract class DispatcherFactory {
  private static Logger            LOG = Logger.getLogger( DispatcherFactory.class );
  private static DispatcherFactory factory;
  
  public static void setFactory( DispatcherFactory factory ) {
    synchronized ( DispatcherFactory.class ) {
      LOG.info( "Setting the dispatcher factory to: " + factory.getClass( ).getCanonicalName( ) );
      DispatcherFactory.factory = factory;
    }
  }
  
  public static Dispatcher build( Component parent, Service service ) {
    return factory.buildChild( parent, service );
  }

  public abstract Dispatcher buildChild( Component parent, Service service );

  public static void remove( Service service ) {
    factory.removeChild( service );
  }
  
  public abstract void removeChild( Service service );
  
}
