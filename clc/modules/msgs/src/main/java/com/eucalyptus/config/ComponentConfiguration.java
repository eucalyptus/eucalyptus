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

package com.eucalyptus.config;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.NoSuchElementException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import com.eucalyptus.bootstrap.CanBootstrap;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.Component.Transition;
import com.eucalyptus.component.ComponentFullName;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.fsm.StateMachine;

@Entity
@PersistenceContext( name = "eucalyptus_config" )
@Table( name = "config_component_base" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
public class ComponentConfiguration extends AbstractPersistent implements ServiceConfiguration {
  @Transient
  private static final long serialVersionUID = 1L;
  @Transient
  private static Logger     LOG              = Logger.getLogger( ComponentConfiguration.class );

  @Column( name = "config_component_partition" )
  private String            partition;
  @NaturalId
  @Column( name = "config_component_name", updatable = false, unique = true, nullable = false )
  private String            name;
  @ConfigurableField( description = "Address which the cloud controller should use to contact this service.", displayName = "Host name", readonly = true )
  @Column( name = "config_component_hostname" )
  private String            hostName;
  @Column( name = "config_component_port" )
  private Integer           port;
  @Column( name = "config_component_service_path" )
  private String            servicePath;
  
  protected ComponentConfiguration( ) {

  }
  
  protected ComponentConfiguration( String partition, String name, String hostName, String servicePath ) {
    super( );
    this.partition = partition;
    this.name = name;
    this.hostName = hostName;
    this.servicePath = servicePath;
  }
  
  protected ComponentConfiguration( String partition, String name, String hostName, Integer port, String servicePath ) {
    this.partition = partition;
    this.name = name;
    this.hostName = hostName;
    this.port = port;
    this.servicePath = servicePath;
  }
  
  @Override
  public InetSocketAddress getSocketAddress( ) {
    return new InetSocketAddress( this.getHostName( ), this.getPort( ) );
  }
  
  @Override
  public InetAddress getInetAddress( ) {
    return this.getSocketAddress( ).getAddress( );
  }
  
  /**
   * Use the facade instead.
   * 
   * @see ServiceUris#remote(ServiceConfiguration, String...)
   */
  @Override
  @Deprecated
  public URI getUri( ) {
    return ServiceUris.remote( this );
  }
  
  @Override
  public String getName( ) {
    return this.name;
  }
  
  @Override
  @Deprecated
  public final ComponentId getComponentId( ) {
    return lookupComponentId( );
  }
  
  public ComponentId lookupComponentId( ) {
    if ( !Ats.from( this ).has( ComponentPart.class ) ) {
      throw new RuntimeException( "BUG: A component configuration must have the @ComponentPart(ComponentId.class) annotation" );
    } else {
      return ComponentIds.lookup( Ats.from( this ).get( ComponentPart.class ).value( ) );
    }
  }
  
  @Override
  public final CanBootstrap lookupBootstrapper( ) {
    return Components.lookup( this.lookupComponentId( ) ).getBootstrapper( );
  }
  
  @Override
  public Boolean isHostLocal( ) {
    try {
      return this.port == -1
        ? true
        : Internets.testLocal( this.getHostName( ) );
    } catch ( Exception e ) {
      return false;
    }
  }
  
  @Override
  public Boolean isVmLocal( ) {
    try {
      return this.port == -1
        ? true
        : Internets.testLocal( this.getHostName( ) );
    } catch ( Exception e ) {
      return false;
    }
  }
  
  @Override
  public final FullName getFullName( ) {
    return ComponentFullName.getInstance( this );
  }
  
  @Override
  public int compareTo( ServiceConfiguration that ) {
    //ASAP: FIXME: GRZE useful ordering here plox.
    return ( this.partition + this.name ).compareTo( that.getPartition( ) + that.getName( ) );
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "ServiceConfiguration " ).append( this.lookupComponentId( ).name( ) ).append( " " );
    try {
      builder.append( this.getFullName( ).toString( ) ).append( " " ).append( this.hostName ).append( ":" ).append( this.port ).append( ":" ).append( this.servicePath ).append( ":" );
    } catch ( Exception ex ) {
      builder.append( this.partition ).append( ":" ).append( this.name ).append( ":" ).append( this.hostName ).append( ":" ).append( this.port ).append( ":" ).append( this.servicePath ).append( ":" );
    }
    if ( this.isVmLocal( ) ) {
      builder.append( "vm-local:" );
    }
    if ( this.isHostLocal( ) ) {
      builder.append( "host-local:" );
    }
    builder.append( this.lookupState( ) );
    return builder.toString( );
  }
  
  public String toStrings( ) {
    return String.format( "ServiceConfiguration %s:%s:%s:%s:%s:%s:%s%s",
                          this.getComponentId( ).name( ), this.partition, this.name, this.hostName, this.port, this.servicePath,
                          ( this.isVmLocal( )
                            ? "vm-local:"
                            : "" ),
                          ( this.isHostLocal( )
                            ? "host-local:"
                            : "" ) );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 0; //super.hashCode( );
    result = prime * result + ( ( this.name == null )
        ? 0
        : this.name.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object that ) {
    if ( this == that ) return true;
    if ( that == null ) return false;
    if ( !getClass( ).equals( that.getClass( ) ) ) return false;
    ComponentConfiguration other = ( ComponentConfiguration ) that;
    if ( this.name == null ) {
      if ( other.name != null ) return false;
    } else if ( !this.name.equals( other.name ) ) return false;
    return true;
  }
  
  @Override
  public String getPartition( ) {
    return this.partition;
  }
  
  @Override
  public void setPartition( final String partition ) {
    this.partition = partition;
  }
  
  @Override
  public String getHostName( ) {
    return this.hostName;
  }
  
  @Override
  public void setHostName( final String hostName ) {
    this.hostName = hostName;
  }
  
  @Override
  public Integer getPort( ) {
    return this.port;
  }
  
  @Override
  public void setPort( Integer port ) {
    this.port = port;
  }
  
  @Override
  public String getServicePath( ) {
    return this.servicePath;
  }
  
  @Override
  public void setServicePath( final String servicePath ) {
    this.servicePath = servicePath;
  }
  
  @Override
  public void setName( final String name ) {
    this.name = name;
  }
  
  @Override
  public Partition lookupPartition( ) {
    return Partitions.lookupByName( this.getPartition() );
  }
  
  @Override
  public StateMachine<ServiceConfiguration, Component.State, Component.Transition> getStateMachine( ) {
    return Components.lookup( this.lookupComponentId( ) ).getStateMachine( this );
  }
  
  @Override
  public StateMachine<ServiceConfiguration, State, Transition> lookupStateMachine( ) {
    return this.getStateMachine( );
  }
  
  @Override
  public Component.State lookupState( ) {
    try {
      return this.lookupStateMachine( ).getState( );
    } catch ( NoSuchElementException ex ) {
      return Component.State.PRIMORDIAL;
    }
  }
}
