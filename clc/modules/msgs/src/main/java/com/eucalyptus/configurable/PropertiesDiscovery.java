package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import edu.emory.mathcs.backport.java.util.Arrays;

public class PropertiesDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( PropertiesDiscovery.class );

  public PropertiesDiscovery() {}
  
  @Override
  public Double getPriority( ) {
    return 0.4;
  }
  
  @Override
  public boolean processClass( Class c ) throws Throwable {
    if ( (c.getAnnotation( ConfigurableClass.class ) != null) )  {
      LOG.info( "-> Registrering configuration properties for entry: " + c.getName( ) );
      LOG.debug( "Checking fields: " + Arrays.asList( c.getDeclaredFields( ) ));
      for( Field  f : c.getDeclaredFields( ) ) {
        LOG.debug( "Checking field: " + f );
        try {
          ConfigurableProperty prop = PropertyDirectory.buildPropertyEntry( c, f );
          if( prop == null ) {
            continue;
          } else {
            LOG.info( "--> Adding property: " + prop.getQualifiedName( )  );
          }
        } catch ( Throwable e ) {
          LOG.debug( e, e );
        }
      }
      return true;
    } else {
      return false;
    }
  }
  
}
