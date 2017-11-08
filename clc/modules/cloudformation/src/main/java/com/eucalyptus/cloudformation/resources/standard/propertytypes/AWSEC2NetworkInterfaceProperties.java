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

public class AWSEC2NetworkInterfaceProperties implements ResourceProperties {

  @Property
  private String description;

  @Property
  private ArrayList<String> groupSet = Lists.newArrayList( );

  @Property
  private String privateIpAddress;

  @Property
  private ArrayList<PrivateIpAddressSpecification> privateIpAddresses = Lists.newArrayList( );

  @Property
  private Integer secondaryPrivateIpAddressCount;

  @Property
  private Boolean sourceDestCheck;

  @Required
  @Property
  private String subnetId;

  @Property
  private ArrayList<EC2Tag> tags = Lists.newArrayList( );

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public ArrayList<String> getGroupSet( ) {
    return groupSet;
  }

  public void setGroupSet( ArrayList<String> groupSet ) {
    this.groupSet = groupSet;
  }

  public String getPrivateIpAddress( ) {
    return privateIpAddress;
  }

  public void setPrivateIpAddress( String privateIpAddress ) {
    this.privateIpAddress = privateIpAddress;
  }

  public ArrayList<PrivateIpAddressSpecification> getPrivateIpAddresses( ) {
    return privateIpAddresses;
  }

  public void setPrivateIpAddresses( ArrayList<PrivateIpAddressSpecification> privateIpAddresses ) {
    this.privateIpAddresses = privateIpAddresses;
  }

  public Integer getSecondaryPrivateIpAddressCount( ) {
    return secondaryPrivateIpAddressCount;
  }

  public void setSecondaryPrivateIpAddressCount( Integer secondaryPrivateIpAddressCount ) {
    this.secondaryPrivateIpAddressCount = secondaryPrivateIpAddressCount;
  }

  public Boolean getSourceDestCheck( ) {
    return sourceDestCheck;
  }

  public void setSourceDestCheck( Boolean sourceDestCheck ) {
    this.sourceDestCheck = sourceDestCheck;
  }

  public String getSubnetId( ) {
    return subnetId;
  }

  public void setSubnetId( String subnetId ) {
    this.subnetId = subnetId;
  }

  public ArrayList<EC2Tag> getTags( ) {
    return tags;
  }

  public void setTags( ArrayList<EC2Tag> tags ) {
    this.tags = tags;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "description", description )
        .add( "groupSet", groupSet )
        .add( "privateIpAddress", privateIpAddress )
        .add( "privateIpAddresses", privateIpAddresses )
        .add( "secondaryPrivateIpAddressCount", secondaryPrivateIpAddressCount )
        .add( "sourceDestCheck", sourceDestCheck )
        .add( "subnetId", subnetId )
        .add( "tags", tags )
        .toString( );
  }
}
