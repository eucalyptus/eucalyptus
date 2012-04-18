/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.cluster;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableIdentifier;
import com.eucalyptus.network.NetworkGroups;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_config" )
@Table( name = "config_clusters" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ComponentPart( ClusterController.class )
@ConfigurableClass( root = "cluster", alias = "basic", description = "Basic cluster controller configuration.", singleton = false, deferred = true )
public class ClusterConfiguration extends ComponentConfiguration implements Serializable {
  @Transient
  private static String         DEFAULT_SERVICE_PATH  = "/axis2/services/EucalyptusCC";
  @Transient
  private static String         INSECURE_SERVICE_PATH = "/axis2/services/EucalyptusGL";
  
  @Transient
  @ConfigurableIdentifier
  private String                propertyPrefix;
  
  @Column( name = "cluster_network_mode" )
  @ConfigurableField( description = "Currently configured network mode", displayName = "Network mode", readonly = true )
  private/*NetworkMode*/String networkMode;
  
  @Column( name = "cluster_use_network_tags" )
  @ConfigurableField( description = "Indicates whether vlans are in use or not.", displayName = "Uses vlans", readonly = true )
  private Boolean               useNetworkTags;
  
  @ConfigurableField( description = "Minimum vlan tag to use (0 < x < max_vlan <= 4096)", displayName = "Min vlan", readonly = true )
  @Column( name = "cluster_min_network_tag" )
  private Integer               minNetworkTag;
  
  @ConfigurableField( description = "Maximum vlan tag to use (0 < min_vlan < x < 4096)", displayName = "Max vlan", readonly = true )
  @Column( name = "cluster_max_network_tag" )
  private Integer               maxNetworkTag;
  
  @ConfigurableField( description = "Maximum usable network index (0 < min_network_index < x)", displayName = "Max network index", readonly = true )
  @Column( name = "cluster_min_addr" )
  private Long                  minNetworkIndex;
  
  @ConfigurableField( description = "Maximum usable network index (0 < x < max_network_index)", displayName = "Min network index", readonly = true )
  @Column( name = "cluster_min_vlan" )
  private Long                  maxNetworkIndex;
  
  @ConfigurableField( description = "Number of total addresses per network (including unusable gateway addresses controlled by the system)", displayName = "Addresses per network (ADDRS_PER_NET)", readonly = true )
  @Column( name = "cluster_addrs_per_net" )
  private Integer               addressesPerNetwork;
  
  @ConfigurableField( description = "IP subnet used by the cluster's virtual private networking.", displayName = "Virtual network subnet (VNET_SUBNET)", readonly = true )
  @Column( name = "cluster_vnet_subnet" )
  private String                vnetSubnet;
  
  @ConfigurableField( description = "Netmask used by the cluster's virtual private networking.", displayName = "Virtual network netmask (VNET_NETMASK)", readonly = true )
  @Column( name = "cluster_vnet_netmask" )
  private String                vnetNetmask;
  
  @ConfigurableField( description = "IP version used by the cluster's virtual private networking.", displayName = "Virtual network IP version", readonly = true )
  @Column( name = "cluster_vnet_type" )
  private String                vnetType              = "ipv4";

  @ConfigurableField( description = "Alternative address which is the source address for requests made by the component to the cloud controller.", displayName = "Source host name" )
  @Column( name = "cluster_alt_source_hostname" )
  private String            sourceHostName;

  public ClusterConfiguration( ) {}
  
  public ClusterConfiguration( String partition, String name, String hostName, Integer port ) {
    super( partition, name, hostName, port, DEFAULT_SERVICE_PATH );
    this.sourceHostName = hostName;
  }
  
  public ClusterConfiguration( String partition, String name, String hostName, Integer port, Integer minVlan, Integer maxVlan ) {
    super( partition, name, hostName, port, DEFAULT_SERVICE_PATH );
    this.minNetworkTag = minVlan;
    this.maxNetworkTag = maxVlan;
    this.sourceHostName = hostName;
  }
  
  @PostLoad
  private void initOnLoad( ) {//GRZE:HACK:HACK: needed to mark field as @ConfigurableIdentifier
    if ( this.propertyPrefix == null ) {
      this.propertyPrefix = this.getPartition( ).replace( ".", "" ) + "." + this.getName( );
    }
  }
  
  @PrePersist
  private void defaultsOnCommit( ) {
    if ( this.useNetworkTags == null ) {
      this.useNetworkTags = Boolean.TRUE;
    }
    if ( this.minNetworkIndex == null ) {
      this.minNetworkIndex = NetworkGroups.networkingConfiguration( ).getMinNetworkIndex( );
    }
    if ( this.maxNetworkIndex == null ) {
      this.maxNetworkIndex = NetworkGroups.networkingConfiguration( ).getMaxNetworkIndex( );
    }
    if ( this.minNetworkTag == null ) {
      this.minNetworkTag = NetworkGroups.networkingConfiguration( ).getMinNetworkTag( );
    }
    if ( this.maxNetworkTag == null ) {
      this.maxNetworkTag = NetworkGroups.networkingConfiguration( ).getMaxNetworkTag( );
    }
  }
  
  public String getInsecureServicePath( ) {
    return INSECURE_SERVICE_PATH;
  }
  
  public String getInsecureUri( ) {
    return "http://" + this.getHostName( ) + ":" + this.getPort( ) + INSECURE_SERVICE_PATH;
  }
  
  @Override
  public Boolean isVmLocal( ) {
    return false;
  }
  
  @Override
  public Boolean isHostLocal( ) {
    return BootstrapArgs.isCloudController( );
  }
  
  public String getNetworkMode( ) {
    return this.networkMode;
  }
  
  public void setNetworkMode( String networkMode ) {
    this.networkMode = networkMode;
  }
  
  public Boolean getUseNetworkTags( ) {
    return this.useNetworkTags;
  }
  
  public void setUseNetworkTags( Boolean useNetworkTags ) {
    this.useNetworkTags = useNetworkTags;
  }
  
  public Integer getMinNetworkTag( ) {
    return this.minNetworkTag;
  }
  
  public void setMinNetworkTag( Integer minNetworkTag ) {
    this.minNetworkTag = minNetworkTag;
  }
  
  public Integer getMaxNetworkTag( ) {
    return this.maxNetworkTag;
  }
  
  public void setMaxNetworkTag( Integer maxNetworkTag ) {
    this.maxNetworkTag = maxNetworkTag;
  }
  
  public Long getMinNetworkIndex( ) {
    return this.minNetworkIndex;
  }
  
  public void setMinNetworkIndex( Long minNetworkIndex ) {
    this.minNetworkIndex = minNetworkIndex;
  }
  
  public Long getMaxNetworkIndex( ) {
    return this.maxNetworkIndex;
  }
  
  public void setMaxNetworkIndex( Long maxNetworkIndex ) {
    this.maxNetworkIndex = maxNetworkIndex;
  }
  
  public Integer getAddressesPerNetwork( ) {
    return this.addressesPerNetwork;
  }
  
  public void setAddressesPerNetwork( Integer addressesPerNetwork ) {
    this.addressesPerNetwork = addressesPerNetwork;
  }
  
  public String getVnetSubnet( ) {
    return this.vnetSubnet;
  }
  
  public void setVnetSubnet( String vnetSubnet ) {
    this.vnetSubnet = vnetSubnet;
  }
  
  public String getVnetNetmask( ) {
    return this.vnetNetmask;
  }
  
  public void setVnetNetmask( String vnetNetmask ) {
    this.vnetNetmask = vnetNetmask;
  }
  
  public String getVnetType( ) {
    return this.vnetType;
  }
  
  public void setVnetType( String vnetType ) {
    this.vnetType = vnetType;
  }
  
  public String getPropertyPrefix( ) {
    return this.propertyPrefix;
  }
  
  public void setPropertyPrefix( String propertyPrefix ) {
    this.propertyPrefix = propertyPrefix;
  }

  public String getSourceHostName( ) {
    return this.sourceHostName;
  }

  public void setSourceHostName( String aliasHostName ) {
    this.sourceHostName = aliasHostName;
  }
}
