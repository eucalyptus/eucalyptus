/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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

package com.eucalyptus.compute.common;

import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.type.RestrictedType;

/**
 * GRZE:WARN: values are intentionally opaque strings and /not/ a symbolic reference. do not change
 * them.
 * NOTE: this class can be safely treated as a public type; as opposed to the implementation
 * specific types which should /not/ be references.
 * 
 * @see PolicyResourceType
 * @see PolicyVendor
 **/
@PolicyVendor( "ec2" )
public interface CloudMetadata extends RestrictedType {
  
  @PolicyResourceType( "availabilityzone" )
  interface AvailabilityZoneMetadata extends CloudMetadata {}
  
  @PolicyResourceType( "key-pair" )
  interface KeyPairMetadata extends CloudMetadata {}
  
  @PolicyResourceType( "security-group" )
  interface NetworkGroupMetadata extends CloudMetadata {}
  
  @PolicyResourceType( "address" )
  interface AddressMetadata extends CloudMetadata {}
  
  @PolicyResourceType( "volume" )
  @CloudMetadataLongIdentifierConfigurable( prefix = "vol" )
  interface VolumeMetadata extends CloudMetadata {}
  
  @PolicyResourceType( "snapshot" )
  @CloudMetadataLongIdentifierConfigurable( prefix = "snap" )
  interface SnapshotMetadata extends CloudMetadata {
  }
  
  @PolicyResourceType( VmInstanceMetadata.POLICY_RESOURCE_TYPE )
  @CloudMetadataLongIdentifierConfigurable( prefix = "i", relatedPrefixes = "r" )
  interface VmInstanceMetadata extends CloudMetadata {
    String POLICY_RESOURCE_TYPE = "instance";
  }

  @PolicyResourceType( "vmtype" )
  interface VmTypeMetadata extends CloudMetadata {
    Integer getMemory( );
    
    Integer getCpu( );
    
    Integer getDisk( );
    
  }

  @PolicyResourceType( "tag" )
  interface TagMetadata extends CloudMetadata {}

  @PolicyResourceType( "dhcp-options" )
  interface DhcpOptionSetMetadata extends CloudMetadata {}

  @PolicyResourceType( "internet-gateway" )
  interface InternetGatewayMetadata extends CloudMetadata {}

  @PolicyResourceType( "nat-gateway" )
  interface NatGatewayMetadata extends CloudMetadata {}

  @PolicyResourceType( "network-acl" )
  interface NetworkAclMetadata extends CloudMetadata {}

  @PolicyResourceType( "network-interface" )
  interface NetworkInterfaceMetadata extends CloudMetadata {}

  @PolicyResourceType( "route-table" )
  interface RouteTableMetadata extends CloudMetadata {}

  @PolicyResourceType( "subnet" )
  interface SubnetMetadata extends CloudMetadata {}

  @PolicyResourceType( "vpc" )
  interface VpcMetadata extends CloudMetadata {}
}
