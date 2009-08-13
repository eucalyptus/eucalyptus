package com.eucalyptus.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.mule.config.ConfigResource;

import com.eucalyptus.bootstrap.Bootstrapper;
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
    while ( jarList.hasMoreElements( ) ) {
      JarEntry j = jarList.nextElement( );
      if ( Bootstrapper.PROPERTIES.equals( j.getName( ) ) ) {
    	  LOG.info("Found properties: " + j.getName());
        try {
          InputStream in = this.getInputStream( j );
          props.load( in );
        } catch ( IOException e ) {
        }
      } else if ( j.getName( ).endsWith( ".class" ) ) {
        try {
          Class c = ServiceJarFile.this.getBootstrapper( j );
          this.bootstrappers.add( c );
        } catch ( Exception e ) {
          LOG.trace(e);
        }
      }
    }
    this.components = new HashMap<String, String>( ( Hashtable ) props );
  }

  @SuppressWarnings( "unchecked" )
  public List<Bootstrapper> getBootstrappers( Class excludeClass ) {
    List<Bootstrapper> ret = Lists.newArrayList( );
    for ( Class c : this.bootstrappers ) {
      if( c.equals( excludeClass ) ) continue;
      try {
        LOG.debug( "-> Calling <init>()V on bootstrapper: " + c.getCanonicalName( ) );
        try {
          ret.add( ( Bootstrapper ) c.newInstance( ) );
        } catch ( Exception e ) {
          LOG.debug( "-> Calling getInstance()L; on bootstrapper: " + c.getCanonicalName( ) );
          Method m = c.getDeclaredMethod( "getInstance", new Class[]{} );
          ret.add( ( Bootstrapper ) m.invoke( null, new Object[]{} ) );
        }
      } catch ( Exception e ) {
        LOG.warn( "Error in <init>()V and getInstance()L; in bootstrapper: " + c.getCanonicalName( ) );
        LOG.warn( e.getMessage( ) );
        LOG.debug( e,e );
      }
    }
    return ret;
  }

  public List<ConfigResource> getConfigResources( ) {
    List<ConfigResource> ret = Lists.newArrayList( );
    for ( String configKey : this.components.keySet( ) ) {
      try {
        LOG.debug( "-> Loading config resource: " + this.components.get( configKey ) );
        ConfigResource rsrc = new ConfigResource( configKey, this.getInputStream( this.getEntry( Bootstrapper.BASEDIR + this.components.get( configKey ) ) ) );
        ret.add( rsrc );
      } catch ( IOException e ) {
        LOG.debug( "Error loading config resource indicated by properties file: " + Bootstrapper.BASEDIR + this.components.get( configKey ) );
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
      if ( !Modifier.isStatic( factory.getModifiers( ) ) || !Modifier.isPublic( factory.getModifiers( ) ) ) { 
        throw new InstantiationException( candidate.getCanonicalName( ) + " does not declare public <init>()V or public static getInstance()L;" ); 
      }
    }
    LOG.info("Found bootstrapper: " + candidate.getName());
    return candidate;
  }
}
