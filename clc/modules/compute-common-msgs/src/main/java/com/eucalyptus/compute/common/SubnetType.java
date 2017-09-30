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

public class SubnetType extends EucalyptusData implements VpcTagged {

  private String subnetId;
  private String state;
  private String vpcId;
  private String cidrBlock;
  private Integer availableIpAddressCount;
  private String availabilityZone;
  private Boolean defaultForAz;
  private Boolean mapPublicIpOnLaunch;
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

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }
}
