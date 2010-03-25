package com.eucalyptus.bootstrap;

import org.apache.log4j.Logger;

public class ConfigurableDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( ConfigurableDiscovery.class );

  
  public ConfigurableDiscovery( ) {}

  @Override
  public Double getPriority( ) {
    return 1.1;
  }
  
  @Override
  public boolean processsClass( Class candidate ) throws Throwable {
    if ( candidate.isAnnotationPresent( Configurable.class ) ) {
    	Configurable configurable = ( Configurable ) candidate.getAnnotation( Configurable.class );
      if ( configurable.component( ).isEnabled( ) ) {
        ConfigurableManagement.getInstance( ).add( candidate );
        LOG.info( "---> Loading configurable for entry: " + candidate.getName( ) );
        return true;
      }
    }
    return false;
  }

}
