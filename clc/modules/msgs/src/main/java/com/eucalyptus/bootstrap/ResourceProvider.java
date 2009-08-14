package com.eucalyptus.bootstrap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mule.config.ConfigResource;

import net.sf.json.JSONObject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ResourceProvider {
  private static Logger LOG = Logger.getLogger( ResourceProvider.class );
  private final Resource            resource;
  private final URL                 origin;
  private final String              name;
  private final Map<String, String> properties;
  private final List<ConfigResource> configs;

  @SuppressWarnings( "unchecked" )
  public ResourceProvider( Resource resource, Properties props, URL origin ) {
    this.resource = resource;
    this.properties = Maps.newHashMap( ( Hashtable ) props );
    this.name = this.properties.remove( "name" );
    this.origin = origin;
    this.configs = Lists.newArrayList( );
  }
  
  public List<ConfigResource> initConfigurationResources() throws IOException {
    for(String rscName : this.properties.keySet( )) {
      try {
        this.configs.add( new ConfigResource( this.properties.get( rscName ) ) );
      } catch ( IOException e ) {
        LOG.error( String.format("Processing %s caused an error %s: %s", this.origin, e.getClass().getSimpleName( ), e.getMessage( )), e );
        throw e;
      } 
    }
    return this.configs;
  }
  
  public Resource getResource( ) {
    return resource;
  }

  public String getName( ) {
    return name;
  }

  public URL getOrigin( ) {
    return origin;
  }

}
