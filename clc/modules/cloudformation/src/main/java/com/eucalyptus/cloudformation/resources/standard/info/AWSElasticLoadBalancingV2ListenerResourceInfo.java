/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.base.MoreObjects;

public class AWSElasticLoadBalancingV2ListenerResourceInfo extends ResourceInfo {
                      
  @AttributeJson
  private String listenerArn;

  public AWSElasticLoadBalancingV2ListenerResourceInfo( ) {
    setType( "AWS::ElasticLoadBalancingV2::Listener" );
  }

  public String getListenerArn() {
    return listenerArn;
  }

  public void setListenerArn(String listenerArn) {
    this.listenerArn = listenerArn;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("listenerArn", listenerArn)
        .toString();
  }
}
