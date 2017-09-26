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
