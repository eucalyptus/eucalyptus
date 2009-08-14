package com.eucalyptus.bootstrap;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.log4j.Logger;
import org.mortbay.xml.XmlConfiguration;

import com.eucalyptus.util.BaseDirectory;

@Depends(resources={Resource.SystemCredentials})
public class HttpServerBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( HttpServerBootstrapper.class );
  private static org.mortbay.jetty.Server jettyServer;

  @Override
  public boolean load(Resource current, List<Resource> dependencies ) throws Exception {
    jettyServer = new org.mortbay.jetty.Server();
    URL defaultConfig = ClassLoader.getSystemResource( "eucalyptus-jetty.xml" );
    XmlConfiguration jettyConfig = new XmlConfiguration( defaultConfig );
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

