package com.eucalyptus.bootstrap;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

public class BootstrapFactory {
  private static Logger LOG = Logger.getLogger( BootstrapFactory.class );
  public static void findAll() {
    for( ResourceProvider r : ResourceProvider.values( ) ) {
      r.getProviders( );
    }
  }
  public enum ResourceProvider {
    SystemCredentials(),
    Database(),
    ClusterCredentials(),
    UserCredentials(),
    CloudService(),
    ;
    private String resourceName;
    private static Logger LOG = Logger.getLogger( BootstrapFactory.ResourceProvider.class );
    private ResourceProvider() {
      this.resourceName = String.format( "com.eucalyptus.%sProvider", this.name( ) );
    }
    
    @SuppressWarnings( "static-access" )
    public List<String> getProviders() {
      List<String> providers = Lists.newArrayList( );
      try {
        Enumeration<URL> p1 = ClassLoader.getSystemClassLoader( ).getResources( this.name( ) );
        URL u = null;
        while(p1.hasMoreElements( )) {
          u = p1.nextElement( );
          LOG.info( u );
        }
        Enumeration<URL> p2 = BootstrapFactory.class.getClassLoader( ).getSystemResources( this.name( ) );
        while(p1.hasMoreElements( )) {
          u = p1.nextElement( );
          LOG.info( u );
        }
      } catch ( IOException e ) {
        e.printStackTrace();
      }
      return providers;
    }
    
  }

}
