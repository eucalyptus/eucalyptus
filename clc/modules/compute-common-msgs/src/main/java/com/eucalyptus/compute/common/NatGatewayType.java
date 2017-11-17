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

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class NatGatewayType extends EucalyptusData {

  private Date createTime;
  private Date deleteTime;
  private String failureCode;
  private String failureMessage;
  private String natGatewayId;
  private String vpcId;
  private String subnetId;
  private String state;
  private NatGatewayAddressSetType natGatewayAddressSet = new NatGatewayAddressSetType( );

  public NatGatewayType( ) {
  }

  public NatGatewayType( final String natGatewayId, final Date createTime, final Date deleteTime, final String failureCode, final String failureMessage, final String vpcId, final String subnetId, final String state, final NatGatewayAddressSetItemType address ) {
    this.natGatewayId = natGatewayId;
    this.createTime = createTime;
    this.deleteTime = deleteTime;
    this.failureCode = failureCode;
    this.failureMessage = failureMessage;
    this.vpcId = vpcId;
    this.subnetId = subnetId;
    this.state = state;
    if ( address != null ) {
      natGatewayAddressSet.getItem( ).add( address );
    }

  }

  public Date getCreateTime( ) {
    return createTime;
  }

  public void setCreateTime( Date createTime ) {
    this.createTime = createTime;
  }

  public Date getDeleteTime( ) {
    return deleteTime;
  }

  public void setDeleteTime( Date deleteTime ) {
    this.deleteTime = deleteTime;
  }

  public String getFailureCode( ) {
    return failureCode;
  }

  public void setFailureCode( String failureCode ) {
    this.failureCode = failureCode;
  }

  public String getFailureMessage( ) {
    return failureMessage;
  }

  public void setFailureMessage( String failureMessage ) {
    this.failureMessage = failureMessage;
  }

  public String getNatGatewayId( ) {
    return natGatewayId;
  }

  public void setNatGatewayId( String natGatewayId ) {
    this.natGatewayId = natGatewayId;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
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

  public NatGatewayAddressSetType getNatGatewayAddressSet( ) {
    return natGatewayAddressSet;
  }

  public void setNatGatewayAddressSet( NatGatewayAddressSetType natGatewayAddressSet ) {
    this.natGatewayAddressSet = natGatewayAddressSet;
  }
}
