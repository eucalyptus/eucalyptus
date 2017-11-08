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
