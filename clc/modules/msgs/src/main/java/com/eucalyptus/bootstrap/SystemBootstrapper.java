package com.eucalyptus.bootstrap;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mule.api.MuleContext;

import com.eucalyptus.util.LogUtils;
import com.google.common.collect.Lists;

import edu.emory.mathcs.backport.java.util.Arrays;

public class SystemBootstrapper {
  private static Logger             LOG = Logger.getLogger( SystemBootstrapper.class );
  static {
    System.setProperty( "euca.db.host", "127.0.0.1" );
    System.setProperty( "euca.db.port", "9001" );
    System.setProperty( "euca.db.password", "" );
  }
  private static SystemBootstrapper singleton;

  public static SystemBootstrapper getInstance( ) {
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

  private SystemBootstrapper( ) {}

  public boolean destroy( ) {
    return true;
  }

  public boolean stop( ) throws Exception {
    this.context.stop( );
    return true;
  }

  public boolean init() throws Exception {
    try {
      LOG.info( LogUtils.header( "Initializing resource providers." ) );
      BootstrapFactory.initResourceProviders( );
      LOG.info( LogUtils.header( "Initializing configuration resources." ) );
      BootstrapFactory.initConfigurationResources( );
      LOG.info( LogUtils.header( "Initializing bootstrappers." ) );
      BootstrapFactory.initBootstrappers( );
      return true;
    } catch ( Exception e ) {
      LOG.fatal( e,e );
      return false;
    }
  }
  
  /*
   * bind privileged ports
   * generate/waitfor credentials
   * start database server
   * configure db/load bootstrap stack & wait for dbconfig
   * TODO: discovery persistence contexts
   * TODO: determine the role of this component 
   * TODO: depends callbacks
   * TODO: remote config
   * TODO: bootstrap bindings
   */
  public boolean load( ) throws Exception {
    for( Resource r : Resource.values( ) ) {
      if( r.getBootstrappers( ).isEmpty( ) ) {
        LOG.info( "Skipping " + r + "... nothing to do.");
      } else { 
        LOG.info( LogUtils.header( "Loading " + r ) );
      }
      for( Bootstrapper b : r.getBootstrappers( ) ) {
        try {
          LOG.info( "-> load: " + b.getClass( ) );
          Depends deps = b.getClass( ).getAnnotation( Depends.class );
          Resource[] depResources = deps!=null?deps.resources( ):new Resource[]{Resource.Nothing};
          List<Resource> depList =Arrays.asList( depResources );
          boolean result = b.load( r, depList );          
        } catch ( Exception e ) {
          LOG.error( b.getClass( ).getSimpleName( ) + " threw an error in load( ): " + e.getMessage( ), e);
          return false;
        }
      }
    }
    return true;
  }

  public boolean start( ) throws Exception {
    for( Resource r : Resource.values( ) ) {
      if( r.getBootstrappers( ).isEmpty( ) ) {
        LOG.info( "Skipping " + r + "... nothing to do.");
      } else { 
        LOG.info( LogUtils.header( "Starting " + r ) );
      }
      for( Bootstrapper b : r.getBootstrappers( ) ) {
        try {
          LOG.info( "-> start: " + b.getClass( ) );
          boolean result = b.start( );          
        } catch ( Exception e ) {
          LOG.error( b.getClass( ).getSimpleName( ) + " threw an error in start( ): " + e.getMessage( ), e);
          return false;
        }
      }
    }
    return true;
  }

  public String getVersion( ) {
    return System.getProperty( "euca.version" );
  }

  public boolean check( ) {
    return true;
  }

  private static native void shutdown( boolean reload );

  public static native void hello( );
}
