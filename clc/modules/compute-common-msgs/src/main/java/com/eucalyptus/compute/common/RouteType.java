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
