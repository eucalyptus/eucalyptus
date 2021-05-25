/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.ArrayList;

public class AWSElasticLoadBalancingV2ListenerProperties implements ResourceProperties {

  @Property
  @Required
  private String loadBalancerArn;

  @Property
  private Integer port;

  @Property
  private String protocol;

  @Property
  private String sslPolicy;

  @Property
  private ArrayList<ElasticLoadBalancingV2CertificateProperties> certificates = Lists.newArrayList( );

  @Property
  private ArrayList<String> alpnPolicy = Lists.newArrayList( );

  @Property
  @Required
  private ArrayList<ElasticLoadBalancingV2ActionProperties> defaultActions = Lists.newArrayList( );

  public String getLoadBalancerArn() {
    return loadBalancerArn;
  }

  public void setLoadBalancerArn(String loadBalancerArn) {
    this.loadBalancerArn = loadBalancerArn;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getSslPolicy() {
    return sslPolicy;
  }

  public void setSslPolicy(String sslPolicy) {
    this.sslPolicy = sslPolicy;
  }

  public ArrayList<ElasticLoadBalancingV2CertificateProperties> getCertificates() {
    return certificates;
  }

  public void setCertificates(
      ArrayList<ElasticLoadBalancingV2CertificateProperties> certificates) {
    this.certificates = certificates;
  }

  public ArrayList<String> getAlpnPolicy() {
    return alpnPolicy;
  }

  public void setAlpnPolicy(ArrayList<String> alpnPolicy) {
    this.alpnPolicy = alpnPolicy;
  }

  public ArrayList<ElasticLoadBalancingV2ActionProperties> getDefaultActions() {
    return defaultActions;
  }

  public void setDefaultActions(
      ArrayList<ElasticLoadBalancingV2ActionProperties> defaultActions) {
    this.defaultActions = defaultActions;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("loadBalancerArn", loadBalancerArn)
        .add("port", port)
        .add("protocol", protocol)
        .add("sslPolicy", sslPolicy)
        .add("certificates", certificates)
        .add("alpnPolicy", alpnPolicy)
        .add("defaultActions", defaultActions)
        .toString();
  }
}
