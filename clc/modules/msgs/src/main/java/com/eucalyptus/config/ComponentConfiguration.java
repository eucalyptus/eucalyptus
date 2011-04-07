/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.config;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ComponentPart;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Service;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.Internets;

@MappedSuperclass
public class ComponentConfiguration extends AbstractPersistent implements ServiceConfiguration {
  @Column( name = "config_component_partition", nullable=false )
  String partition;
  @Column( name = "config_component_name", unique=true, nullable=false )
  String name;
  @Column( name = "config_component_hostname", nullable=false )
  String hostName;
  @Column( name = "config_component_port" )
  Integer port;
  @Column( name = "config_component_service_path" )
  String servicePath;
  
  public ComponentConfiguration( ) {
    
  }
  
  public ComponentConfiguration( String partition, String name, String hostName, String servicePath ) {
    super( );
    this.partition = partition;
    this.name = name;
    this.hostName = hostName;
    this.servicePath = servicePath;
  }
  public ComponentConfiguration( String partition, String name, String hostName, Integer port, String servicePath ) {
    this.partition = partition;
    this.name = name;
    this.hostName = hostName;
    this.port = port;
    this.servicePath = servicePath;
  }
  
  public String getUri() {
    return "http://" + this.getHostName() + ":" + this.getPort() + this.getServicePath();
  }
  
  public String getName() {
    return name;
  }
  

  @Deprecated
  public final ComponentId getComponentId() {
    return lookupComponentId();
  }
  
  public ComponentId lookupComponentId() {
    if( !Ats.from( this ).has( ComponentPart.class ) ) {
      throw new RuntimeException( "BUG: A component configuration must have the @ComponentPart(ComponentId.class) annotation" );
    } else {
      return ComponentIds.lookup( Ats.from( this ).get( ComponentPart.class ).value( ) );
    }
  }
  
  public final Component lookupComponent() {
    return Components.lookup( this.lookupComponentId() );
  }
  
  public final Service lookupService() {
    return Components.lookup( this.getComponentId() ).lookupService( this );
  }
  
  public Boolean isLocal() {
    try {
      return this.port == -1 ? true : Internets.testLocal( this.getHostName( ) );
    } catch ( Exception e ) {
      return false;
    }
  }
  
  public final FullName getFullName( ) {
    return this.getComponentId().makeFullName( this );
  }
  
  
  public int compareTo(ServiceConfiguration that) {
    //ASAP: FIXME: GRZE useful ordering here plox.
    return (partition + name).compareTo( that.getPartition( ) + that.getName( ) );
  }
  
  @Override
  public String toString( ) {
    return String.format( "ComponentConfiguration component=%s local=%s partition=%s name=%s hostName=%s port=%s servicePath=%s",
    this.getComponentId( ), this.isLocal( ), this.partition, this.name, this.hostName, this.port, this.servicePath );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
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
    if ( name == null ) {
      if ( other.name != null ) return false;
    } else if ( !name.equals( other.name ) ) return false;
    return true;
  }

  public String getPartition( ) {
    return this.partition;
  }

  public void setPartition( String partition ) {
    this.partition = partition;
  }

  public String getHostName( ) {
    return this.hostName;
  }

  public void setHostName( String hostName ) {
    this.hostName = hostName;
  }

  public Integer getPort( ) {
    return this.port;
  }

  public void setPort( Integer port ) {
    this.port = port;
  }

  public String getServicePath( ) {
    return this.servicePath;
  }

  public void setServicePath( String servicePath ) {
    this.servicePath = servicePath;
  }

  public void setName( String name ) {
    this.name = name;
  }
}
  