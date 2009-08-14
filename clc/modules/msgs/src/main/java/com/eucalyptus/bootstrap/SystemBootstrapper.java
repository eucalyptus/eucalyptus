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
import com.eucalyptus.util.LogUtils;
import com.eucalyptus.util.ServiceJarFile;
import com.google.common.collect.Lists;

public class SystemBootstrapper extends Bootstrapper {
  private static Logger             LOG = Logger.getLogger( SystemBootstrapper.class );
  static {
    System.setProperty( "euca.db.host", "127.0.0.1" );
    System.setProperty( "euca.db.port", "9001" );
    System.setProperty( "euca.db.password", "" );
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
        LOG.info( "-> Invoking bootstrapper " + b.getClass( ).getSimpleName( ) + ".start()Z" );
        b.start( );
      }
      LOG.info( LogUtils.header( "Starting Eucalyptus." ) );
      context.start( );
      return true;
    } catch ( Exception e ) {
      LOG.error( e, e );
      return false;
    }
  }
  
  @Override
  public boolean load( ) throws Exception {
    LOG.info( LogUtils.header( "Initializing resource providers." ) );
    BootstrapFactory.initResourceProviders( );
    LOG.info( LogUtils.header( "Initializing configuration resources." ) );
    BootstrapFactory.initConfigurationResources( );
    LOG.info( LogUtils.header( "Initializing bootstrappers." ) );
    BootstrapFactory.initBootstrappers( );
    for( Resource r : Resource.values( ) ) {
      LOG.info( LogUtils.header( "Bootstrapping " + r ) );
      for( Bootstrapper b : BootstrapFactory.getBootstrappers( r ) ) {
        LOG.info( b.getClass( ) );
      }
    }

    //TODO: discovery persistence contexts
    //TODO: determine the role of this component 

    LOG.info( "-> Configuring..." );
    
//    context = new DefaultMuleContextFactory( ).createMuleContext( new SpringXmlConfigurationBuilder( configs.toArray( new ConfigResource[] {} ) ) );
//    for ( Bootstrapper b : this.bootstrappers ) {
//      LOG.info( "-> Found bootstrapper " + b.getClass( ).getSimpleName( ) + ".load()Z" );
//    }
//    for ( Bootstrapper b : this.bootstrappers ) {
//      LOG.info( "-> Invoking bootstrapper " + b.getClass( ).getSimpleName( ) + ".load()Z" );
//      try {
//        b.load( );
//      } catch ( Exception e ) {
//        LOG.info( e, e );
//      }
//    }
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

  public static native void hello( );
}
