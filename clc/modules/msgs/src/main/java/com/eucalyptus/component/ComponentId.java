package com.eucalyptus.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.NoSuchElementException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.credential.HmacPrincipal;
import com.eucalyptus.auth.principal.credential.X509Principal;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.component.auth.SystemCredentialProvider;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.AnonymousMessage;
import com.eucalyptus.system.LogLevels;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasName;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class ComponentId implements HasName<ComponentId> {
  private static Logger       LOG         = Logger.getLogger( ComponentId.class );
  private static final String EMPTY_MODEL = "  <mule xmlns=\"http://www.mulesource.org/schema/mule/core/2.0\"\n"
                                            +
                                            "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                            +
                                            "      xmlns:spring=\"http://www.springframework.org/schema/beans\"\n"
                                            +
                                            "      xmlns:vm=\"http://www.mulesource.org/schema/mule/vm/2.0\"\n"
                                            +
                                            "      xmlns:euca=\"http://www.eucalyptus.com/schema/cloud/1.6\"\n"
                                            +
                                            "      xsi:schemaLocation=\"\n"
                                            +
                                            "       http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.0.xsd\n"
                                            +
                                            "       http://www.mulesource.org/schema/mule/core/2.0 http://www.mulesource.org/schema/mule/core/2.0/mule.xsd\n" +
                                            "       http://www.mulesource.org/schema/mule/vm/2.0 http://www.mulesource.org/schema/mule/vm/2.0/mule-vm.xsd\n" +
                                            "       http://www.eucalyptus.com/schema/cloud/1.6 http://www.eucalyptus.com/schema/cloud/1.6/euca.xsd\">\n" +
                                            "</mule>\n";
  protected static final List<Class<? extends ComponentId>> getList( final Class<? extends ComponentId> id ) {
    List<Class<? extends ComponentId>> hi = new ArrayList<Class<? extends ComponentId>>( ) {{
      this.add( id );
    }}; 
    return hi;
  }

  private final String        name;
  private final String        capitalizedName;
  private final String        entryPoint;
  private final Integer       port;
  private final String        modelContent;
  private String              uriPattern;
  private String              uriLocal;
  
  protected ComponentId( String name ) {
    this.capitalizedName = name;
    this.name = this.capitalizedName.toLowerCase( );
    this.entryPoint = this.capitalizedName + "RequestQueueEndpoint";
    this.port = 8773;
    this.uriPattern = "http://%s:%d/internal/%s";
    this.uriLocal = String.format( "vm://%sInternal", this.getClass( ).getSimpleName( ) );
    this.modelContent = loadModel( );
  }
  
  protected ComponentId( ) {
    this.capitalizedName = this.getClass( ).getSimpleName( );
    this.name = this.capitalizedName.toLowerCase( );
    this.entryPoint = this.capitalizedName + "RequestQueueEndpoint";
    this.port = 8773;
    this.uriPattern = "http://%s:%d/internal/%s";
    this.uriLocal = String.format( "vm://%sInternal", this.getClass( ).getSimpleName( ) );
    this.modelContent = loadModel( );
  }
  
  private String loadModel( ) {
    StringWriter out = new StringWriter( );
    try {
      InputStream in = Thread.currentThread( ).getContextClassLoader( ).getResourceAsStream( this.getServiceModelFileName( ) );
      if ( in == null ) {
        return EMPTY_MODEL;
      } else {
        IOUtils.copy( in, out );
        in.close( );
        out.flush( );
        String outString = out.toString( );
        if ( LogLevels.EXTREME ) {
          LOG.trace( "Loaded model for: " + this );
          LOG.trace( outString );
        }
        return outString;
      }
    } catch ( IOException ex ) {
      LOG.error( ex, ex );
      throw BootstrapException.throwError( "BUG! BUG! Failed to load configuration specified for Component: " + this.name, ex );
    }
  }
  
  public String name( ) {
    return this.name;
  }
  
  @Override
  public String getName( ) {
    return this.name;
  }
  
  public boolean isPartitioned( ) {
    return !this.isCloudLocal( );
  }
  
  public FullName makeFullName( String partition, String name, String... parts ) {
    if( this.isPartitioned( ) ) {
      return new ComponentFullName( this, partition, name, parts );
    } else {
      return new ComponentFullName( this, this.getName( ), name, parts );
    }
  }
  
  private static final List<Class<? extends ComponentId>> EMPTY = Lists.newArrayList( );
  public List<Class<? extends ComponentId>> serviceDependencies( ) { 
    return EMPTY;
  }

  public abstract Boolean isCloudLocal( );
  
  public abstract Boolean hasDispatcher( );
  
  public abstract Boolean isAlwaysLocal( );
  
  public Boolean hasCredentials( ) {
    return false;
  }
  
  
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
  
  /**
   * @return the entryPoint
   */
  public String getEntryPoint( ) {
    return this.entryPoint;
  }
  
  /**
   * @return the capitalizedName
   */
  public String getCapitalizedName( ) {
    return this.capitalizedName;
  }
  
  public Class<? extends BaseMessage> lookupBaseMessageType( ) {
    try {
      return ComponentMessages.lookup( this.getClass( ) );
    } catch ( NoSuchElementException ex ) {
      return AnonymousMessage.class;
    }
  }

  public FullName getMyFullName( ServiceConfiguration conf ) {
    return this.makeFullName( conf.getPartition( ), conf.getName( ) );
  }

  public Integer getPort( ) {
    return this.port;
  }
  
  public String getLocalEndpointName( ) {
    return this.uriLocal;
  }
  
  public URI getLocalEndpointUri( ) {
    URI uri = URI.create( this.getLocalEndpointName( ) );
    try {
      uri.parseServerAuthority( );
    } catch ( URISyntaxException ex ) {
      LOG.error( ex, ex );
    }
    return uri;
  }
  
  public final String getServiceModel( ) {
    return this.modelContent;
  }
  
  public final Reader getServiceModelAsReader( ) {
    return new StringReader( this.modelContent );
  }
  
  public String getServiceModelFileName( ) {
    return String.format( "%s-model.xml", this.getName( ) );
  }
  
  public String getUriPattern( ) {
    return this.uriPattern;
  }
  
  /**
   * Get the HTTP service path
   */
  public final URI makeRemoteUri( String hostName, Integer port ) {
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
    return String.format( "ComponentIdentity:name=%s:port=%s:uriPattern=%s:uriLocal=%s", this.getName( ), this.getPort( ), this.getUriPattern( ),
                          this.getLocalEndpointName( ) );
  }
}
