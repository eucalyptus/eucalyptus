package com.eucalyptus.component;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
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
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.ws.StackConfiguration.BasicTransport;
import com.eucalyptus.ws.TransportDefinition;
import com.eucalyptus.ws.server.Pipelines;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.emory.mathcs.backport.java.util.Arrays;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class ComponentId implements HasName<ComponentId>, HasFullName<ComponentId>, Serializable {
  
  /**
   *
   */
  @Target( { ElementType.TYPE,
      ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface PolicyVendor {
    String value( );
  }
  
  /**
   * The annotated type provides an implementation of some component specific functionality for the
   * ComponentId indicated.
   */
  @Target( { ElementType.TYPE,
      ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface ComponentPart {
    Class<? extends ComponentId> value( );
  }
  
  /**
   *
   */
  @Target( { ElementType.TYPE,
      ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface ComponentMessage {
    Class<? extends ComponentId> value( );
  }
  
  /**
   * Declares the component which controls the partition of the annotated component type. e.g.,
   * cluster controllers are in partitions controlled by the cloud controller (Eucalyptus) and is so
   * annotated.
   * 
   * Cases:
   * 
   * 1. No annotation ==> in own partition.
   * 2. @Partition(OWNTYPE.class) ==> OWNTYPE.class in own partition.
   * 3. @Partition(OTHERTYPE.class) ==> sub-component of OTHERTYPE; one-to-one relationship.
   * 4. @Partition(value={OTHERTYPE.class}, manyToOne=true) ==> sub-component of OTHERTYPE;
   * many-to-one relationship.
   * 
   * @note for use on Class<? extends ComponentId>
   */
  @Target( { ElementType.TYPE,
      ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface Partition {
    Class<? extends ComponentId>[] value( ) default {};
    
    /**
     * This service type can have many siblings registered to the same parent w/in a single
     * partition; e.g., many NCs belong in the same partion as their parent CC
     */
    boolean manyToOne( ) default false;
  }
  
  /**
   * Component needs system-wide credentials (in keystore).
   * 
   * @value alias to use in the keystore; Component.name() is not specified.
   * @note for use on Class<? extends ComponentId>
   */
  @Target( { ElementType.TYPE,
      ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface GenerateKeys {
    /**
     * Key store alias
     */
    String value( ) default "";
  }
  
  /**
   * Defines a user-facing service.
   * 
   * @note for use on Class<? extends ComponentId>
   */
  @Target( { ElementType.TYPE,
      ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface PublicService {}
  
  /**
   * Annotated ComponentId defines an admin service.
   * 
   * @note for use on Class<? extends ComponentId>
   */
  @Target( { ElementType.TYPE,
      ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface AdminService {}
  
  /**
   * Component should not receive messages.
   */
  @Target( { ElementType.TYPE,
      ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface InternalService {}
  
  @Target( { ElementType.TYPE,
      ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface ServiceOperation {
    boolean user( ) default false;
  }
  
  private static final long  serialVersionUID = 1L;
  private static Logger      LOG              = Logger.getLogger( ComponentId.class );
  private String             capitalizedName;
  private final Ats          ats;
  private final Partition    partitionInfo;
  private final GenerateKeys keyInfo;
  private final String       vendorName;
  
  protected ComponentId( final String name ) {
    this( );
    this.capitalizedName = ( name == null
                                         ? this.getClass( ).getSimpleName( )
                                         : name );
  }
  
  protected ComponentId( ) {
    this.capitalizedName = this.getClass( ).getSimpleName( );
    this.ats = Ats.from( this );
    this.partitionInfo = this.ats.get( Partition.class );
    if ( this.ats.has( GenerateKeys.class ) ) {
      this.keyInfo = this.ats.get( GenerateKeys.class );
    } else {
      this.keyInfo = null;
    }
    this.vendorName = ( !this.ats.has( PolicyVendor.class )
                                                           ? "euca"
                                                           : this.ats.get( PolicyVendor.class ).value( ) );
  }
  
  public List<? extends TransportDefinition> getTransports( ) {
    return Lists.newArrayList( BasicTransport.HTTP );
  }
  
  public String getServicePath( final String... pathParts ) {
    return "/services/" + this.capitalizedName;
  }
  
  public String getInternalServicePath( final String... pathParts ) {
    return "/internal/" + this.capitalizedName;
  }
  
  public final String getVendorName( ) {
    return this.vendorName;
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
    return this.partitionParent( ).name( );
  }
  
  public final boolean isRootService( ) {
    return this.partitionParent( ).equals( this );
  }
  
  public final boolean isAncestor( final Class<? extends ComponentId> compId ) {
    if ( this.isCloudLocal( ) && Eucalyptus.class.equals( compId ) ) {
      return true;
    } else if ( this.isAlwaysLocal( ) && Empyrean.class.equals( compId ) ) {
      return true;
    } else {
      for ( ComponentId deps = this; ( deps != null ) && !deps.equals( deps.partitionParent( ) ); deps = deps.partitionParent( ) ) {
        if ( compId.equals( deps.getClass( ) ) ) {
          return true;
        }
      }
    }
    return false;
  }
  
  final ComponentId partitionParent( ) {
    if ( this.partitionInfo == null ) {
      return this;
    } else if ( this.partitionInfo.value( ).length == 0 ) {
      return this;
    } else if ( Arrays.asList( this.partitionInfo.value( ) ).contains( Empyrean.class ) ) {
      return Empyrean.INSTANCE;
    } else if ( Arrays.asList( this.partitionInfo.value( ) ).contains( this.getClass( ) ) ) {
      return this;
    } else if ( Arrays.asList( this.partitionInfo.value( ) ).contains( Eucalyptus.class ) && !this.partitionInfo.manyToOne( ) ) {
      return Eucalyptus.INSTANCE;
    } else {
      return ComponentIds.lookup( this.partitionInfo.value( )[0] );
    }
  }
  
  public final boolean isPartitioned( ) {
    return this.isRegisterable( ) && !this.equals( this.partitionParent( ) );
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
      public boolean apply( final ComponentId input ) {
        return ComponentId.this.equals( input ) || Arrays.asList( input.partitionInfo.value( ) ).contains( ComponentId.this.getClass( ) );
      }
    };
  }
  
  public Boolean hasCredentials( ) {
    return this.ats.has( GenerateKeys.class );
  }
  
  private static final ConcurrentMap<String, Class<ChannelPipelineFactory>> clientPipelines = Maps.newConcurrentMap( );
  
  public ChannelPipelineFactory getClientPipeline( ) {
    ChannelPipelineFactory factory = null;
    for ( final Class c : Classes.ancestors( this ) ) {
      if ( ( factory = Pipelines.lookup( this.getClass( ) ) ) != null ) {
        return factory;
      }
    }
    return helpGetClientPipeline( defaultClientPipelineClass );//TODO:GRZE:URGENT: fix handling of internal pipeline
  }
  
  private static final String defaultClientPipelineClass = "com.eucalyptus.ws.client.pipeline.InternalClientPipeline";
  
  protected static ChannelPipelineFactory helpGetClientPipeline( final String fqName ) {
    if ( clientPipelines.containsKey( fqName ) ) {
      try {
        return clientPipelines.get( fqName ).newInstance( );
      } catch ( final InstantiationException ex ) {
        LOG.error( ex, ex );
      } catch ( final IllegalAccessException ex ) {
        LOG.error( ex, ex );
      }
    } else {
      try {
        clientPipelines.putIfAbsent( fqName, ( Class<ChannelPipelineFactory> ) ClassLoader.getSystemClassLoader( ).loadClass( fqName ) );
        return clientPipelines.get( fqName ).newInstance( );
      } catch ( final InstantiationException ex ) {
        LOG.error( ex, ex );
      } catch ( final IllegalAccessException ex ) {
        LOG.error( ex, ex );
      } catch ( final ClassNotFoundException ex ) {
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
    } catch ( final NoSuchElementException ex ) {
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
    final URI uri = URI.create( this.getLocalEndpointName( ) );
    try {
      uri.parseServerAuthority( );
    } catch ( final URISyntaxException ex ) {
      LOG.error( ex, ex );
    }
    return uri;
  }
  
  public String getServiceModelFileName( ) {
    return String.format( "%s-model.xml", this.getName( ) );
  }
  
  @Override
  public final int compareTo( final ComponentId that ) {
    return this.name( ).compareTo( that.name( ) );
  }
  
  @Override
  public final int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result
             + ( ( this.name( ) == null )
                                         ? 0
                                         : this.name( ).hashCode( ) );
    return result;
  }
  
  @Override
  public final boolean equals( final Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( this.getClass( ) != obj.getClass( ) ) return false;
    final ComponentId other = ( ComponentId ) obj;
    if ( this.name( ) == null ) {
      if ( other.name( ) != null ) return false;
    } else if ( !this.name( ).equals( other.name( ) ) ) return false;
    return true;
  }
  
  public boolean runLimitedServices( ) {
    return false;
  }
  
  @Override
  public String toString( ) {
    final StringBuilder builder = new StringBuilder( );
    builder.append( this.getFullName( ) ).append( " " );
    builder.append( this.name( ) ).append( ":" );
    if ( this.isPartitioned( ) ) {
      builder.append( "partitioned:" );
    } else {
      builder.append( "unpartitioned:" );
    }
    if ( this.isCloudLocal( ) ) {
      builder.append( "cloudLocal:" );
    } else if ( this.isAlwaysLocal( ) ) {
      builder.append( "alwaysLocal:" );
    }
    return builder.toString( );
  }
  
  public final boolean isInternal( ) {
    return this.ats.has( InternalService.class )
           || !this.isAdminService( )
           || !this.isPublicService( )
           || ( this.partitionParent( ).equals( Empyrean.INSTANCE ) && !this.isRegisterable( ) )
           || ( this.partitionParent( ).equals( Eucalyptus.INSTANCE ) && !this.isRegisterable( ) );
  }
  
  /**
   * @return true if does not require internal system privileges, false otherwise.
   */
  public boolean isPublicService( ) {
    return this.ats.has( PublicService.class );
  }
  
  /**
   * @return true if does not require internal system privileges, false otherwise.
   */
  public boolean isAdminService( ) {
    return this.ats.has( AdminService.class );
  }
  
  /**
   * @return
   */
  public boolean isRegisterable( ) {
    return !( ServiceBuilders.lookup( this ) instanceof DummyServiceBuilder );
  }
  
  /**
   * Temporarily this includes only a registerability check.
   * 
   * @param config
   * @return
   */
  public boolean isDistributedService( ) {
    return this.isRegisterable( );
  }
  
  /**
   * Can the component be run locally (i.e., is the needed code available)
   * 
   * @param component TODO
   * @return true if the component could be run locally.
   */
  public Boolean isAvailableLocally( ) {
    return this.isAlwaysLocal( ) || ( this.isCloudLocal( ) && BootstrapArgs.isCloudController( ) )
           || this.checkComponentParts( );
  }
  
  public Boolean isManyToOnePartition( ) {
    return this.ats.has( Partition.class ) && this.ats.get( Partition.class ).manyToOne( );
  }
  
  private boolean checkComponentParts( ) {
    return true;//TODO:GRZE:add checks to ensure full component state is present
//  try {
//    return ComponentMessages.lookup( this.getComponentId( ).getClass( ) ) != null;
//  } catch ( NoSuchElementException ex ) {
//    return false;
//  }
  }
  
}
