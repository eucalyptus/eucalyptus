/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.compute.common;

import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class SubnetType extends EucalyptusData implements ResourceTagged {

  private String subnetId;
  private String state;
  private String vpcId;
  private String cidrBlock;
  private Integer availableIpAddressCount;
  private String availabilityZone;
  private Boolean defaultForAz;
  private Boolean mapPublicIpOnLaunch;
  private Boolean mapCustomerOwnedIpOnLaunch;
  private Boolean assignIpv6AddressOnCreation;
  private ResourceTagSetType tagSet;

  public SubnetType( ) {
  }

  public SubnetType( final String subnetId, final String state, final String vpcId, final String cidrBlock, final Integer availableIpAddressCount, final String availabilityZone, final Boolean defaultForAz, final Boolean mapPublicIpOnLaunch ) {
    this.subnetId = subnetId;
    this.state = state;
    this.vpcId = vpcId;
    this.cidrBlock = cidrBlock;
    this.availableIpAddressCount = availableIpAddressCount;
    this.availabilityZone = availabilityZone;
    this.defaultForAz = defaultForAz;
    this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
    this.mapCustomerOwnedIpOnLaunch = false;
    this.assignIpv6AddressOnCreation = false;
  }

  public static CompatFunction<SubnetType, String> id( ) {
    return new CompatFunction<SubnetType, String>( ) {
      @Override
      public String apply( final SubnetType subnetType ) {
        return subnetType.getSubnetId( );
      }
    };
  }

  public static CompatFunction<SubnetType, String> zone( ) {
    return new CompatFunction<SubnetType, String>( ) {
      @Override
      public String apply( final SubnetType subnetType ) {
        return subnetType.getAvailabilityZone( );
      }
    };
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( String subnetId ) {
    this.subnetId = subnetId;
  }

  public String getState( ) {
    return state;
  }

  public void setState( String state ) {
    this.state = state;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }

  public String getCidrBlock( ) {
    return cidrBlock;
  }

  public void setCidrBlock( String cidrBlock ) {
    this.cidrBlock = cidrBlock;
  }

  public Integer getAvailableIpAddressCount( ) {
    return availableIpAddressCount;
  }

  public void setAvailableIpAddressCount( Integer availableIpAddressCount ) {
    this.availableIpAddressCount = availableIpAddressCount;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public Boolean getDefaultForAz( ) {
    return defaultForAz;
  }

  public void setDefaultForAz( Boolean defaultForAz ) {
    this.defaultForAz = defaultForAz;
  }

  public Boolean getMapPublicIpOnLaunch( ) {
    return mapPublicIpOnLaunch;
  }

  public void setMapPublicIpOnLaunch( Boolean mapPublicIpOnLaunch ) {
    this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
  }

  public Boolean getMapCustomerOwnedIpOnLaunch() {
    return mapCustomerOwnedIpOnLaunch;
  }

  public void setMapCustomerOwnedIpOnLaunch(Boolean mapCustomerOwnedIpOnLaunch) {
    this.mapCustomerOwnedIpOnLaunch = mapCustomerOwnedIpOnLaunch;
  }

  public Boolean getAssignIpv6AddressOnCreation() {
    return assignIpv6AddressOnCreation;
  }

  public void setAssignIpv6AddressOnCreation(Boolean assignIpv6AddressOnCreation) {
    this.assignIpv6AddressOnCreation = assignIpv6AddressOnCreation;
  }

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }
}
