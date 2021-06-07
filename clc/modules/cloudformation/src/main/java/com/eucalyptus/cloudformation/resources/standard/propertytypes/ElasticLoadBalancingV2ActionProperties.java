/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class ElasticLoadBalancingV2ActionProperties {

  @Property
  private ElasticLoadBalancingV2FixedResponseConfigProperties fixedResponseConfig;

  @Property
  private ElasticLoadBalancingV2ForwardConfigProperties forwardConfig;

  @Property
  private Integer order;

  @Property
  private ElasticLoadBalancingV2RedirectConfigProperties redirectConfig;

  @Property
  private String targetGroupArn;

  @Property
  @Required
  private String type;

  public ElasticLoadBalancingV2FixedResponseConfigProperties getFixedResponseConfig() {
    return fixedResponseConfig;
  }

  public void setFixedResponseConfig(
      ElasticLoadBalancingV2FixedResponseConfigProperties fixedResponseConfig) {
    this.fixedResponseConfig = fixedResponseConfig;
  }

  public ElasticLoadBalancingV2ForwardConfigProperties getForwardConfig() {
    return forwardConfig;
  }

  public void setForwardConfig(
      ElasticLoadBalancingV2ForwardConfigProperties forwardConfig) {
    this.forwardConfig = forwardConfig;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  public ElasticLoadBalancingV2RedirectConfigProperties getRedirectConfig() {
    return redirectConfig;
  }

  public void setRedirectConfig(
      ElasticLoadBalancingV2RedirectConfigProperties redirectConfig) {
    this.redirectConfig = redirectConfig;
  }

  public String getTargetGroupArn() {
    return targetGroupArn;
  }

  public void setTargetGroupArn(String targetGroupArn) {
    this.targetGroupArn = targetGroupArn;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fixedResponseConfig", fixedResponseConfig)
        .add("forwardConfig", forwardConfig)
        .add("order", order)
        .add("redirectConfig", redirectConfig)
        .add("targetGroupArn", targetGroupArn)
        .add("type", type)
        .toString();
  }
}
