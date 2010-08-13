package com.eucalyptus.component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.MissingFormatArgumentException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.util.NetworkUtil;
import com.google.common.collect.Lists;

public class Configuration implements ComponentInformation {
  private static Logger LOG = Logger.getLogger( Configuration.class );
  private final Component          parent;
  private final Resource           resource;
  private final List<Bootstrapper> bootstrappers = Lists.newArrayList( );
  private final String             propertyKey;
  private URI                      uriLocal;
  private String                   uriPattern;
  private Integer                  port;

  Configuration( Component parent ) {
    this.parent = parent;
    this.propertyKey = "euca." + this.parent.getName( ) + ".host";
    //    this.resource = new Resource( this, URI.create( "/dev/null" ) );
    this.resource = null;
    this.port = Integer.parseInt( System.getProperty("euca.ws.port") );
    this.uriPattern = "http://%s:%d/internal/%s";
    this.uriLocal = URI.create( "vm://"+parent.getName( ).substring( 0, 1 ).toUpperCase( ) + parent.getName( ).substring( 1 )+"RequestQueue" );
  }
  
  Configuration( Component parent, URI u ) {
    this.parent = parent;
    this.propertyKey = "euca." + this.parent.getName( ) + ".host";
    this.resource = new Resource( this, u );
    this.port = Integer.parseInt( this.resource.get( Resource.Keys.PORT ) );
    this.uriPattern = this.resource.get( Resource.Keys.URL_PATTERN );
    this.uriLocal = URI.create( this.resource.get( Resource.Keys.LOCAL_URL ) );
  }
  
  @Override
  public String getName( ) {
    return this.parent.getName( );
  }
  
  public Resource getResource( ) {
    return this.resource;
  }
  
  public List<Bootstrapper> getBootstrappers( ) {
    return this.bootstrappers;
  }
  
  public String getPropertyKey( ) {
    return this.propertyKey;
  }
  
  public URI getLocalUri( ) {
    try {
      this.uriLocal.parseServerAuthority( );
    } catch ( URISyntaxException e ) {
      LOG.fatal( e, e );
      System.exit( -1 );
    }
    return this.uriLocal;
  }
  
  public void addBootstrapper( Bootstrapper bootstrap ) {
    this.bootstrappers.add( bootstrap );
  }
  
  public String getUriPattern( ) {
    return this.uriPattern;
  }
  
  public Integer getDefaultPort( ) {
    return this.port;
  }
  
  public URI makeUri( String host, Integer port ) {
    String uri;
    try {
      if ( NetworkUtil.testLocal( host ) ) {
        return this.getLocalUri( );
      } else {
        try {
          uri = String.format( this.getUriPattern( ), host, port );
        } catch ( MissingFormatArgumentException e ) {
          uri = String.format( this.getUriPattern( ), host, port , this.getLocalUri( ).getHost( ).replaceAll( "RequestQueue", "Internal" ) );
        }
        try {
          URI u = new URI( uri );
          u.parseServerAuthority( );
          return u;
        } catch ( URISyntaxException e ) {
          LOG.error( e, e );
          return URI.create( uri );
        }
      }
    } catch ( Exception e ) {
      return this.getLocalUri( );
    }
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.resource.getOrigin( ) == null ) ? 0 : this.resource.getOrigin( ).hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    Configuration other = ( Configuration ) obj;
    if ( this.resource.getOrigin( ) == null ) {
      if ( this.resource.getOrigin( ) != null ) return false;
    } else if ( !this.resource.getOrigin( ).equals( other.resource.getOrigin( ) ) ) return false;
    return true;
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "Configuration [" );
    if ( this.bootstrappers != null ) {
      builder.append( "bootstrappers=" ).append( this.bootstrappers ).append( ", " );
    }
    if ( this.parent.getName( ) != null ) {
      builder.append( "name=" ).append( this.parent.getName( ) ).append( ", " );
    }
    if ( this.propertyKey != null ) {
      builder.append( "propertyKey=" ).append( this.propertyKey ).append( ", " );
    }
    if ( this.resource != null ) {
      builder.append( "resource=" ).append( this.resource );
    }
    builder.append( "]" );
    return builder.toString( );
  }
  
  /**
   * @return the parent
   */
  protected final Component getParent( ) {
    return this.parent;
  }
  
}
