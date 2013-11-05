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

@GroovyAddClassUUID
package edu.ucsb.eucalyptus.msgs

public class CloudClusterMessage extends EucalyptusMessage {
  
  public CloudClusterMessage( ) {
    super( );
  }
  
  public CloudClusterMessage( EucalyptusMessage msg ) {
    super( msg );
  }
  
  public CloudClusterMessage( String userId ) {
    super( userId );
  }
}
public class StartNetworkType extends CloudClusterMessage {
  String networkUuid;
  int vlan;
  String netName;
  String nameserver;
  ArrayList<String> clusterControllers = new ArrayList<String>();
  String accountId;
  
  
  public StartNetworkType(){
  }
  
  public StartNetworkType(final String accountId, final String userId, final Integer vlan, final String netName, final String networkUuid) {
    super( userId );
    this.networkUuid = networkUuid;
    this.vlan = vlan;
    this.netName = netName;
  }
}

public class StartNetworkResponseType extends CloudClusterMessage {
}
public class StopNetworkType extends CloudClusterMessage {
  Integer vlan;
  String netName;
  String accountId;
  
  public StopNetworkType(){
  }
  
  public StopNetworkType(final String accountId, final String userId, final String netName, final Integer vlan) {
    super( userId );
    this.vlan = vlan;
    this.netName = netName;
    this.accountId = accountId;
  }
  
  public StopNetworkType(final StartNetworkType msg) {
    super(msg);
    this.vlan = msg.vlan;
    this.netName = msg.netName;
  }
}

public class StopNetworkResponseType extends CloudClusterMessage {
}

public class DescribeNetworksType extends CloudClusterMessage {
  String nameserver;
  String dnsDomainName;
  ArrayList<String> clusterControllers = new ArrayList<String>();
}

public class DescribeNetworksResponseType extends CloudClusterMessage {
  Integer useVlans;
  String mode;
  Integer addrIndexMin;
  Integer addrIndexMax;
  Integer vlanMin;
  Integer vlanMax;
  String vnetSubnet;
  String vnetNetmask;
  Integer addrsPerNet;
  ArrayList<NetworkInfoType> activeNetworks = new ArrayList<NetworkInfoType>();
  
  public String toString() {
    return "${this.getClass().getSimpleName()} mode=${mode} addrsPerNet=${addrsPerNet} " \
      + "\n${this.getClass().getSimpleName()} " + this.activeNetworks*.toString().join( "\n${this.getClass().getSimpleName()} " );
  }
}
public class AddressMappingInfoType extends EucalyptusData {
  String uuid;
  String source;
  String destination;
}

public class NetworkInfoType extends EucalyptusData {
  String uuid;
  Integer tag;
  String networkName;
  String accountNumber;
  ArrayList<String> allocatedIndexes = new ArrayList<String>();
  public String toString( ) {
    return "NetworkInfoType ${accountNumber} ${networkName} ${uuid} ${tag} ${allocatedIndexes}";
  }
}

public class ClusterAddressInfo extends EucalyptusData implements Comparable<ClusterAddressInfo> {
  String uuid;
  String address;
  String instanceIp;
  
  public ClusterAddressInfo( String address ) {
    this.address = address;
  }
  
  public boolean hasMapping() {
    return this.instanceIp != null &&  !"".equals( this.instanceIp ) && !"0.0.0.0".equals( this.instanceIp );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.address == null ) ? 0 : this.address.hashCode( ) );
    return result;
  }
  
  public int compareTo( ClusterAddressInfo that ) {
    return this.address.compareTo( that.address );
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) return true;
    if ( obj == null ) return false;
    if ( !getClass( ).equals( obj.getClass( ) ) ) return false;
    ClusterAddressInfo other = ( ClusterAddressInfo ) obj;
    if ( this.address == null ) {
      if ( other.address != null ) return false;
    } else if ( !this.address.equals( other.address ) ) return false;
    return true;
  }
  
  public String toString( ) {
    return String.format( "ClusterAddressInfo %s %s", this.address, this.instanceIp );
  }
}



public class AssignAddressType extends CloudClusterMessage {
  String uuid;
  String instanceId;
  String source;
  String destination;
  def AssignAddressType(final String uuid, final String source, final String destination, final String instanceId ) {
    this.uuid = uuid;
    this.source = source;
    this.destination = destination;
    this.instanceId = instanceId;
  }
  
  def AssignAddressType(final BaseMessage msg, final String uuid, final String source, final String destination, final String instanceId) {
    super(msg);
    this.uuid = uuid;
    this.source = source;
    this.destination = destination;
    this.instanceId = instanceId;
  }
  
  def AssignAddressType() {
  }

  @Override
  public String toString( ) {
    return "AssignAddress ${this.source}=>${this.destination} ${this.instanceId} ${this.uuid}";
  }

  @Override
  public String toSimpleString( ) {
    return "${super.toSimpleString( )} ${this.source}=>${this.destination} ${this.instanceId} ${this.uuid}";
  }
  
  
}
public class AssignAddressResponseType extends CloudClusterMessage {
}
public class UnassignAddressType extends CloudClusterMessage {
  
  String source;
  String destination;
  
  def UnassignAddressType(final msg, final source, final destination) {
    super(msg);
    this.source = source;
    this.destination = destination;
  }
  def UnassignAddressType(final source, final destination) {
    
    this.source = source;
    this.destination = destination;
  }
  
  
  def UnassignAddressType() {
  }

  @Override
  public String toString( ) {
    return "UnassignAddress ${this.source}=>${this.destination}";
  }

  @Override
  public String toSimpleString( ) {
    return "${super.toSimpleString( )} ${this.source}=>${this.destination}";
  }
}
public class UnassignAddressResponseType extends CloudClusterMessage {
}
public class DescribePublicAddressesType extends CloudClusterMessage {
}
public class DescribePublicAddressesResponseType extends CloudClusterMessage {
  ArrayList<ClusterAddressInfo> addresses = new ArrayList<ClusterAddressInfo>();
  public String toString() {
    return "${this.getClass().getSimpleName()} " + addresses.each{ it -> "${it}" }.join("\n${this.getClass().getSimpleName()} ");
  }
}

public class ConfigureNetworkType extends CloudClusterMessage {
  
  ArrayList<PacketFilterRule> rules = new ArrayList<PacketFilterRule>();
  
  def ConfigureNetworkType(final EucalyptusMessage msg, final ArrayList<PacketFilterRule> rules) {
    super(msg);
    this.rules = rules;
  }
  
  def ConfigureNetworkType(final ArrayList<PacketFilterRule> rules) {
    this.rules = rules;
  }
  
  def ConfigureNetworkType(){
  }
}
public class ConfigureNetworkResponseType extends CloudClusterMessage {
}
