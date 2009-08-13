package com.eucalyptus.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.mule.api.MuleContext;
import org.mule.config.ConfigResource;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;

import com.eucalyptus.util.BaseDirectory;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.ServiceJarFile;
import com.google.common.collect.Lists;

public class SystemBootstrapper extends Bootstrapper {
  private static Logger             LOG = Logger.getLogger( SystemBootstrapper.class );
  static {
    LOG.info( "Loaded Bootstrapper." );
  }
  private static SystemBootstrapper singleton;

  public static Bootstrapper getInstance( ) {
    synchronized ( SystemBootstrapper.class ) {
      if ( singleton == null ) {
        singleton = new SystemBootstrapper( );
        LOG.info( "Creating Bootstrapper instance." );
      } else {
        LOG.info( "Returning Bootstrapper instance." );
      }
    }
    return singleton;
  }

  private MuleContext          context;
  private List<ConfigResource> configs       = Lists.newArrayList( );
  private List<Bootstrapper>   bootstrappers = Lists.newArrayList( );

  private SystemBootstrapper( ) {
  }

  @Override
  public boolean destroy( ) {
    return true;
  }

  @Override
  public boolean stop( ) throws Exception {
    this.context.stop( );
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    LOG.info( "Starting Eucalyptus." );
    try {
      for ( Bootstrapper b : this.bootstrappers ) {
        LOG.info( "-> Invoking bootsrapper " + b.getClass( ).getSimpleName( ) + ".start()Z" );
        b.start( );
      }
      context.start( );
      return true;
    } catch ( Exception e ) {
      LOG.error( e, e );
      return false;
    }
  }

  @SuppressWarnings( "deprecation" )
  @Override
  public boolean load( ) throws Exception {
    String bootstrapConfig = System.getProperty( Bootstrapper.BOOTSTRAP_CONFIG_PROPERTY );
    if( bootstrapConfig == null ) {
      LOG.fatal( "Bootstrap configuration property is undefined: " + Bootstrapper.BOOTSTRAP_CONFIG_PROPERTY );
      return false;
    }
    try {
      this.configs.add( new ConfigResource( bootstrapConfig ) );
    } catch ( Exception e ) {
      LOG.fatal( "Couldn't load bootstrap configuration file: " + bootstrapConfig, e );
      return false;
    }

    LOG.info( "Eucalyptus component discovery [" + BaseDirectory.LIB.toString( ) +"]" );
    File libDir = new File( BaseDirectory.LIB.toString( ) );
    for ( File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( EucalyptusProperties.NAME ) && f.getName( ).endsWith( ".jar" ) ) {
        try {
          LOG.info( "Found eucalyptus component jar: " + f.getName( ) );
          ServiceJarFile jar = new ServiceJarFile( f );
          this.bootstrappers.addAll( jar.getBootstrappers( this.getClass( ) ) );
          this.configs.addAll( jar.getConfigResources( ) );
        } catch ( IOException e ) {
          LOG.fatal( e,e );
          SystemBootstrapper.shutdown( false );
          return false;
        }
      }
    }
    //load/check credentials
    //bind DNS
    try {
      LOG.info( "-> Configuring..." );
      context = new DefaultMuleContextFactory( ).createMuleContext( new SpringXmlConfigurationBuilder( configs.toArray( new ConfigResource[] {} ) ) );
      for ( Bootstrapper b : this.bootstrappers ) {
        LOG.info( "-> Found bootsrapper " + b.getClass( ).getSimpleName( ) + ".load()Z" );
      }
      for ( Bootstrapper b : this.bootstrappers ) {
        LOG.info( "-> Invoking bootsrapper " + b.getClass( ).getSimpleName( ) + ".load()Z" );
        b.load( );
      }
    } catch ( Exception e ) {
      LOG.info( e, e );
    }
    return true;
  }

  @Override
  public String getVersion( ) {
    return System.getProperty( "euca.version" );
  }

  @Override
  public boolean check( ) {
    return true;
  }

  private static native void shutdown( boolean reload );

  private static native void hello( );
}
