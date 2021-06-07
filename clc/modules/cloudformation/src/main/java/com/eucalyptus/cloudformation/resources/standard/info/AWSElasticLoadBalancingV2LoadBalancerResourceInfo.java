/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.base.MoreObjects;

public class AWSElasticLoadBalancingV2LoadBalancerResourceInfo extends ResourceInfo {

  @AttributeJson
  private String canonicalHostedZoneID;

  @AttributeJson( name = "DNSName" )
  private String dnsName;

  @AttributeJson
  private String loadBalancerFullName;

  @AttributeJson
  private String loadBalancerName;

  @AttributeJson
  private String securityGroups;

  public AWSElasticLoadBalancingV2LoadBalancerResourceInfo( ) {
    setType( "AWS::ElasticLoadBalancingV2::LoadBalancer" );
  }

  @Override
  public boolean supportsTags( ) {
    return true;
  }

  public String getCanonicalHostedZoneID() {
    return canonicalHostedZoneID;
  }

  public void setCanonicalHostedZoneID(String canonicalHostedZoneID) {
    this.canonicalHostedZoneID = canonicalHostedZoneID;
  }

  public String getDnsName() {
    return dnsName;
  }

  public void setDnsName(String dnsName) {
    this.dnsName = dnsName;
  }

  public String getLoadBalancerFullName() {
    return loadBalancerFullName;
  }

  public void setLoadBalancerFullName(String loadBalancerFullName) {
    this.loadBalancerFullName = loadBalancerFullName;
  }

  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  public String getSecurityGroups() {
    return securityGroups;
  }

  public void setSecurityGroups(String securityGroups) {
    this.securityGroups = securityGroups;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("canonicalHostedZoneID", canonicalHostedZoneID)
        .add("dnsName", dnsName)
        .add("loadBalancerFullName", loadBalancerFullName)
        .add("loadBalancerName", loadBalancerName)
        .add("securityGroups", securityGroups)
        .toString();
  }
}
