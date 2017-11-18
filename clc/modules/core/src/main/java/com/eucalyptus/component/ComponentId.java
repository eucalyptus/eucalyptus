/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.component;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.component.annotation.AdminService;
import com.eucalyptus.component.annotation.AwsServiceName;
import com.eucalyptus.component.annotation.ComponentApi;
import com.eucalyptus.component.annotation.PublicComponentAccounts;
import com.eucalyptus.component.annotation.ComponentDatabase;
import com.eucalyptus.component.annotation.DatabaseNamingStrategy;
import com.eucalyptus.component.annotation.FaultLogPrefix;
import com.eucalyptus.component.annotation.GenerateKeys;
import com.eucalyptus.component.annotation.InternalService;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.component.annotation.PublicService;
import com.eucalyptus.component.annotation.ServiceNames;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.HasName;
import com.eucalyptus.ws.StackConfiguration.BasicTransport;
import com.eucalyptus.ws.TransportDefinition;
import com.eucalyptus.ws.WebServices;
import com.eucalyptus.ws.client.ClientChannelInitializers;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public abstract class ComponentId implements HasName<ComponentId>, HasFullName<ComponentId>, Serializable {
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

  /**
   * Maps the componentId to an optional set of account Ids
   * @return
   */
  public Optional<? extends Iterable<UserPrincipal>> getPublicComponentAccounts() {
    PublicComponentAccounts accnts = this.getClass().getAnnotation(PublicComponentAccounts.class);
    if (accnts != null) {
      return Optional.of(Iterables.transform(Lists.newArrayList(accnts.value()), new Function<String, UserPrincipal>() {

        @Nullable
        @Override
        public UserPrincipal apply(@Nullable String s) {
          try {
            return Accounts.lookupSystemAccountByAlias(s);
          } catch (Exception e) {
            return null;
          }
        }
      }));
    }
    return Optional.absent();
  }

  public List<? extends TransportDefinition> getTransports( ) {
    return Lists.newArrayList( BasicTransport.HTTP );
  }

  public boolean isUseServiceHostName( ) {
    return false;
  }

  public String getServicePath( final String... pathParts ) {
    return "/services/" + this.capitalizedName;
  }

  public String getInternalServicePath( final String... pathParts ) {
    return "/internal/" + this.capitalizedName;
  }

  public Map<String,String> getServiceQueryParameters() {
    return Collections.emptyMap();
  }

  public final String getVendorName( ) {
    return this.vendorName;
  }

  public final String name( ) {
    return this.capitalizedName.toLowerCase();
  }

  @Override
  public final String getName( ) {
    return this.name();
  }

  @Override
  public final FullName getFullName( ) {
    return ComponentFullName.getInstance(ServiceConfigurations.createEphemeral(this),
                                         this.getPartition(), this.name());
  }

  @Override
  public String getPartition( ) {
    return this.partitionParent( ).name();
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

  public boolean isPartitioned( ) {
    return this.isRegisterable( ) && !this.equals( this.partitionParent( ) );
  }

  public Boolean isCloudLocal( ) {
    return Eucalyptus.INSTANCE.isRelated( ).apply( this );
  }

  public final Boolean isAlwaysLocal( ) {
    return Empyrean.INSTANCE.isRelated( ).apply( this );
  }

  public Predicate<ComponentId> isRelated( ) {
    return new Predicate<ComponentId>( ) {

      @Override
      public boolean apply( final ComponentId input ) {
        return ComponentId.this.equals( input ) || ( input.partitionInfo != null && Arrays.asList( input.partitionInfo.value( ) ).contains( ComponentId.this.getClass( ) ) );
      }
    };
  }

  public Boolean hasCredentials( ) {
    return this.ats.has( GenerateKeys.class );
  }

  private static final ConcurrentMap<String, Class<ChannelInitializer<?>>> clientChannelInitializers = Maps.newConcurrentMap( );

  public Bootstrap getClientBootstrap( ) {
    return WebServices.clientBootstrap( );
  }

  public final ChannelInitializer<?> getClientChannelInitializer( ) {
    ChannelInitializer<?> channelInitializer;
    for ( final Class c : Classes.ancestors( this ) ) {
      if ( ( channelInitializer = ClientChannelInitializers.lookup( c ) ) != null ) {
        return channelInitializer;
      }
    }
    throw new IllegalStateException( "No channel initializer" );
  }

  public final String getCapitalizedName( ) {
    return this.capitalizedName;
  }

  public final String getFaultLogPrefix( ) {
	  if ( Ats.from( this ).has( FaultLogPrefix.class ) ) {
		  String value = Ats.from( this ).get( FaultLogPrefix.class ).value( );
		  return (value != null  && !"".equals(value) ) ? value : this.name( );
	  } else {
		  return Ats.from( Empyrean.class ).get( FaultLogPrefix.class ).value( );//this requires that Empyrean.class is annotated as @FaultLogging("cloud")
	  }
  }

  public Integer getPort( ) {
    return 8773;
  }

  public String getChannelName( ) {
    return String.format( "%s-request", getName( ) );
  }

  public String getServiceModelFileName( ) {
    return String.format("%s-model.xml", this.getName());
  }

  public Set<String> getCertificateUsages( ) {
    return Collections.emptySet( );
  }

  public X509Certificate getCertificate( final String usage ) {
    return null;
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
    return builder.toString();
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
    return this.ats.has(AdminService.class);
  }

  /**
    * @return true if this component represents the api for another component
   */
  public boolean isApi( ) {
    return this.ats.has( ComponentApi.class ) && this.ats.get( ComponentApi.class ).value( ).equals( getClass( ) );
  }

  /**
   * @return true if this component implements the api defined by another component
   */
  public boolean hasApi( ) {
    return this.ats.has( ComponentApi.class ) && !isApi( );
  }

  /**
   * @return true if this component implements the api defined by another component
   */
  public boolean hasApi( Class<? extends ComponentId> api ) {
    return hasApi( ) && this.ats.get( ComponentApi.class ).value( ).equals( api );
  }

  /**
   * @return the api component class, which will be this components class if !hasApi()
   */
  public Class<? extends ComponentId> toApiClass( ) {
    return hasApi( ) ?
        this.ats.get( ComponentApi.class ).value( ) :
        getClass( );
  }

  /**
   * Does this component support impersonation.
   *    * @return true if impersonation is supported.
   */
  public boolean isImpersonationSupported( ) {
    return isPublicService( );
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

  /**
   * Temporary work around allowing components to opt out of DNS.
   *
   * @deprecated
   */
  @Deprecated
  public boolean isDnsSupported( ) {
    return true;
  }

  public String getAwsServiceName( ) {
    if ( this.ats.has( AwsServiceName.class ) ) {
      return this.ats.get( AwsServiceName.class ).value( );
    } else {
      return "eucalyptus";
    }
  }

  /**
   * Get any additional DNS host labels for this component.
   */
  public Set<String> getServiceNames( ) {
    Set<String> names = Collections.emptySet( );
    if ( this.ats.has( ServiceNames.class ) ) {
      names = ImmutableSet.copyOf( this.ats.get( ServiceNames.class ).value( ) );
    } else if ( this.ats.has( AwsServiceName.class ) ) {
      names = Collections.singleton( getAwsServiceName( ) );
    }
    return names;
  }

  /**
   * Get all DNS host labels for this component.
   */
  public Set<String> getAllServiceNames( ) {
    final Set<String> names = Sets.newLinkedHashSet( );
    names.add( name( ) );
    names.addAll( getServiceNames( ) );
    return names;
  }

  public DatabaseNamingStrategy getDatabaseNamingStrategy( ) {
    if ( this.ats.has( ComponentDatabase.class ) ) {
      return this.ats.get( ComponentDatabase.class ).namingStrategy( );
    } else {
      return DatabaseNamingStrategy.Schema;
    }
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
