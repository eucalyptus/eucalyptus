/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.ArrayList;

public class AWSElasticLoadBalancingV2LoadBalancerProperties implements ResourceProperties {

  @Property
  private String name;

  @Property
  private String ipAddressType;

  @Property
  private String scheme;

  @Property
  private String type;

  @Property
  private ArrayList<String> securityGroups = Lists.newArrayList( );

  @Property
  private ArrayList<String> subnets = Lists.newArrayList( );

  @Property
  private ArrayList<ElasticLoadBalancingV2SubnetMappingProperties> subnetMappings =
      Lists.newArrayList();

  @Property
  private ArrayList<ElasticLoadBalancingV2LoadBalancerAttributeProperties> loadBalancerAttributes =
      Lists.newArrayList();

  @Property
  private ArrayList<CloudFormationResourceTag> tags = Lists.newArrayList( );

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIpAddressType() {
    return ipAddressType;
  }

  public void setIpAddressType(String ipAddressType) {
    this.ipAddressType = ipAddressType;
  }

  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ArrayList<String> getSecurityGroups() {
    return securityGroups;
  }

  public void setSecurityGroups(ArrayList<String> securityGroups) {
    this.securityGroups = securityGroups;
  }

  public ArrayList<String> getSubnets() {
    return subnets;
  }

  public void setSubnets(ArrayList<String> subnets) {
    this.subnets = subnets;
  }

  public ArrayList<ElasticLoadBalancingV2SubnetMappingProperties> getSubnetMappings() {
    return subnetMappings;
  }

  public void setSubnetMappings(
      ArrayList<ElasticLoadBalancingV2SubnetMappingProperties> subnetMappings) {
    this.subnetMappings = subnetMappings;
  }

  public ArrayList<ElasticLoadBalancingV2LoadBalancerAttributeProperties> getLoadBalancerAttributes() {
    return loadBalancerAttributes;
  }

  public void setLoadBalancerAttributes(
      ArrayList<ElasticLoadBalancingV2LoadBalancerAttributeProperties> loadBalancerAttributes) {
    this.loadBalancerAttributes = loadBalancerAttributes;
  }

  public ArrayList<CloudFormationResourceTag> getTags() {
    return tags;
  }

  public void setTags(
      ArrayList<CloudFormationResourceTag> tags) {
    this.tags = tags;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("ipAddressType", ipAddressType)
        .add("scheme", scheme)
        .add("type", type)
        .add("securityGroups", securityGroups)
        .add("subnets", subnets)
        .add("subnetMappings", subnetMappings)
        .add("loadBalancerAttributes", loadBalancerAttributes)
        .add("tags", tags)
        .toString();
  }
}
