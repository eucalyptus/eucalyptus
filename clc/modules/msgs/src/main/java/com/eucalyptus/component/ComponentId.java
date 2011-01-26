package com.eucalyptus.component;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.MissingFormatArgumentException;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.mule.config.ConfigResource;
import com.eucalyptus.auth.principal.credential.HmacPrincipal;
import com.eucalyptus.auth.principal.credential.X509Principal;
import com.eucalyptus.component.auth.SystemCredentialProvider;
import com.eucalyptus.util.HasName;
import com.google.common.collect.Lists;

public abstract class ComponentId implements ComponentInformation, HasName<ComponentId>, X509Principal, HmacPrincipal {
  private static Logger LOG = Logger.getLogger( ComponentId.class );
  
  private final String name;
  private final Integer port;
  private String uriPattern;
  private String uriLocal;
  
  protected ComponentId( String name ) {
    this.name = name;
    this.port = 8773;
    this.uriPattern = "http://%s:%d/internal/%s";
    this.uriLocal = String.format( "vm://%sInternal", this.getClass( ).getSimpleName( ) );
  }

  protected ComponentId( ) {
    this.name = this.getClass( ).getSimpleName( ).toLowerCase( );
    this.port = 8773;
    this.uriPattern = "http://%s:%d/internal/%s";
    this.uriLocal = String.format( "vm://%sInternal", this.getClass( ).getSimpleName( ) );
  }

  public String name( ) {
    return this.name;
  }
  
  @Override
  public String getName( ) {
    return this.name;
  }
  
  public abstract Boolean isCloudLocal( );
  public abstract Boolean hasDispatcher( );
  public abstract Boolean isAlwaysLocal( );

  
  /**
   * Get the HTTP service path
   */
  public URI makeRemoteUri( String hostName, Integer port ) {
    String uri;
    try {
      uri = String.format( this.getUriPattern( ), hostName, port );
    } catch ( MissingFormatArgumentException e ) {
      uri = String.format( this.getUriPattern( ), hostName, port, this.getLocalEndpointName( ).replaceAll( "RequestQueue", "Internal" ) );
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
  
  public Integer getPort( ) {
    return this.port;
  }
  
  public final ConfigResource getModel( ) {
    try {
      return new ConfigResource( this.getModelConfiguration( ) );
    } catch ( IOException ex ) {
      LOG.error( "Failed to load model: " + this.getModelConfiguration( ) + " because of: " + ex.getMessage( ), ex );
      System.exit( -1 );
      throw new RuntimeException( ex );
    }
  }
  
  public String getLocalEndpointName( ) {
    return this.uriLocal;
  }

  public URI getLocalEndpointUri( ) {
    URI uri = URI.create( this.getLocalEndpointName( ) );
    try {
      uri.parseServerAuthority( );
    } catch ( URISyntaxException ex ) {
      LOG.error( ex , ex );
    }
    return uri;
  }
  
  public String getModelConfiguration( ) {
    return String.format( "%s-model.xml", this.getName( ) );
  }
  
  @Override
  public X509Certificate getX509Certificate( ) {
    return SystemCredentialProvider.getCredentialProvider( this.getClass( ) ).getCertificate( );
  }
  
  @Override
  public List<X509Certificate> getAllX509Certificates( ) {
    return Lists.newArrayList( SystemCredentialProvider.getCredentialProvider( this.getClass( ) ).getCertificate( ) );
  }
  
  public String getUriPattern( ) {
    return this.uriPattern;
  }

  @Override
  public final int compareTo( ComponentId that ) {
    return this.name.compareTo( that.name );
  }

  @Override
  public final int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.name == null )
      ? 0
      : this.name.hashCode( ) );
    return result;
  }

  @Override
  public final boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    ComponentId other = ( ComponentId ) obj;
    if ( this.name == null ) {
      if ( other.name != null ) return false;
    } else if ( !this.name.equals( other.name ) ) return false;
    return true;
  }

  @Override
  public String toString( ) {
    return String.format( "ComponentIdentity:name=%s:port=%s:uriPattern=%s:uriLocal=%s", this.getName( ), this.getPort( ), this.getUriPattern( ), this.getLocalEndpointName( ) );
  }

  @Override public BigInteger getNumber( ) { throw new RuntimeException( "getNumber is not implemented for component principals." ); }
  @Override public void revokeSecretKey( ) { throw new RuntimeException( "getQueryId is not implemented for component principals." ); }
  @Override public final String getQueryId( ) { throw new RuntimeException( "getQueryId is not implemented for component principals." ); }  
  @Override public final String getSecretKey( ) { throw new RuntimeException( "getSecretKey is not implemented for component principals." ); }
  @Override public final void setQueryId( String queryId ) { throw new RuntimeException( "setQueryId is not implemented for component principals." ); }  
  @Override public final void setSecretKey( String secretKey ) { throw new RuntimeException( "setSecretKey is not implemented for component principals." ); }  
  @Override public final void setX509Certificate( X509Certificate cert ) { throw new RuntimeException( "setX509Certificate is not implemented for component principals." ); }
  @Override public final void revokeX509Certificate( ) { throw new RuntimeException( "revokeX509Certificate is not implemented for component principals." ); }

  public ChannelPipelineFactory getClientPipeline( ) {
    return new ChannelPipelineFactory( ) {
      
      @Override
      public ChannelPipeline getPipeline( ) throws Exception {
        return Channels.pipeline( );
      }
    };
  }

  protected static ChannelPipelineFactory helpGetClientPipeline( String fqName ) {
    try {
      return ( ChannelPipelineFactory ) ClassLoader.getSystemClassLoader( ).loadClass( fqName ).newInstance( );
    } catch ( InstantiationException ex ) {
      LOG.error( ex, ex );
    } catch ( IllegalAccessException ex ) {
      LOG.error( ex, ex );
    } catch ( ClassNotFoundException ex ) {
      LOG.error( ex, ex );
    }
    return new ChannelPipelineFactory( ) {
      
      @Override
      public ChannelPipeline getPipeline( ) throws Exception {
        return Channels.pipeline( );
      }
    };
  }

}
