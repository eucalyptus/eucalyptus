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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class AWSEC2SecurityGroupIngressProperties implements ResourceProperties {

  @Property
  private String groupName;

  @Property
  private String groupId;

  @Required
  @Property
  private String ipProtocol;

  @Property
  private String cidrIp;

  @Property
  private String sourceSecurityGroupName;

  @Property
  private String sourceSecurityGroupId;

  @Property
  private String sourceSecurityGroupOwnerId;

  @Property
  private Integer fromPort;

  @Property
  private Integer toPort;

  public String getCidrIp( ) {
    return cidrIp;
  }

  public void setCidrIp( String cidrIp ) {
    this.cidrIp = cidrIp;
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

  public String getGroupName( ) {
    return groupName;
  }

  public void setGroupName( String groupName ) {
    this.groupName = groupName;
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
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "groupName", groupName )
        .add( "groupId", groupId )
        .add( "ipProtocol", ipProtocol )
        .add( "cidrIp", cidrIp )
        .add( "sourceSecurityGroupName", sourceSecurityGroupName )
        .add( "sourceSecurityGroupId", sourceSecurityGroupId )
        .add( "sourceSecurityGroupOwnerId", sourceSecurityGroupOwnerId )
        .add( "fromPort", fromPort )
        .add( "toPort", toPort )
        .toString( );
  }
}
