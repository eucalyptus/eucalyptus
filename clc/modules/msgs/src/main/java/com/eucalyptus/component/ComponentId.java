/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.component;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.component.annotation.AdminService;
import com.eucalyptus.component.annotation.AwsServiceName;
import com.eucalyptus.component.annotation.FaultLogPrefix;
import com.eucalyptus.component.annotation.GenerateKeys;
import com.eucalyptus.component.annotation.InternalService;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.component.annotation.PolicyVendor;
import com.eucalyptus.component.annotation.PublicService;
import com.eucalyptus.component.id.Eucalyptus;
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
  
  public List<? extends TransportDefinition> getTransports( ) {
    return Lists.newArrayList( BasicTransport.HTTP );
  }
  
  public String getServicePath( final String... pathParts ) {
    return "/services/" + this.capitalizedName;
  }
  
  public String getInternalServicePath( final String... pathParts ) {
    return "/internal/" + this.capitalizedName;
  }

  /**
   * Components may declare a namespace suffix for internal use.
   * 
   * <p>To avoid binding conflicts between components a namespace suffix may be
   * declared. By default all components use the same namespace in the internal
   * binding.</p>
   * 
   * @return The suffix or null for no suffix
   */
  @Nullable
  public String getInternalNamespaceSuffix() {
    return null;
  }
  
  public Map<String,String> getServiceQueryParameters() {
    return Collections.emptyMap();
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
  
  public String getAwsServiceName( ) {
    if ( this.ats.has( AwsServiceName.class ) ) {
      return this.ats.get( AwsServiceName.class ).value( );
    } else {
      return "eucalyptus";
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
