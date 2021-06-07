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
@Table(name = "metadata_v2_tag_targetgroup")
@DiscriminatorValue("targetgroup")
public class TargetGroupTag extends Tag<TargetGroupTag> {

  private static final long serialVersionUID = 1L;

  @JoinColumn(name = "targetgroup_idfref", updatable = false, nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private TargetGroup targetGroup;

  protected TargetGroupTag() {
  }

  protected TargetGroupTag(final TargetGroup targetGroup, final String key, final String value) {
    super(targetGroup.getOwner(), targetGroup.getArn(), key, value);
    setTargetGroup(targetGroup);
  }

  public static TargetGroupTag create(final TargetGroup targetGroup, final String key, final String value) {
    return new TargetGroupTag(targetGroup, key, value);
  }

  public TargetGroup getTargetGroup() {
    return targetGroup;
  }

  public void setTargetGroup(final TargetGroup targetGroup) {
    this.targetGroup = targetGroup;
  }
}
