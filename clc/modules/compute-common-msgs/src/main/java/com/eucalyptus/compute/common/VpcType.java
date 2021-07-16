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

public class VpcType extends EucalyptusData implements ResourceTagged {

  private String vpcId;
  private String state;
  private String cidrBlock;
  private String cidrBlockAssociationId;
  private String dhcpOptionsId;
  private ResourceTagSetType tagSet;
  private String instanceTenancy;
  private Boolean isDefault;

  public VpcType( ) {
  }

  public VpcType( final String vpcId, final String state, final String cidrBlock, final String dhcpOptionsId, final Boolean isDefault ) {
    this.vpcId = vpcId;
    this.state = state;
    this.cidrBlock = cidrBlock;
    this.cidrBlockAssociationId = vpcId == null ? null : vpcId.replace("vpc-", "vpc-cidr-assoc-");
    this.dhcpOptionsId = dhcpOptionsId;
    this.instanceTenancy = "default";
    this.isDefault = isDefault;
  }

  public static CompatFunction<VpcType, String> id( ) {
    return new CompatFunction<VpcType, String>( ) {
      @Override
      public String apply( final VpcType vpcType ) {
        return vpcType.getVpcId( );
      }
    };
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }

  public String getState( ) {
    return state;
  }

  public void setState( String state ) {
    this.state = state;
  }

  public String getCidrBlock( ) {
    return cidrBlock;
  }

  public void setCidrBlock( String cidrBlock ) {
    this.cidrBlock = cidrBlock;
  }

  public String getCidrBlockAssociationId() {
    return cidrBlockAssociationId;
  }

  public void setCidrBlockAssociationId(String cidrBlockAssociationId) {
    this.cidrBlockAssociationId = cidrBlockAssociationId;
  }

  public String getDhcpOptionsId( ) {
    return dhcpOptionsId;
  }

  public void setDhcpOptionsId( String dhcpOptionsId ) {
    this.dhcpOptionsId = dhcpOptionsId;
  }

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }

  public String getInstanceTenancy( ) {
    return instanceTenancy;
  }

  public void setInstanceTenancy( String instanceTenancy ) {
    this.instanceTenancy = instanceTenancy;
  }

  public Boolean getIsDefault( ) {
    return isDefault;
  }

  public void setIsDefault( Boolean isDefault ) {
    this.isDefault = isDefault;
  }
}
