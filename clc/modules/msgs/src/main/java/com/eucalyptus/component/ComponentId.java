package com.eucalyptus.component;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.AnonymousMessage;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.Internets;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.StackConfiguration.Transport;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class ComponentId implements HasName<ComponentId>, HasFullName<ComponentId>, Serializable {
  private static Logger LOG = Logger.getLogger( ComponentId.class );
  private final String  capitalizedName;
  
  protected ComponentId( String name ) {
    this.capitalizedName = ( name == null
      ? this.getClass( ).getSimpleName( )
      : name );
  }
  
  protected ComponentId( ) {
    this.capitalizedName = this.getClass( ).getSimpleName( );
  }
  
  public Transport getTransport( ) {
    return Transport.HTTP;
  }
  
  public String getServicePath( String... pathParts ) {
    return "/services/" + this.capitalizedName;
  }
  
  public String getInternalServicePath( String... pathParts ) {
    return "/internal/" + this.capitalizedName;
  }
  
  public String getVendorName( ) {
    return "euca";
  }
  
  public final String name( ) {
    return this.capitalizedName.toLowerCase( );
  }
  
  @Override
  public final String getName( ) {
    return this.name( );
  }
  
  @Override
  public final FullName getFullName( ) {
    return ComponentFullName.getInstance( ServiceConfigurations.createEphemeral( this ), this.getPartition( ), this.name( ) );
  }
  
  @Override
  public String getPartition( ) {
    return ( this.isPartitioned( )
      /** && !this.isRegisterable( ) **/
      ? Eucalyptus.INSTANCE.name( )
      : ( ( Unpartioned ) this ).getPartition( ) );
  }
  
  public final boolean isRootService( ) {
    return this.serviceDependencies( ).isEmpty( );
  }
  
  public final boolean isPartitioned( ) {
    return !Unpartioned.class.isAssignableFrom( this.getClass( ) );
  }
  
  public List<Class<? extends ComponentId>> serviceDependencies( ) {
    return Lists.newArrayList( );
  }
  
  public final Boolean isCloudLocal( ) {
    return Eucalyptus.INSTANCE.isRelated( ).apply( this );
  }
  
  public final Boolean isAlwaysLocal( ) {
    return Empyrean.INSTANCE.isRelated( ).apply( this );
  }
  
  public Predicate<ComponentId> isRelated( ) {
    return new Predicate<ComponentId>( ) {
      
      @Override
      public boolean apply( ComponentId input ) {
        return ComponentId.this.equals( input ) || input.serviceDependencies( ).contains( ComponentId.this.getClass( ) );
      }
    };
  }
  
  public Boolean hasCredentials( ) {
    return false;
  }
  
  private static final ConcurrentMap<String, Class<ChannelPipelineFactory>> clientPipelines = Maps.newConcurrentMap( );
  
  public ChannelPipelineFactory getClientPipeline( ) {
    return helpGetClientPipeline( defaultClientPipelineClass );//TODO:GRZE:URGENT: fix handling of internal pipeline
  }
  
  private static final String defaultClientPipelineClass = "com.eucalyptus.ws.client.pipeline.InternalClientPipeline";
  
  protected static ChannelPipelineFactory helpGetClientPipeline( String fqName ) {
    if ( clientPipelines.containsKey( fqName ) ) {
      try {
        return clientPipelines.get( fqName ).newInstance( );
      } catch ( InstantiationException ex ) {
        LOG.error( ex, ex );
      } catch ( IllegalAccessException ex ) {
        LOG.error( ex, ex );
      }
    } else {
      try {
        clientPipelines.putIfAbsent( fqName, ( Class<ChannelPipelineFactory> ) ClassLoader.getSystemClassLoader( ).loadClass( fqName ) );
        return clientPipelines.get( fqName ).newInstance( );
      } catch ( InstantiationException ex ) {
        LOG.error( ex, ex );
      } catch ( IllegalAccessException ex ) {
        LOG.error( ex, ex );
      } catch ( ClassNotFoundException ex ) {
        LOG.error( ex, ex );
      }
    }
    return new ChannelPipelineFactory( ) {
      
      @Override
      public ChannelPipeline getPipeline( ) throws Exception {
        return Channels.pipeline( );
      }
    };
  }
  
  public final String getEntryPoint( ) {
    return this.capitalizedName + "RequestQueueEndpoint";
  }
  
  public final String getCapitalizedName( ) {
    return this.capitalizedName;
  }
  
  public Class<? extends BaseMessage> lookupBaseMessageType( ) {
    try {
      return ComponentMessages.lookup( this.getClass( ) );
    } catch ( NoSuchElementException ex ) {
      return AnonymousMessage.class;
    }
  }
  
  public Integer getPort( ) {
    return 8773;
  }
  
  public String getLocalEndpointName( ) {
    return String.format( "vm://%sInternal", this.getClass( ).getSimpleName( ) );
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
  
  public String getServiceModelFileName( ) {
    return String.format( "%s-model.xml", this.getName( ) );
  }
  
  @Override
  public final int compareTo( ComponentId that ) {
    return this.name( ).compareTo( that.name( ) );
  }
  
  @Override
  public final int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.name( ) == null )
      ? 0
      : this.name( ).hashCode( ) );
    return result;
  }
  
  @Override
  public final boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    ComponentId other = ( ComponentId ) obj;
    if ( this.name( ) == null ) {
      if ( other.name( ) != null ) return false;
    } else if ( !this.name( ).equals( other.name( ) ) ) return false;
    return true;
  }
  
  public static abstract class Unpartioned extends ComponentId {
    
    public Unpartioned( ) {
      super( );
    }
    
    public Unpartioned( String name ) {
      super( name );
    }
    
    @Override
    public String getPartition( ) {
      return this.isCloudLocal( ) && !this.isRegisterable( )
        ? Eucalyptus.INSTANCE.name( )
        : ( this.isAlwaysLocal( )
          ? Empyrean.INSTANCE.name( )
          : this.name( ) );
    }
    
  }
  
  public boolean runLimitedServices( ) {
    return false;
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( this.getFullName( ) ).append( " " );
    builder.append( this.name( ) ).append( ":" );
    if ( this.isPartitioned( ) ) {
      builder.append( "partitioned:" );
    } else {
      builder.append( "unpartitioned:" );
    }
    if ( !this.serviceDependencies( ).isEmpty( ) ) {
      builder.append( "deps=" ).append( Lists.transform( this.serviceDependencies( ), new Function<Class, String>( ) {
        
        @Override
        public String apply( Class arg0 ) {
          return arg0.getSimpleName( );
        }
      } ) ).append( ":" );
    }
    if ( this.isCloudLocal( ) ) {
      builder.append( "cloudLocal:" );
    } else if ( this.isAlwaysLocal( ) ) {
      builder.append( "alwaysLocal:" );
    }
    if ( this.runLimitedServices( ) ) {
      builder.append( "runs-limited-services:" );
    }
    return builder.toString( );
  }
  
  public final boolean isInternal( ) {
    return !this.isAdminService( ) && !this.isUserService( );
  }
  
  public boolean isUserService( ) {
    return false;
  }
  
  public boolean isAdminService( ) {
    return false;
  }
  
  public boolean isRegisterable( ) {
    return !( ServiceBuilders.lookup( this ) instanceof DummyServiceBuilder );
  }
  
  /**
   * Can the component be run locally (i.e., is the needed code available)
   * 
   * @param component TODO
   * @return true if the component could be run locally.
   */
  public Boolean isAvailableLocally( ) {
    return this.isAlwaysLocal( ) || ( this.isCloudLocal( ) && BootstrapArgs.isCloudController( ) );
  }
  
}
