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

package edu.ucsb.eucalyptus.cloud.entities;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;

@Entity
@PersistenceContext( name = "eucalyptus_general" )
@Table( name = "system_info" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@ConfigurableClass( root = "system.dns", description = "Basic system configuration." )
@Deprecated  //GRZE: this class will FINALLY be superceded by new DNS support in 3.4: DO NOT USE IT!
public class SystemConfiguration extends AbstractPersistent {
  private static Logger LOG = Logger.getLogger( SystemConfiguration.class );
  @ConfigurableField( description = "Unique ID of this cloud installation.", readonly = false )
  @Column( name = "system_registration_id" )
  private String  registrationId;
  @Deprecated  //GRZE: this class will FINALLY be superceded by new DNS support in 3.4: DO NOT USE IT!
  @ConfigurableField( description = "Domain name to use for DNS." )
  @Column( name = "dns_domain" )
  private String  dnsDomain;
  @Deprecated  //GRZE: this class will FINALLY be superceded by new DNS support in 3.4: DO NOT USE IT!
  @ConfigurableField( description = "Nameserver hostname." )
  @Column( name = "nameserver" )
  private String  nameserver;
  @Deprecated  //GRZE: this class will FINALLY be superceded by new DNS support in 3.4: DO NOT USE IT!
  @ConfigurableField( description = "Nameserver ip address." )
  @Column( name = "ns_address" )
  private String  nameserverAddress;

  public SystemConfiguration( ) {
  }

  public SystemConfiguration( final String dnsDomain, final String nameserver, final String nameserverAddress ) {
    this.dnsDomain = dnsDomain;
    this.nameserver = nameserver;
    this.nameserverAddress = nameserverAddress;
  }
  
  public String getRegistrationId( ) {
    return registrationId;
  }
  
  public void setRegistrationId( final String registrationId ) {
    this.registrationId = registrationId;
  }
    
  @Deprecated  //GRZE: this class will FINALLY be superceded by new DNS support in 3.4: DO NOT USE IT!
  public String getDnsDomain( ) {
    return dnsDomain;
  }
  
  @Deprecated  //GRZE: this class will FINALLY be superceded by new DNS support in 3.4: DO NOT USE IT!
  public void setDnsDomain( String dnsDomain ) {
    this.dnsDomain = dnsDomain;
  }
  
  @Deprecated  //GRZE: this class will FINALLY be superceded by new DNS support in 3.4: DO NOT USE IT!
  public String getNameserver( ) {
    return nameserver;
  }
  
  @Deprecated  //GRZE: this class will FINALLY be superceded by new DNS support in 3.4: DO NOT USE IT!
  public void setNameserver( String nameserver ) {
    this.nameserver = nameserver;
  }
  
  @Deprecated  //GRZE: this class will FINALLY be superceded by new DNS support in 3.4: DO NOT USE IT!
  public String getNameserverAddress( ) {
    return nameserverAddress;
  }
  
  @Deprecated  //GRZE: this class will FINALLY be superceded by new DNS support in 3.4: DO NOT USE IT!
  public void setNameserverAddress( String nameserverAddress ) {
    this.nameserverAddress = nameserverAddress;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( registrationId == null ) ? 0 : registrationId.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    SystemConfiguration other = ( SystemConfiguration ) obj;
    if ( registrationId == null ) {
      if ( other.registrationId != null ) return false;
    } else if ( !registrationId.equals( other.registrationId ) ) return false;
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
  	catch (Exception t) {
  		LOG.error("Unable to get system configuration.");
  		confDb.rollback();
  		return validateSystemConfiguration(null);
  	}
  	return conf;
  }

  /*public static String getWalrusUrl() throws EucalyptusCloudException {
    Component walrus = Components.lookup( Walrus.class );
    if( Topology.isEnabled( Walrus.class ) ) {
      ServiceConfiguration walrusConfig = Topology.lookup( Walrus.class );
      return walrusConfig.getUri( ).toASCIIString( );
    } else {
      LOG.error( "BUG BUG: Deprecated method called. No walrus service is registered.  Using local address for walrus URL." );
      return ServiceUris.remote( walrus ).toASCIIString( );
    }
  }*/

  private static SystemConfiguration validateSystemConfiguration(SystemConfiguration s) {
    SystemConfiguration sysConf = s != null ? s : new SystemConfiguration(); 
    if( sysConf.getRegistrationId() == null ) {
      sysConf.setRegistrationId( UUID.randomUUID().toString() );
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
    return sysConf;
  }

}
