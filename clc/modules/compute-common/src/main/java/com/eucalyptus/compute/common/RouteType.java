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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class RouteType extends EucalyptusData {

  private String destinationCidrBlock;
  private String gatewayId;
  private String instanceId;
  private String instanceOwnerId;
  private String natGatewayId;
  private String networkInterfaceId;
  private String vpcPeeringConnectionId;
  private String state;
  private String origin;

  public RouteType( ) {
  }

  public RouteType( final String destinationCidrBlock, final String state, final String origin ) {
    this.destinationCidrBlock = destinationCidrBlock;
    this.gatewayId = gatewayId;
    this.state = state;
    this.origin = origin;
  }

  public RouteType( final String destinationCidrBlock, final String instanceId, final String instanceOwnerId, final String networkInterfaceId, final String state, final String origin ) {
    this.destinationCidrBlock = destinationCidrBlock;
    this.instanceId = instanceId;
    this.instanceOwnerId = instanceOwnerId;
    this.networkInterfaceId = networkInterfaceId;
    this.state = state;
    this.origin = origin;
  }

  public RouteType withGatewayId( String gatewayId ) {
    setGatewayId( gatewayId );
    return this;
  }

  public RouteType withNatGatewayId( String natGatewayId ) {
    setNatGatewayId( natGatewayId );
    return this;
  }

  public String getDestinationCidrBlock( ) {
    return destinationCidrBlock;
  }

  public void setDestinationCidrBlock( String destinationCidrBlock ) {
    this.destinationCidrBlock = destinationCidrBlock;
  }

  public String getGatewayId( ) {
    return gatewayId;
  }

  public void setGatewayId( String gatewayId ) {
    this.gatewayId = gatewayId;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getInstanceOwnerId( ) {
    return instanceOwnerId;
  }

  public void setInstanceOwnerId( String instanceOwnerId ) {
    this.instanceOwnerId = instanceOwnerId;
  }

  public String getNatGatewayId( ) {
    return natGatewayId;
  }

  public void setNatGatewayId( String natGatewayId ) {
    this.natGatewayId = natGatewayId;
  }

  public String getNetworkInterfaceId( ) {
    return networkInterfaceId;
  }

  public void setNetworkInterfaceId( String networkInterfaceId ) {
    this.networkInterfaceId = networkInterfaceId;
  }

  public String getVpcPeeringConnectionId( ) {
    return vpcPeeringConnectionId;
  }

  public void setVpcPeeringConnectionId( String vpcPeeringConnectionId ) {
    this.vpcPeeringConnectionId = vpcPeeringConnectionId;
  }

  public String getState( ) {
    return state;
  }

  public void setState( String state ) {
    this.state = state;
  }

  public String getOrigin( ) {
    return origin;
  }

  public void setOrigin( String origin ) {
    this.origin = origin;
  }
}
