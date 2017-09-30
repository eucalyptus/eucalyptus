/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.compute.common;

import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class VpcType extends EucalyptusData implements VpcTagged {

  private String vpcId;
  private String state;
  private String cidrBlock;
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
