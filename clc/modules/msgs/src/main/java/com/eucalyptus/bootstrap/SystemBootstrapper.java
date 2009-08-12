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
        LOG.info( "-> Invoking bootsrapper " + b.getClass( ).getSimpleName( ) + ".start()" );
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
    LOG.info( "Looking for Eucalyptus components in: " + BaseDirectory.LIB.toString( ) );

    File libDir = new File( BaseDirectory.LIB.toString( ) );
    for ( File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( EucalyptusProperties.NAME ) ) {
        JarFile jar = new JarFile( f );

        LOG.info( "Found eucalyptus component jar: " + f.getName( ) );
        URLClassLoader classLoader = URLClassLoader.newInstance( new URL[] { f.getAbsoluteFile( ).toURL( ) } );
        Enumeration<JarEntry> jarList = jar.entries( );
        while ( jarList.hasMoreElements( ) ) {
          JarEntry j = jarList.nextElement( );
          if ( j.getName( ).endsWith( ".class" ) ) {
            String classGuess = j.getName( ).replaceAll( "/", "." ).replaceAll( ".class", "" );
            try {
              Class c = classLoader.loadClass( classGuess );
              if ( Bootstrapper.class.isAssignableFrom( c ) && !Bootstrapper.class.equals( c ) ) {
                try {
                  Bootstrapper b = ( Bootstrapper ) c.newInstance( );
                  this.bootstrappers.add( b );
                  LOG.info( "-> Registered bootsrapper instance: " + c.getSimpleName( ) );
                } catch ( Exception e ) {
                  LOG.info( "-> Failed to create bootstrapper instance: " + c.getSimpleName( ), e );
                }
              }
            } catch ( Exception e ) {
              LOG.error("Error occurred while trying to process class: " + classGuess );
            }
          }
        }
        
        LOG.info( "-> Loaded properties..." );
        JarEntry entry = jar.getJarEntry( Bootstrapper.PROPERTIES );
        InputStream in = jar.getInputStream( entry );
        List<ConfigResource> conf = Lists.newArrayList( );
        Properties props = new Properties( );
        props.load( in );
        props.list( System.out );
        String servicesEntryPath = null;
        try {
          servicesEntryPath = props.getProperty( Bootstrapper.SERVICES_PROPERTY );//TODO: null check hi
          JarEntry servicesEntry = jar.getJarEntry( Bootstrapper.BASEDIR + servicesEntryPath );
          ConfigResource servicesResource = new ConfigResource( servicesEntryPath, jar.getInputStream( servicesEntry ) );
          conf.add( servicesResource );
          LOG.info( "-> Added configuration " + servicesEntryPath + "..." );
        } catch ( Exception e ) {
          if ( servicesEntryPath != null ) {
            LOG.info( "-> Skipping " + servicesEntryPath + "..." );
          }
        }
        String modelEntryPath = null;
        try {
          modelEntryPath = props.getProperty( Bootstrapper.MODEL_PROPERTY );
          JarEntry modelEntry = jar.getJarEntry( Bootstrapper.BASEDIR + modelEntryPath );
          ConfigResource modelResource = new ConfigResource( modelEntryPath, jar.getInputStream( modelEntry ) );
          conf.add( modelResource );
          LOG.info( "-> Added configuration " + modelEntryPath + "..." );
        } catch ( Exception e ) {
          if ( modelEntryPath != null ) {
            LOG.info( "-> Skipping " + modelEntryPath + "..." );
          }
        }
      }
    }
    //load/check credentials
    //bind DNS
    try {
      LOG.info( "-> Configuring..." );
      configs.add( new ConfigResource( "eucalyptus-bootstrap.xml" ) );
      context = new DefaultMuleContextFactory( ).createMuleContext( new SpringXmlConfigurationBuilder( configs.toArray( new ConfigResource[] {} ) ) );
      for ( Bootstrapper b : this.bootstrappers ) {
        LOG.info( "-> Invoking bootsrapper " + b.getClass( ).getSimpleName( ) + ".load()" );
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
