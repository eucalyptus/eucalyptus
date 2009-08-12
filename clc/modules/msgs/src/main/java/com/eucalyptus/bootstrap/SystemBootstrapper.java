package com.eucalyptus.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.Registry;
import org.mule.config.ConfigResource;
import org.mule.config.spring.MuleApplicationContext;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;

import com.eucalyptus.util.BaseDirectory;
import com.eucalyptus.util.EucalyptusProperties;
import com.google.common.collect.Lists;

public class SystemBootstrapper implements Bootstrapper {
  private static Logger             LOG = Logger.getLogger( SystemBootstrapper.class );
  private static SystemBootstrapper singleton;
  private static MuleContext        context;

  private SystemBootstrapper( ) {
  }

  static {
    LOG.info( "Loaded Bootstrapper." );
  }
  private static MuleServer server;

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

  @Override
  public String getVersion( ) {
    return System.getProperty( "euca.version" );
  }

  @Override
  public boolean check( ) {
    LOG.info( "Hello there in check." );
    return true;
  }

  @Override
  public boolean destroy( ) {
    LOG.info( "Hello there in destroy." );
    return true;
  }

  @Override
  public boolean stop( ) {
    LOG.info( "Hello there in stop." );
    return true;
  }

  @Override
  public boolean start( ) {
    LOG.info( "Starting Eucalyptus." );
    return true;
  }

  @Override
  public boolean load( ) {
    LOG.info( "Looking for Eucalyptus components in: " + BaseDirectory.LIB.toString( ) );
//    server = new MuleServer("eucalyptus-bootstrap.xml");
//    server.start( true, true );
    List<ConfigResource> confs = Lists.newArrayList( );
    File libDir = new File( BaseDirectory.LIB.toString( ) );
    for ( File f : libDir.listFiles( ) ) {
      if ( f.getName( ).startsWith( EucalyptusProperties.NAME ) ) {
        try {
          JarFile jar = new JarFile( f );
          JarEntry entry = jar.getJarEntry( Bootstrapper.PROPERTIES );
          InputStream in = jar.getInputStream( entry );
          LOG.info( "Found eucalyptus jar: " + f.getName( ) );
          Properties props = new Properties( );
          props.load( in );
          LOG.info( "-> Loaded properties..." );
          props.list( System.out );
          String servicesEntryPath = props.getProperty( Bootstrapper.SERVICES_PROPERTY );//TODO: null check hi.
          JarEntry servicesEntry = jar.getJarEntry( Bootstrapper.BASEDIR + servicesEntryPath );
          ConfigResource rsc = new ConfigResource( servicesEntryPath, jar.getInputStream( servicesEntry ) );
          confs.add( rsc );
//          new MuleApplicationContext(server.getMuleContext( ), server.getMuleContext( ).getRegistry( ), conf);

          //          String model = props.getProperty( Bootstrapper.MODEL_PROPERTY );
//          JarEntry modelEntry = jar.getJarEntry( Bootstrapper.BASEDIR + model );
          //TODO: finish up here.
        } catch ( Exception e ) {
          LOG.info( e, e );
        }
      }
    }
    //load credentials
    //bind DNS
    //bind http
    //bind webservices 
    //setup db
    try {
      LOG.info( "-> Configuring..." );
      confs.add( new ConfigResource( "eucalyptus-bootstrap.xml" ) );
      context = new DefaultMuleContextFactory( ).createMuleContext( new SpringXmlConfigurationBuilder( confs.toArray( new ConfigResource[]{} ) ) );
      context.start( );
    } catch ( Exception e ) {
      LOG.info( e, e );
    }
    return true;
  }

  private static native void shutdown( boolean reload );

  private static native void hello( );
}
