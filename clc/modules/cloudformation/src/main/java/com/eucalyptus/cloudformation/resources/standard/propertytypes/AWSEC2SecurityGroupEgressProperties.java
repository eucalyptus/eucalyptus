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

import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class AWSEC2SecurityGroupEgressProperties implements ResourceProperties {

  @Property
  private String cidrIp;

  @Property
  private String destinationSecurityGroupId;

  @Property
  private Integer fromPort;

  @Required
  @Property
  private String groupId;

  @Required
  @Property
  private String ipProtocol;

  @Property
  private Integer toPort;

  public String getCidrIp( ) {
    return cidrIp;
  }

  public void setCidrIp( String cidrIp ) {
    this.cidrIp = cidrIp;
  }

  public String getDestinationSecurityGroupId( ) {
    return destinationSecurityGroupId;
  }

  public void setDestinationSecurityGroupId( String destinationSecurityGroupId ) {
    this.destinationSecurityGroupId = destinationSecurityGroupId;
  }

  public Integer getFromPort( ) {
    return fromPort;
  }

  public void setFromPort( Integer fromPort ) {
    this.fromPort = fromPort;
  }

  public String getGroupId( ) {
    return groupId;
  }

  public void setGroupId( String groupId ) {
    this.groupId = groupId;
  }

  public String getIpProtocol( ) {
    return ipProtocol;
  }

  public void setIpProtocol( String ipProtocol ) {
    this.ipProtocol = ipProtocol;
  }

  public Integer getToPort( ) {
    return toPort;
  }

  public void setToPort( Integer toPort ) {
    this.toPort = toPort;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "cidrIp", cidrIp )
        .add( "destinationSecurityGroupId", destinationSecurityGroupId )
        .add( "fromPort", fromPort )
        .add( "groupId", groupId )
        .add( "ipProtocol", ipProtocol )
        .add( "toPort", toPort )
        .toString( );
  }
}
