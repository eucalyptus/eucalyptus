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

import java.io.Serializable;
import java.net.URI;
import java.util.Date;

import org.hibernate.annotations.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.FetchType;
import javax.persistence.CascadeType;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;
import org.hibernate.sql.Alias;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import com.eucalyptus.component.Component;
import com.eucalyptus.component.Service;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.id.Cluster;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.component.id.VMwareBroker;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.component.id.Arbitrator;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.entities.AbstractPersistent;

@MappedSuperclass
public abstract class ComponentConfiguration extends AbstractPersistent implements ServiceConfiguration {
  @Column( name = "config_component_partition" )
  String partition;
  @Column( name = "config_component_name" )
  String name;
  @Column( name = "config_component_hostname" )
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
  
  public abstract ComponentId getComponentId();
  
  public final Component lookupComponent() {
    return Components.lookup( this.getComponentId() );
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
    return (partition + name).compareTo( that.partition + that.name );
  }
  
  @Override
  public String toString( ) {
    return String.format( "ComponentConfiguration component=%s local=%s partition=%s name=%s uuid=%s hostName=%s port=%s servicePath=%s",
    this.getComponentId( ), this.isLocal( ), this.partition, this.name, this.getId(), this.hostName, this.port, this.servicePath );
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
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) return true;
    if ( obj == null ) return false;
    if ( !getClass( ).equals( obj.getClass( ) ) ) return false;
    ComponentConfiguration other = ( ComponentConfiguration ) obj;
    if ( name == null ) {
      if ( other.name != null ) return false;
    } else if ( !name.equals( other.name ) ) return false;
    return true;
  }
}
public class EphemeralConfiguration extends ComponentConfiguration {
  URI uri;
  ComponentId c;
  
  public EphemeralConfiguration( ComponentId c, String partition, String name, URI uri ) {
    super( partition, name, uri.getHost( ), uri.getPort( ), uri.getPath( ) );
    this.uri = uri;
    this.c = c;
  }
  public ComponentId getComponentId() {
    return c;
  }
  public String getUri() {
    return this.uri.toASCIIString( );
  }
}

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_config")
@Table( name = "config_clusters" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ClusterConfiguration extends ComponentConfiguration implements Serializable {
  @Transient
  private static String DEFAULT_SERVICE_PATH = "/axis2/services/EucalyptusCC";
  @Transient
  private static String INSECURE_SERVICE_PATH = "/axis2/services/EucalyptusGL";
  @Column(name="minvlan")
  Integer minVlan;
  @Column(name="maxvlan")
  Integer maxVlan;
  @Lob
  @Column(name="auth_cluster_x509_certificate")
  String clusterCertificate;
  @Lob
  @Column(name="auth_cluster_node_x509_certificate")
  String nodeCertificate;
  
  public ClusterConfiguration( ) {
    
  }
  public ClusterConfiguration( String partition, String name, String hostName, Integer port ) {
    super( partition, name, hostName, port, DEFAULT_SERVICE_PATH );
  }
  public ClusterConfiguration( String partition, String name, String hostName, Integer port, Integer minVlan, Integer maxVlan ) {
    super( partition, name, hostName, port, DEFAULT_SERVICE_PATH );
    this.minVlan = minVlan;
    this.maxVlan = maxVlan;
  }
  public String getInsecureServicePath() {
    return INSECURE_SERVICE_PATH;
  }
  public String getInsecureUri() {
    return "http://" + this.getHostName() + ":" + this.getPort() + INSECURE_SERVICE_PATH;
  }
  
  public ComponentId getComponentId() {
    return ComponentIds.lookup(Cluster.class);
  }
  @Override
  public Boolean isLocal() {
    return false;
  }
}

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_config")
@Table( name = "config_sc" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StorageControllerConfiguration extends ComponentConfiguration implements Serializable {
  @Transient
  private static String DEFAULT_SERVICE_PATH = "/services/Storage";
  public StorageControllerConfiguration( ) {
    
  }
  public StorageControllerConfiguration( String partition, String name, String hostName, Integer port ) {
    super( partition, name, hostName, port, DEFAULT_SERVICE_PATH );
  }
  public ComponentId getComponentId() {
    return ComponentIds.lookup(Storage.class);
  }
}
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_config")
@Table( name = "config_walrus" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class WalrusConfiguration extends ComponentConfiguration implements Serializable {
  @Transient
  private static String DEFAULT_SERVICE_PATH = "/services/Walrus";
  public WalrusConfiguration( ) {
  }
  public WalrusConfiguration( String name, String hostName, Integer port ) {
    super( "walrus", name, hostName, port, DEFAULT_SERVICE_PATH );
  }
  public ComponentId getComponentId() {
    return ComponentIds.lookup(Walrus.class);
  }
}
@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_config")
@Table( name = "config_eucalyptus" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class EucalyptusConfiguration extends ComponentConfiguration implements Serializable {
  @Transient
  private static String DEFAULT_SERVICE_PATH = "/services/Eucalyptus";
  public EucalyptusConfiguration( ) {
  }
  public EucalyptusConfiguration( String name, String hostName, Integer port ) {
    super( "eucalyptus", name, hostName, port, DEFAULT_SERVICE_PATH );
  }
  public ComponentId getComponentId() {
    return ComponentIds.lookup(Eucalyptus.class);
  }
}


@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_config")
@Table( name = "config_system" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class System implements Serializable {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "config_system_id" )
  String id
  @Column( name = "config_system_default_kernel" )
  String defaultKernel
  @Column( name = "config_system_default_ramdisk" )
  String defaultRamdisk
  @Column( name = "config_system_registration_id" )
  String registrationId
}

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_config")
@Table( name = "config_vmwarebroker" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class VMwareBrokerConfiguration extends ComponentConfiguration implements Serializable {
  @Transient
  private static String DEFAULT_SERVICE_PATH = "/services/VMwareBroker";
  public VMwareBrokerConfiguration( ) {
  }
  public VMwareBrokerConfiguration( String partition, String name, String hostName, Integer port ) {
    super( partition, name, hostName, port, DEFAULT_SERVICE_PATH );
  }
  public ComponentId getComponentId() {
    return ComponentIds.lookup(VMwareBroker.class);
  }
}

@Entity @javax.persistence.Entity
@PersistenceContext(name="eucalyptus_config")
@Table( name = "config_arbitrator" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ArbitratorConfiguration extends ComponentConfiguration implements Serializable {
  @Transient
  private static String DEFAULT_SERVICE_PATH = "/services/Arbitrator";
  public ArbitratorConfiguration( ) {
  }
  public ArbitratorConfiguration( String partition, String name, String hostName, Integer port ) {
    super( partition, name, hostName, port, DEFAULT_SERVICE_PATH );
  }
  public ComponentId getComponentId() {
    return ComponentIds.lookup(Arbitrator.class);
  }
}

