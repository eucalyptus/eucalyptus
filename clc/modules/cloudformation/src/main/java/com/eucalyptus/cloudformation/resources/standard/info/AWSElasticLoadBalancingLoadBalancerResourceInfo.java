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
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.base.MoreObjects;

public class AWSElasticLoadBalancingLoadBalancerResourceInfo extends ResourceInfo {

  @AttributeJson
  private String canonicalHostedZoneName;
  @AttributeJson
  private String canonicalHostedZoneNameID;
  @AttributeJson( name = "DNSName" )
  private String dnsName;
  @AttributeJson( name = "SourceSecurityGroup.GroupName" )
  private String sourceSecurityGroupGroupName;
  @AttributeJson( name = "SourceSecurityGroup.OwnerAlias" )
  private String sourceSecurityGroupOwnerAlias;

  public AWSElasticLoadBalancingLoadBalancerResourceInfo( ) {
    setType( "AWS::ElasticLoadBalancing::LoadBalancer" );
  }

  @Override
  public boolean supportsTags( ) {
    return true;
  }

  public String getCanonicalHostedZoneName( ) {
    return canonicalHostedZoneName;
  }

  public void setCanonicalHostedZoneName( String canonicalHostedZoneName ) {
    this.canonicalHostedZoneName = canonicalHostedZoneName;
  }

  public String getCanonicalHostedZoneNameID( ) {
    return canonicalHostedZoneNameID;
  }

  public void setCanonicalHostedZoneNameID( String canonicalHostedZoneNameID ) {
    this.canonicalHostedZoneNameID = canonicalHostedZoneNameID;
  }

  public String getDnsName( ) {
    return dnsName;
  }

  public void setDnsName( String dnsName ) {
    this.dnsName = dnsName;
  }

  public String getSourceSecurityGroupGroupName( ) {
    return sourceSecurityGroupGroupName;
  }

  public void setSourceSecurityGroupGroupName( String sourceSecurityGroupGroupName ) {
    this.sourceSecurityGroupGroupName = sourceSecurityGroupGroupName;
  }

  public String getSourceSecurityGroupOwnerAlias( ) {
    return sourceSecurityGroupOwnerAlias;
  }

  public void setSourceSecurityGroupOwnerAlias( String sourceSecurityGroupOwnerAlias ) {
    this.sourceSecurityGroupOwnerAlias = sourceSecurityGroupOwnerAlias;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "canonicalHostedZoneName", canonicalHostedZoneName )
        .add( "canonicalHostedZoneNameID", canonicalHostedZoneNameID )
        .add( "dnsName", dnsName )
        .add( "sourceSecurityGroupGroupName", sourceSecurityGroupGroupName )
        .add( "sourceSecurityGroupOwnerAlias", sourceSecurityGroupOwnerAlias )
        .toString( );
  }
}
