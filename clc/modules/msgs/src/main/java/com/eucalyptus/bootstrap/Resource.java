package com.eucalyptus.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mule.config.ConfigResource;

import com.google.common.collect.Lists;

public enum Resource {
  PrivilegedContext( ),
  SystemCredentials( ),
  RemoteConfiguration( ),
  Database( ),
  PersistenceContext( ),
  ClusterCredentials( ),
  UserCredentials( ),
  CloudService( ),
  SpringService( ),
  Nothing( );
  private String                 resourceName;
  private static Logger          LOG = Logger.getLogger( Resource.class );
  private List<Bootstrapper>     bootstrappers;
  private List<ResourceProvider> providers;

  private Resource( ) {
    this.resourceName = String.format( "com.eucalyptus.%sProvider", this.name( ) );
    this.bootstrappers = Lists.newArrayList( );
  }

  public List<ResourceProvider> getProviders( ) {
    synchronized ( this ) {
      if ( this.providers == null ) {
        this.providers = Lists.newArrayList( );
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
              ResourceProvider p = new ResourceProvider( this, props, u );
              providers.add( p );
            }
          } catch ( IOException e ) {
            LOG.error( e, e );
          }
        } catch ( IOException e1 ) {
          LOG.error( e1, e1 );
        }
      }
      return providers;
    }
  }

  public boolean providedBy( Class clazz ) {
    for ( Annotation a : clazz.getAnnotations( ) ) {
      if ( a instanceof Provides && this.equals( ( ( Provides ) a ).resource( ) ) ) { return true; }
    }
    return false;
  }

  public boolean satisfiesDependency( Class clazz ) {
    Depends d = ( Depends ) clazz.getAnnotation( Depends.class );//TODO: lame AST parser complains about this and requires cast...
    if( d != null && Lists.newArrayList( d.resources( ) ).contains( this ) ) {
      return true;
    }
    return false;
  }

  public boolean add( Bootstrapper b ) {
    return this.bootstrappers.add( b );
  }

  public List<Bootstrapper> getBootstrappers( ) {
    return this.bootstrappers;
  }

  public boolean add( ResourceProvider p ) {
    return this.providers.add( p );
  }

  public List<ResourceProvider> initProviders( ) throws IOException {
    for ( ResourceProvider p : this.providers ) {
      p.initConfigurationResources( );
    }
    return this.providers;
  }

}
