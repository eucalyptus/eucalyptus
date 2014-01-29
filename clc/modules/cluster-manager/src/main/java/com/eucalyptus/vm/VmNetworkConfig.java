/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.vm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import org.hibernate.annotations.Parent;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

@Embeddable
public class VmNetworkConfig {
  
  @Parent
  private VmInstance   parent;
  @Column( name = "metadata_vm_private_addressing" )
  private Boolean      usePrivateAddressing;
  @Column( name = "metadata_vm_mac_address" )
  private String       macAddress;
  @Column( name = "metadata_vm_private_address" )
  private String       privateAddress;
  @Column( name = "metadata_vm_public_address" )
  private String       publicAddress;
  @Column( name = "metadata_vm_private_dns" )
  private String       privateDnsName;
  @Column( name = "metadata_vm_public_dns" )
  private String       publicDnsName;
  @Transient
  public static String DEFAULT_IP = "0.0.0.0";
  
  VmNetworkConfig( VmInstance parent, String ipAddress, String ignoredPublicIp ) {
    super( );
    this.usePrivateAddressing = false;
    this.parent = parent;
    this.privateAddress = ipAddress;
    this.publicAddress = ignoredPublicIp;
    this.updateDns( );
  }
  
  VmNetworkConfig( String ipAddress, String ignoredPublicIp ) {
    super( );
    this.privateAddress = ipAddress;
    this.publicAddress = ignoredPublicIp;
  }

  VmNetworkConfig( ) {
    super( );
  }
  
  /**
   * @param vmInstance
   */
  VmNetworkConfig( VmInstance vmInstance ) {
    this( vmInstance, DEFAULT_IP, DEFAULT_IP );
  }

  /**
   *
   */
  VmNetworkConfig( final VmInstance vmInstance, final Boolean usePrivateAddressing ) {
    this( vmInstance );
    this.usePrivateAddressing = usePrivateAddressing;
  }

  void updateDns( ) {
    String dnsDomain = null;
    try {
      dnsDomain = edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration.getSystemConfiguration( ).getDnsDomain( );
    } catch ( final Exception e ) {}
    dnsDomain = dnsDomain == null
      ? "dns-disabled"
      : dnsDomain;

    this.publicAddress = ipOrDefault( this.publicAddress );
    this.publicDnsName =  DEFAULT_IP.equals( publicAddress ) ?
        "" :
        "euca-" + this.publicAddress.replace( '.', '-' ) + VmInstances.INSTANCE_SUBDOMAIN + "." + dnsDomain;
    this.privateAddress = ipOrDefault( this.privateAddress );
    this.privateDnsName = DEFAULT_IP.equals( privateAddress ) ?
        "" :
        "euca-" + this.privateAddress.replace( '.', '-' ) + VmInstances.INSTANCE_SUBDOMAIN + ".internal";
  }
  
  private VmInstance getParent( ) {
    return this.parent;
  }
  
  void setParent( VmInstance parent ) {
    this.parent = parent;
  }

  Boolean getUsePrivateAddressing( ) {
    return this.usePrivateAddressing;
  }

  void setUsePrivateAddressing( Boolean usePrivateAddressing ) {
    this.usePrivateAddressing = usePrivateAddressing;
  }

  String getMacAddress( ) {
    return this.macAddress;
  }
  
  void setMacAddress( String macAddress ) {
    this.macAddress = macAddress;
  }
  
  String getPrivateAddress( ) {
    return this.privateAddress;
  }
  
  void setPrivateAddress( String privateAddress ) {
    this.privateAddress = privateAddress;
  }
  
  String getPublicAddress( ) {
    return this.publicAddress;
  }
  
  void setPublicAddress( String publicAddress ) {
    this.publicAddress = publicAddress;
  }
  
  String getPrivateDnsName( ) {
    return this.privateDnsName;
  }
  
  void setPrivateDnsName( String privateDnsName ) {
    this.privateDnsName = privateDnsName;
  }
  
  String getPublicDnsName( ) {
    return this.publicDnsName;
  }
  
  void setPublicDnsName( String publicDnsName ) {
    this.publicDnsName = publicDnsName;
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "VmNetworkConfig:" );
    if ( this.usePrivateAddressing != null ) builder.append( "usePrivateAddressing=" ).append( this.usePrivateAddressing ).append( ":" );
    if ( this.macAddress != null ) builder.append( "macAddress=" ).append( this.macAddress ).append( ":" );
    if ( this.privateAddress != null ) builder.append( "privateAddress=" ).append( this.privateAddress ).append( ":" );
    if ( this.publicAddress != null ) builder.append( "publicAddress=" ).append( this.publicAddress ).append( ":" );
    if ( this.privateDnsName != null ) builder.append( "privateDnsName=" ).append( this.privateDnsName ).append( ":" );
    if ( this.publicDnsName != null ) builder.append( "publicDnsName=" ).append( this.publicDnsName );
    return builder.toString( );
  }

  static String ipOrDefault( final String ip ) {
    return Objects.firstNonNull( Strings.emptyToNull( ip ), VmNetworkConfig.DEFAULT_IP );
  }

  /**
   * @param ip
   * @return
   */
  public static VmNetworkConfig exampleWithPrivateIp( String ip ) {
    return VmNetworkConfig.exampleWithIps( ip, null );
  }
  public static VmNetworkConfig exampleWithPublicIp( String ip ) {
    return VmNetworkConfig.exampleWithIps( null, ip );
  }
  public static VmNetworkConfig exampleWithIps( String privateIp, String publicIp ) {
    return new VmNetworkConfig( privateIp, publicIp );
  }
}
