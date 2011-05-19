package com.eucalyptus.webui.server;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.eucalyptus.system.BaseDirectory;

public class WebProperties {
  
  public static final String VERSION = "version";
  
  private static final Logger LOG = Logger.getLogger( WebProperties.class );
  
  private static String PROPERTIES_FILE =  BaseDirectory.CONF.toString() + File.separator + "eucalyptus-web.properties";
  private static final String EUCA_VERSION = "euca.version";
    
  public static HashMap<String, String> getProperties( ) {
    Properties props = new Properties( );
    FileInputStream input = null;
    try {
      input = new FileInputStream( PROPERTIES_FILE );
      props.load( input );
      props.setProperty( VERSION, "Eucalyptus " + System.getProperty( EUCA_VERSION ) );    
    } catch ( Exception e ) {
      LOG.error( "Failed to load web properties", e );
    } finally {
      if ( input != null ) {
        try {
          input.close( );
        } catch ( Exception e ) { }
      }
    }
    return new HashMap<String, String>( ( Map ) props );
  }
  
}
