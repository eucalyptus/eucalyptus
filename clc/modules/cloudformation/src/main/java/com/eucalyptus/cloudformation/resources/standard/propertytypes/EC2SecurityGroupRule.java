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

import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class EC2SecurityGroupRule {

  @Property
  private String cidrIp;

  @Property
  private String destinationSecurityGroupId;

  @Property
  private Integer fromPort;

  @Required
  @Property
  private String ipProtocol;

  @Property
  private String sourceSecurityGroupId;

  @Property
  private String sourceSecurityGroupName;

  @Property
  private String sourceSecurityGroupOwnerId;

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

  public String getIpProtocol( ) {
    return ipProtocol;
  }

  public void setIpProtocol( String ipProtocol ) {
    this.ipProtocol = ipProtocol;
  }

  public String getSourceSecurityGroupId( ) {
    return sourceSecurityGroupId;
  }

  public void setSourceSecurityGroupId( String sourceSecurityGroupId ) {
    this.sourceSecurityGroupId = sourceSecurityGroupId;
  }

  public String getSourceSecurityGroupName( ) {
    return sourceSecurityGroupName;
  }

  public void setSourceSecurityGroupName( String sourceSecurityGroupName ) {
    this.sourceSecurityGroupName = sourceSecurityGroupName;
  }

  public String getSourceSecurityGroupOwnerId( ) {
    return sourceSecurityGroupOwnerId;
  }

  public void setSourceSecurityGroupOwnerId( String sourceSecurityGroupOwnerId ) {
    this.sourceSecurityGroupOwnerId = sourceSecurityGroupOwnerId;
  }

  public Integer getToPort( ) {
    return toPort;
  }

  public void setToPort( Integer toPort ) {
    this.toPort = toPort;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final EC2SecurityGroupRule that = (EC2SecurityGroupRule) o;
    return Objects.equals( getCidrIp( ), that.getCidrIp( ) ) &&
        Objects.equals( getDestinationSecurityGroupId( ), that.getDestinationSecurityGroupId( ) ) &&
        Objects.equals( getFromPort( ), that.getFromPort( ) ) &&
        Objects.equals( getIpProtocol( ), that.getIpProtocol( ) ) &&
        Objects.equals( getSourceSecurityGroupId( ), that.getSourceSecurityGroupId( ) ) &&
        Objects.equals( getSourceSecurityGroupName( ), that.getSourceSecurityGroupName( ) ) &&
        Objects.equals( getSourceSecurityGroupOwnerId( ), that.getSourceSecurityGroupOwnerId( ) ) &&
        Objects.equals( getToPort( ), that.getToPort( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getCidrIp( ), getDestinationSecurityGroupId( ), getFromPort( ), getIpProtocol( ), getSourceSecurityGroupId( ), getSourceSecurityGroupName( ), getSourceSecurityGroupOwnerId( ), getToPort( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "cidrIp", cidrIp )
        .add( "destinationSecurityGroupId", destinationSecurityGroupId )
        .add( "fromPort", fromPort )
        .add( "ipProtocol", ipProtocol )
        .add( "sourceSecurityGroupId", sourceSecurityGroupId )
        .add( "sourceSecurityGroupName", sourceSecurityGroupName )
        .add( "sourceSecurityGroupOwnerId", sourceSecurityGroupOwnerId )
        .add( "toPort", toPort )
        .toString( );
  }
}
