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

public class VpcPeeringConnectionType extends EucalyptusData {

  private String vpcPeeringConnectionId;
  private VpcPeeringConnectionVpcInfoType requesterVpcInfo;
  private VpcPeeringConnectionVpcInfoType accepterVpcInfo;
  private VpcPeeringConnectionStateReasonType status;
  private Date expirationTime;
  private ResourceTagSetType tagSet;

  public String getVpcPeeringConnectionId( ) {
    return vpcPeeringConnectionId;
  }

  public void setVpcPeeringConnectionId( String vpcPeeringConnectionId ) {
    this.vpcPeeringConnectionId = vpcPeeringConnectionId;
  }

  public VpcPeeringConnectionVpcInfoType getRequesterVpcInfo( ) {
    return requesterVpcInfo;
  }

  public void setRequesterVpcInfo( VpcPeeringConnectionVpcInfoType requesterVpcInfo ) {
    this.requesterVpcInfo = requesterVpcInfo;
  }

  public VpcPeeringConnectionVpcInfoType getAccepterVpcInfo( ) {
    return accepterVpcInfo;
  }

  public void setAccepterVpcInfo( VpcPeeringConnectionVpcInfoType accepterVpcInfo ) {
    this.accepterVpcInfo = accepterVpcInfo;
  }

  public VpcPeeringConnectionStateReasonType getStatus( ) {
    return status;
  }

  public void setStatus( VpcPeeringConnectionStateReasonType status ) {
    this.status = status;
  }

  public Date getExpirationTime( ) {
    return expirationTime;
  }

  public void setExpirationTime( Date expirationTime ) {
    this.expirationTime = expirationTime;
  }

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }
}
