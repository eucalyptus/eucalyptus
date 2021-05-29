/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.base.MoreObjects;

public class AWSElasticLoadBalancingV2TargetGroupResourceInfo extends ResourceInfo {

  @AttributeJson
  private String loadBalancerArns;

  @AttributeJson
  private String targetGroupFullName;

  @AttributeJson
  private String targetGroupName;

  public AWSElasticLoadBalancingV2TargetGroupResourceInfo( ) {
    setType( "AWS::ElasticLoadBalancingV2::TargetGroup" );
  }

  @Override
  public boolean supportsTags( ) {
    return true;
  }

  public String getLoadBalancerArns() {
    return loadBalancerArns;
  }

  public void setLoadBalancerArns(String loadBalancerArns) {
    this.loadBalancerArns = loadBalancerArns;
  }

  public String getTargetGroupFullName() {
    return targetGroupFullName;
  }

  public void setTargetGroupFullName(String targetGroupFullName) {
    this.targetGroupFullName = targetGroupFullName;
  }

  public String getTargetGroupName() {
    return targetGroupName;
  }

  public void setTargetGroupName(String targetGroupName) {
    this.targetGroupName = targetGroupName;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("loadBalancerArns", loadBalancerArns)
        .add("targetGroupFullName", targetGroupFullName)
        .add("targetGroupName", targetGroupName)
        .toString();
  }
}
