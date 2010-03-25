package com.eucalyptus.bootstrap;

import org.apache.log4j.Logger;

public class DeferredInitializerDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( DeferredInitializerDiscovery.class );

  
  public DeferredInitializerDiscovery( ) {}

  @Override
  public Double getPriority( ) {
    return 1.0;
  }
  
  @Override
  public boolean processsClass( Class candidate ) throws Throwable {
    if ( candidate.isAnnotationPresent( NeedsDeferredInitialization.class ) ) {
      NeedsDeferredInitialization needsDeferredInit = ( NeedsDeferredInitialization ) candidate.getAnnotation( NeedsDeferredInitialization.class );
      if ( needsDeferredInit.component( ).isEnabled( ) ) {
        DeferredInitializer.getInstance( ).add( candidate );
        LOG.info( "---> Loading deferred initializer for entry: " + candidate.getName( ) );
        return true;
      }
    }
    return false;
  }

}
