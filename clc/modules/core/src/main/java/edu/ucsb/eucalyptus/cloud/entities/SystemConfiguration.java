/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 * Author: Sunil Soman sunils@cs.ucsb.edu
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import java.net.SocketException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.entities.AbstractStatefulPersistent;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.images.ForwardImages;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.NetworkUtil;
import com.eucalyptus.util.StorageProperties;

@Entity
@PersistenceContext( name = "eucalyptus_general" )
@Table( name = "system_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass( root = "config", description = "Basic system configuration." )
public class SystemConfiguration {
  private static Logger LOG = Logger.getLogger( SystemConfiguration.class );
  @Id
  @GeneratedValue
  @Column( name = "system_info_id" )
  private Long    id = -1l;
  @ConfigurableField( description = "Hostname of the cloud controller." )
  @Column( name = "system_info_cloud_host" )
  private String  cloudHost;
  @ConfigurableField( description = "Default kernel to use when none is supplied by an image's manifest or the user at runtime." )
  @Column( name = "system_info_default_kernel" )
  private String  defaultKernel;
  @ConfigurableField( description = "Default ramdisk to use when none is supplied by an image's manifest or the user at runtime." )
  @Column( name = "system_info_default_ramdisk" )
  private String  defaultRamdisk;
  @ConfigurableField( description = "Unique ID of this cloud installation.", readonly = false )
  @Column( name = "system_registration_id" )
  private String  registrationId;
  @Column( name = "system_max_user_public_addresses" )
  private Integer maxUserPublicAddresses;
  @Column( name = "system_do_dynamic_public_addresses" )
  private Boolean doDynamicPublicAddresses;
  @Column( name = "system_reserved_public_addresses" )
  private Integer systemReservedPublicAddresses;
  @ConfigurableField( description = "Domain name to use for DNS." )
  @Column( name = "dns_domain" )
  private String  dnsDomain;
  @ConfigurableField( description = "Nameserver address." )
  @Column( name = "nameserver" )
  private String  nameserver;
  @Column( name = "ns_address" )
  private String  nameserverAddress;

  public SystemConfiguration( ) {
  }

  public SystemConfiguration( final String defaultKernel, final String defaultRamdisk, final Integer maxUserPublicAddresses,
                              final Boolean doDynamicPublicAddresses, final Integer systemReservedPublicAddresses,
                              final String dnsDomain, final String nameserver, final String nameserverAddress, final String cloudHost ) {
    this.defaultKernel = defaultKernel;
    this.defaultRamdisk = defaultRamdisk;
    this.maxUserPublicAddresses = maxUserPublicAddresses;
    this.doDynamicPublicAddresses = doDynamicPublicAddresses;
    this.systemReservedPublicAddresses = systemReservedPublicAddresses;
    this.dnsDomain = dnsDomain;
    this.nameserver = nameserver;
    this.nameserverAddress = nameserverAddress;
    this.cloudHost = cloudHost;
  }
  
  public Long getId( ) {
    return id;
  }
  
  public String getDefaultKernel( ) {
    return defaultKernel;
  }
  
  public String getDefaultRamdisk( ) {
    return defaultRamdisk;
  }
  
  public void setDefaultKernel( final String defaultKernel ) {
    this.defaultKernel = defaultKernel;
  }
  
  public void setDefaultRamdisk( final String defaultRamdisk ) {
    this.defaultRamdisk = defaultRamdisk;
  }
  
  public String getRegistrationId( ) {
    return registrationId;
  }
  
  public void setRegistrationId( final String registrationId ) {
    this.registrationId = registrationId;
  }
  
  public Integer getMaxUserPublicAddresses( ) {
    return maxUserPublicAddresses;
  }
  
  public void setMaxUserPublicAddresses( final Integer maxUserPublicAddresses ) {
    this.maxUserPublicAddresses = maxUserPublicAddresses;
  }
  
  public Integer getSystemReservedPublicAddresses( ) {
    return systemReservedPublicAddresses;
  }
  
  public void setSystemReservedPublicAddresses( final Integer systemReservedPublicAddresses ) {
    this.systemReservedPublicAddresses = systemReservedPublicAddresses;
  }
  
  public Boolean isDoDynamicPublicAddresses( ) {
    return doDynamicPublicAddresses;
  }
  
  public void setDoDynamicPublicAddresses( final Boolean doDynamicPublicAddresses ) {
    this.doDynamicPublicAddresses = doDynamicPublicAddresses;
  }
  
  public String getDnsDomain( ) {
    return dnsDomain;
  }
  
  public void setDnsDomain( String dnsDomain ) {
    this.dnsDomain = dnsDomain;
  }
  
  public String getNameserver( ) {
    return nameserver;
  }
  
  public void setNameserver( String nameserver ) {
    this.nameserver = nameserver;
  }
  
  public String getNameserverAddress( ) {
    return nameserverAddress;
  }
  
  public void setNameserverAddress( String nameserverAddress ) {
    this.nameserverAddress = nameserverAddress;
  }
  
  public String getCloudHost( ) {
    return cloudHost;
  }
  
  public void setCloudHost( String cloudHost ) {
    this.cloudHost = cloudHost;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( cloudHost == null ) ? 0 : cloudHost.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    SystemConfiguration other = ( SystemConfiguration ) obj;
    if ( cloudHost == null ) {
      if ( other.cloudHost != null ) return false;
    } else if ( !cloudHost.equals( other.cloudHost ) ) return false;
    return true;
  }

  public static SystemConfiguration getSystemConfiguration() {
  	EntityWrapper<SystemConfiguration> confDb = EntityWrapper.get( SystemConfiguration.class );
  	SystemConfiguration conf = null;
  	try {
  		conf = confDb.getUnique( new SystemConfiguration());
  		SystemConfiguration.validateSystemConfiguration(conf);
  		confDb.commit();
  	}
  	catch ( EucalyptusCloudException e ) {
  	  LOG.warn("Failed to get system configuration. Loading defaults.");
  		conf = SystemConfiguration.validateSystemConfiguration(null);
  		confDb.add(conf);
  		confDb.commit();
  	}
  	catch (Throwable t) {
  		LOG.error("Unable to get system configuration.");
  		confDb.rollback();
  		return validateSystemConfiguration(null);
  	}
  	return conf;
  }

  public static String getCloudUrl() {
    try {
      String cloudHost = SystemConfiguration.getSystemConfiguration( ).getCloudHost( );
      if( cloudHost == null ) {
        for( WalrusConfiguration w : ServiceConfigurations.getConfigurations( WalrusConfiguration.class ) ) {
          if( NetworkUtil.testLocal( w.getHostName( ) ) ) {
            cloudHost = w.getHostName( );
            break;
          }
        }
      }
      if( cloudHost == null ) {
        try {
          cloudHost = NetworkUtil.getAllAddresses( ).get( 0 );
        } catch ( SocketException e ) {}
      }
      return String.format( "http://%s:"+System.getProperty("euca.ws.port")+"/services/Eucalyptus", cloudHost );
    } catch ( PersistenceException e ) {
      return "http://127.0.0.1:8773/services/Eucalyptus";
    }
  }

  public static String getWalrusUrl() throws EucalyptusCloudException {
    String walrusHost = null;
    try {
      walrusHost = ServiceConfigurations.getConfiguration( WalrusConfiguration.class, ComponentIds.lookup(Walrus.class).name( ) ).getHostName( );
    } catch ( Throwable e ) {
      try {
        walrusHost = ServiceConfigurations.getConfiguration( WalrusConfiguration.class, "Walrus" ).getHostName( );
      } catch ( Throwable ex ) {
      }
    }
    return String.format( "http://%s:8773/services/Walrus", walrusHost == null ? "127.0.0.1" : walrusHost );
  }

  public static String getCloudHostAddress( ) {
    String cloudHost = null;
    try {
      cloudHost = SystemConfiguration.getSystemConfiguration( ).getCloudHost( );
      if( cloudHost == null ) {
        for( WalrusConfiguration w : ServiceConfigurations.getConfigurations( WalrusConfiguration.class ) ) {
          if( NetworkUtil.testLocal( w.getHostName( ) ) ) {
            cloudHost = w.getHostName( );
            break;
          }
        }
      }
    } catch ( PersistenceException e ) {
    }
    if( cloudHost == null ) {
      try {
        cloudHost = NetworkUtil.getAllAddresses( ).get( 0 );
      } catch ( SocketException e ) {}
    }
    return cloudHost;
  }

  public static String getInternalIpAddress ()
  {
    String ipAddr = null;
    try {
      for( String addr : NetworkUtil.getAllAddresses( ) ) {
        ipAddr = addr;
        break;
      }
    } catch ( SocketException e ) {}
    return ipAddr == null ? "127.0.0.1" : ipAddr;
  }

  private static SystemConfiguration validateSystemConfiguration(SystemConfiguration s) {
    SystemConfiguration sysConf = s != null ? s : new SystemConfiguration(); 
    if( sysConf.getRegistrationId() == null ) {
      sysConf.setRegistrationId( UUID.randomUUID().toString() );
    }
    if(sysConf.getCloudHost() == null) {
      String ipAddr = SystemConfiguration.getInternalIpAddress ();
      sysConf.setCloudHost(ipAddr);
    }
    if(sysConf.getDefaultKernel() == null) {
      sysConf.setDefaultRamdisk( ForwardImages.defaultKernel( ) );//TODO:GRZE:ASAP this semantic no longer makes any sense.  fix it.
    }
    if(sysConf.getDefaultRamdisk() == null) {
      sysConf.setDefaultRamdisk( ForwardImages.defaultRamdisk( ) );//TODO:GRZE:ASAP this semantic no longer makes any sense.  fix it.
    }
    if(sysConf.getDnsDomain() == null) {
      sysConf.setDnsDomain(DNSProperties.DOMAIN);
    }
    if(sysConf.getNameserver() == null) {
      sysConf.setNameserver(DNSProperties.NS_HOST);
    }
    if(sysConf.getNameserverAddress() == null) {
      sysConf.setNameserverAddress(DNSProperties.NS_IP);
    }
    if( sysConf.getMaxUserPublicAddresses() == null ) {
      sysConf.setMaxUserPublicAddresses( 5 );
    }
    if( sysConf.isDoDynamicPublicAddresses() == null ) {
      sysConf.setDoDynamicPublicAddresses( true );
    }
    if( sysConf.getSystemReservedPublicAddresses() == null ) {
      sysConf.setSystemReservedPublicAddresses( 10 );
    }
    return sysConf;
  }

}
