package com.eucalyptus.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.SystemBootstrapper;
import com.google.common.collect.Lists;

public class ServiceJarFile extends JarFile {
  private static Logger       LOG = Logger.getLogger( ServiceJarFile.class );
  private URLClassLoader      classLoader;
  private List<Class>         bootstrappers;
  private Map<String, String> components;

  @SuppressWarnings( { "deprecation", "unchecked" } )
  public ServiceJarFile( File f ) throws IOException {
    super( f );
    Properties props = new Properties( );
    this.bootstrappers = Lists.newArrayList( );
    Enumeration<JarEntry> jarList = this.entries( );
    this.classLoader = URLClassLoader.newInstance( new URL[] { f.getAbsoluteFile( ).toURL( ) } );
    LOG.debug( "-> Trying to load component info from " + f.getAbsolutePath( ) );
    while ( jarList.hasMoreElements( ) ) {
      JarEntry j = jarList.nextElement( );
      LOG.debug( "--> Handling entry: " + j.getName( ) );
      if ( j.getName( ).endsWith( ".class" ) ) {
        try {
          Class c = ServiceJarFile.this.getBootstrapper( j );
          LOG.debug( "---> Loading bootstrapper from entry: " + j.getName( ) );
          this.bootstrappers.add( c );
        } catch ( Exception e ) {
          LOG.debug( e, e );
        }
      }
    }
  }

  @SuppressWarnings( "unchecked" )
  public List<Bootstrapper> getBootstrappers( ) {
    List<Bootstrapper> ret = Lists.newArrayList( );
    for ( Class c : this.bootstrappers ) {
      if ( c.equals( SystemBootstrapper.class ) ) continue;
      try {
        LOG.debug( "-> Calling <init>()V on bootstrapper: " + c.getCanonicalName( ) );
        try {
          ret.add( ( Bootstrapper ) c.newInstance( ) );
        } catch ( Exception e ) {
          LOG.debug( "-> Calling getInstance()L; on bootstrapper: " + c.getCanonicalName( ) );
          Method m = c.getDeclaredMethod( "getInstance", new Class[] {} );
          ret.add( ( Bootstrapper ) m.invoke( null, new Object[] {} ) );
        }
      } catch ( Exception e ) {
        LOG.warn( "Error in <init>()V and getInstance()L; in bootstrapper: " + c.getCanonicalName( ) );
        LOG.warn( e.getMessage( ) );
        LOG.debug( e, e );
      }
    }
    return ret;
  }

  @SuppressWarnings( "unchecked" )
  private Class getBootstrapper( JarEntry j ) throws Exception {
    String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( ".class", "" );
    Class candidate = this.classLoader.loadClass( classGuess );
    if ( Bootstrapper.class.equals( candidate ) ) throw new InstantiationException( Bootstrapper.class + " is abstract." );
    if ( !Bootstrapper.class.isAssignableFrom( candidate ) ) throw new InstantiationException( candidate + " does not conform to " + Bootstrapper.class );
    if ( !Modifier.isPublic( candidate.getDeclaredConstructor( new Class[] {} ).getModifiers( ) ) ) {
      Method factory = candidate.getDeclaredMethod( "getInstance", new Class[] {} );
      if ( !Modifier.isStatic( factory.getModifiers( ) ) || !Modifier.isPublic( factory.getModifiers( ) ) ) { throw new InstantiationException( candidate.getCanonicalName( ) + " does not declare public <init>()V or public static getInstance()L;" ); }
    }
    LOG.info("Found bootstrapper: " + candidate.getName());
    return candidate;
  }
}
