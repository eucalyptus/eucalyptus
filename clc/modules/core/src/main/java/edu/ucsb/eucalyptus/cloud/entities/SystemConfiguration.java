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
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table( name = "system_info" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class SystemConfiguration {
  @Id
  @GeneratedValue
  @Column( name = "system_info_id" )
  private Long    id = -1l;
  @Column( name = "system_info_cloud_host" )
  private String  cloudHost;
  @Column( name = "system_info_default_kernel" )
  private String  defaultKernel;
  @Column( name = "system_info_default_ramdisk" )
  private String  defaultRamdisk;
  @Column( name = "system_registration_id" )
  private String  registrationId;
  @Column( name = "system_max_user_public_addresses" )
  private Integer maxUserPublicAddresses;
  @Column( name = "system_do_dynamic_public_addresses" )
  private Boolean doDynamicPublicAddresses;
  @Column( name = "system_reserved_public_addresses" )
  private Integer systemReservedPublicAddresses;
  @Column( name = "zero_fill_volumes" )
  private Boolean zeroFillVolumes;
  @Column( name = "dns_domain" )
  private String  dnsDomain;
  @Column( name = "nameserver" )
  private String  nameserver;
  @Column( name = "ns_address" )
  private String  nameserverAddress;

  public SystemConfiguration( ) {
  }

  public SystemConfiguration( final String defaultKernel, final String defaultRamdisk, final Integer maxUserPublicAddresses, final Boolean doDynamicPublicAddresses, final Integer systemReservedPublicAddresses, final Boolean zeroFillVolumes, final String dnsDomain, final String nameserver,
      final String nameserverAddress, final String cloudHost ) {
    this.defaultKernel = defaultKernel;
    this.defaultRamdisk = defaultRamdisk;
    this.maxUserPublicAddresses = maxUserPublicAddresses;
    this.doDynamicPublicAddresses = doDynamicPublicAddresses;
    this.systemReservedPublicAddresses = systemReservedPublicAddresses;
    this.dnsDomain = dnsDomain;
    this.zeroFillVolumes = zeroFillVolumes;
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

  public Boolean getZeroFillVolumes( ) {
    return zeroFillVolumes;
  }

  public void setZeroFillVolumes( Boolean zeroFillVolumes ) {
    this.zeroFillVolumes = zeroFillVolumes;
  }

  public String getCloudHost( ) {
    return cloudHost;
  }

  public void setCloudHost( String cloudHost ) {
    this.cloudHost = cloudHost;
  }

@Override
public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((cloudHost == null) ? 0 : cloudHost.hashCode());
	return result;
}

@Override
public boolean equals(Object obj) {
	if (this == obj)
		return true;
	if (obj == null)
		return false;
	if (getClass() != obj.getClass())
		return false;
	SystemConfiguration other = (SystemConfiguration) obj;
	if (cloudHost == null) {
		if (other.cloudHost != null)
			return false;
	} else if (!cloudHost.equals(other.cloudHost))
		return false;
	return true;
}

}
