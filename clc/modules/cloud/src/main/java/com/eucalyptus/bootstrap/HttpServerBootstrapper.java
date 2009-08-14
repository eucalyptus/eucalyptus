package com.eucalyptus.bootstrap;

import java.io.File;

import org.apache.log4j.Logger;
import org.mortbay.xml.XmlConfiguration;

import com.eucalyptus.util.BaseDirectory;

@Depends(resources={Resource.SystemCredentials})
public class HttpServerBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( HttpServerBootstrapper.class );
  private static org.mortbay.jetty.Server jettyServer;

  @Override
  public boolean load( ) throws Exception {
    jettyServer = new org.mortbay.jetty.Server();
    XmlConfiguration jettyConfig = new XmlConfiguration( new File( BaseDirectory.CONF.toString() + File.separator + "eucalyptus-jetty.xml" ).toURL() );
    jettyConfig.configure( jettyServer );
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    LOG.info("Starting admin interface.");
    jettyServer.start();
    return false;
  }

}

