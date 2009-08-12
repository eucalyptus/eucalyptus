package com.eucalyptus.bootstrap;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.mule.MuleServer;

import com.eucalyptus.util.BaseDirectory;
import com.eucalyptus.util.EucalyptusProperties;
import com.google.common.collect.Lists;


public class SystemBootstrapper implements Bootstrapper {
  private static Logger LOG = Logger.getLogger( SystemBootstrapper.class );
  private static SystemBootstrapper singleton;
  private SystemBootstrapper(){}
  static {
    LOG.info("Loaded Bootstrapper.");
  }
  
  public static Bootstrapper getInstance(){
    synchronized( SystemBootstrapper.class ) {
      if( singleton == null ) {
        singleton = new SystemBootstrapper( );
        LOG.info("Creating Bootstrapper instance.");
      } else {
        LOG.info("Returning Bootstrapper instance.");        
      }
    }
    return singleton;
  }
  
  @Override
  public List<String> getDependencies( ) {
    return null;
  }
  @Override
  public String getVersion( ) {
    return System.getProperty( "euca.version" );
  }
  @Override
  public boolean check() {
    LOG.info("Hello there in check.");
    return true;
  }
  @Override
  public boolean destroy() {
    LOG.info("Hello there in destroy.");
    return true;
  }
  @Override
  public boolean stop() {
    LOG.info("Hello there in stop.");
    return true;
  }
  @Override
  public boolean start() {
    LOG.info("Starting Eucalyptus.");
    return true;
  }
  @Override
  public boolean load() {
    LOG.info("Looking for Eucalyptus components in: " + BaseDirectory.LIB.toString( ) );
    MuleServer server = new MuleServer("eucalyptus-bootstrap.xml");
//    MuleServer server = new MuleServer( "eucalyptus-mule.xml" );
//    server.start( false, true );
    File libDir = new File( BaseDirectory.LIB.toString( ) );
    for( File f : libDir.listFiles( ) ) {
      if( f.getName( ).startsWith( EucalyptusProperties.NAME ) ) {
        LOG.info( "Found eucalyptus jar: " + f.getName( ) );
      }
    }
    //load credentials
    //bind DNS
    //bind http
    //bind webservices 
    //setup db
    return true;
  }
  private static native void shutdown(boolean reload);
  private static native void hello();
}
