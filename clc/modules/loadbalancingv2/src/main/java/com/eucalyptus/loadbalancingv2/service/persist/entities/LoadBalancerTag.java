/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.entities;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

@Entity
@PersistenceContext(name = "eucalyptus_loadbalancing")
@Table(name = "metadata_v2_tag_loadbalancer")
@DiscriminatorValue("loadbalancer")
public class LoadBalancerTag extends Tag<LoadBalancerTag> {

  private static final long serialVersionUID = 1L;

  @JoinColumn(name = "loadbalancer_idfref", updatable = false, nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private LoadBalancer loadBalancer;

  protected LoadBalancerTag() {
  }

  protected LoadBalancerTag(final LoadBalancer loadBalancer, final String key, final String value) {
    super(loadBalancer.getOwner(), loadBalancer.getArn(), key, value);
    setLoadBalancer(loadBalancer);
  }

  public static LoadBalancerTag create(final LoadBalancer loadBalancer, final String key, final String value) {
    return new LoadBalancerTag(loadBalancer, key, value);
  }

  public LoadBalancer getLoadBalancer() {
    return loadBalancer;
  }

  public void setLoadBalancer(final LoadBalancer loadBalancer) {
    this.loadBalancer = loadBalancer;
  }
}
