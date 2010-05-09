package com.eucalyptus.component;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.mule.config.ConfigResource;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.records.EventType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.eucalyptus.records.EventRecord;

public class Resource implements ComponentInformation {
  enum Keys {
    URL_PATTERN( "euca.url.pattern", "http://%s:%d/internal/%s" ), LOCAL_URL( "euca.url.local", "vm://EucalyptusRequestQueue" ), PORT( "euca.url.port", "8773" );
    private String key;
    private String defaultValue;
    
    private Keys( String key, String defaultValue ) {
      this.key = key;
      this.defaultValue = defaultValue;
    }
    
    public String getKey( ) {
      return this.key;
    }
    
    public String getDefaultValue( ) {
      return this.defaultValue;
    }
  }
  
  private static String              KEY_MODEL_PREFIX = "euca.model.";
  private static Logger              LOG              = Logger.getLogger( Resource.class );
  protected final Configuration      parent;
  protected final URI                origin;
  private final Map<String, String>  properties       = Maps.newHashMap( );
  private final List<ConfigResource> configurations   = Lists.newArrayList( );
  
  private Resource( Configuration parent ) {
    this.parent = parent;
    this.origin = URI.create( "file:///dev/null" );
  }
  
  @SuppressWarnings( "unchecked" )
  public Resource( Configuration parent, URI origin ) {
    this.parent = parent;
    if ( origin == null ) {
      this.origin = URI.create( "/dev/null" );
      for ( Keys prop : Keys.values( ) ) {
        this.properties.put( prop.getKey( ), prop.getDefaultValue( ) );
      }
    } else {
      this.origin = origin;
      Properties props = new Properties( );
      try {
        props.load( this.origin.toURL( ).openStream( ) );
        this.properties.putAll( Maps.fromProperties( props ) );
      } catch ( IOException e1 ) {
        throw BootstrapException.throwError( "Error loading component resources.  Failed to load: " + origin.toString( ) );
      }
      if ( !this.properties.containsKey( "name" ) ) {
        LOG.error( this.properties );
        throw BootstrapException.throwError( "Error loading component resources.  Failed to find 'name' property in: " + origin.toString( ) );
      }
      try {
        for ( String rscName : this.properties.keySet( ) ) {
          if ( rscName.startsWith( KEY_MODEL_PREFIX ) ) {
            String rscPath = this.properties.get( rscName );
            EventRecord.caller( Resource.class, EventType.BOOTSTRAP_RESOURCES_SERVICE_CONFIG, this.parent.getName( ), rscPath ).info( );
            this.configurations.add( new ConfigResource( rscPath ) );
          }
        }
      } catch ( IOException e ) {
        throw BootstrapException.throwFatal( "Processing " + this.origin + " caused an error: " + e.getMessage( ), e );
      }
      for ( Keys prop : Keys.values( ) ) {
        if ( !this.properties.containsKey( prop.getKey( ) ) ) {
          this.properties.put( prop.getKey( ), prop.getDefaultValue( ) );
          EventRecord.caller( Resource.class, EventType.BOOTSTRAP_RESOURCES_SERVICE_CONFIG, this.parent.getName( ), "default", prop.name( ), prop.getKey( ),
                              prop.getDefaultValue( ) ).info( );
        }
      }
    }
  }
  
  /**
   * Get a property from the module's configuration
   * 
   * @param key
   * @return
   */
  public String get( Keys key ) {
    return this.get( key.getKey( ) );
  }
  
  /**
   * Get a property from the module's configuration
   * 
   * @param key
   * @return
   */
  public String get( String key ) {
    return this.properties.get( key );
  }
  
  public List<ConfigResource> getConfigurations( ) {
    return this.configurations;
  }
  
  public URI getOrigin( ) {
    return this.origin;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.origin == null ) ? 0 : this.origin.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    Resource other = ( Resource ) obj;
    if ( this.origin == null ) {
      if ( other.origin != null ) return false;
    } else if ( !this.origin.equals( other.origin ) ) return false;
    return true;
  }
  
  public final Configuration getParent( ) {
    return this.parent;
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "Resource [" );
    if ( this.origin != null ) {
      builder.append( "origin=" ).append( this.origin );
    }
    builder.append( "]" );
    return builder.toString( );
  }

  @Override
  public String getName( ) {
    return this.parent.getName( );
  }
  
}
