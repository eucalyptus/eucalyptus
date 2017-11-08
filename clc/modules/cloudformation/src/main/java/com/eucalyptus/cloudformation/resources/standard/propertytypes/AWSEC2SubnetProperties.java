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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AWSEC2SubnetProperties implements ResourceProperties {

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "availabilityZone", availabilityZone )
        .add( "cidrBlock", cidrBlock )
        .add( "mapPublicIpOnLaunch", mapPublicIpOnLaunch )
        .add( "tags", tags )
        .add( "vpcId", vpcId )
        .toString( );
  }

  @Property
  private String availabilityZone;

  @Required
  @Property
  private String cidrBlock;

  @Property
  private Boolean mapPublicIpOnLaunch;

  @Property
  private ArrayList<EC2Tag> tags = Lists.newArrayList( );

  @Required
  @Property
  private String vpcId;

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public String getCidrBlock( ) {
    return cidrBlock;
  }

  public void setCidrBlock( String cidrBlock ) {
    this.cidrBlock = cidrBlock;
  }

  public Boolean getMapPublicIpOnLaunch( ) {
    return mapPublicIpOnLaunch;
  }

  public void setMapPublicIpOnLaunch( Boolean mapPublicIpOnLaunch ) {
    this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
  }

  public ArrayList<EC2Tag> getTags( ) {
    return tags;
  }

  public void setTags( ArrayList<EC2Tag> tags ) {
    this.tags = tags;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }
}
