package com.eucalyptus.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

public enum Resource {
  Bootstrap( ),
  PrivilegedContext( ),
  SystemCredentials( ),
  Database( ),
  ClusterCredentials( ),
  UserCredentials( ),
  CloudService( ),
  SpringService( ),
  Nothing( ), ;
  private String        resourceName;
  private boolean initialized = false; //TODO: state needed for @Depends
  private boolean started = false;
  private static Logger LOG = Logger.getLogger( Resource.class );

  private Resource( ) {
    this.resourceName = String.format( "com.eucalyptus.%sProvider", this.name( ) );
  }

  public List<ResourceProvider> getProviders( ) {
    List<ResourceProvider> providers = Lists.newArrayList( );
    Enumeration<URL> p1;
    try {
      p1 = Thread.currentThread( ).getContextClassLoader( ).getResources( this.resourceName );
      try {
        URL u = null;
        while ( p1.hasMoreElements( ) ) {
          u = p1.nextElement( );
          LOG.debug( "Found resource provider: " + u );
          Properties props = new Properties( );
          props.load( u.openStream( ) );
          providers.add( new ResourceProvider( this, props, u ) );
        }
      } catch ( IOException e ) {
        LOG.error( e, e );
      }
    } catch ( IOException e1 ) {
      LOG.error( e1, e1 );
    }
    return providers;
  }
  
  public boolean providedBy( Class clazz ) {
    for ( Annotation a : clazz.getAnnotations( ) ) {
      if ( a instanceof Provides && this.equals(((Provides)a).resource( ) )) {
        return true;
      }
    }
    return false;
  }
}
