package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.google.common.collect.ObjectArrays;
import edu.emory.mathcs.backport.java.util.Arrays;

public class PropertiesDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( PropertiesDiscovery.class );
  
  public PropertiesDiscovery( ) {}
  
  @Override
  public Double getPriority( ) {
    return 0.4;
  }
  
  @Override
  public boolean processClass( Class c ) throws Exception {
    if ( ( c.getAnnotation( ConfigurableClass.class ) != null ) ) {
      LOG.trace( "-> Registering configuration properties for entry: " + c.getName( ) );
      LOG.trace( "Checking fields: " + Arrays.asList( c.getDeclaredFields( ) ) );
      for ( Field f : ObjectArrays.concat( c.getFields( ), c.getDeclaredFields( ), Field.class ) ) {
        LOG.trace( "Checking field: " + f );
        try {
          ConfigurableProperty prop = PropertyDirectory.buildPropertyEntry( c, f );
          if ( prop == null ) {
            continue;
          } else {
            LOG.info( "--> Registered property: " + prop.getQualifiedName( ) );
          }
        } catch ( Exception e ) {
          LOG.debug( e, e );
        }
      }
      return true;
    } else {
      return false;
    }
  }
  
}
