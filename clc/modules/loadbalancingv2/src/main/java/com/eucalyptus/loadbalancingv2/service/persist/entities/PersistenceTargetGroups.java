/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.loadbalancingv2.service.persist.TargetGroups;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;

@ComponentNamed
public class PersistenceTargetGroups extends Loadbalancingv2PersistenceSupport<Loadbalancingv2Metadata.TargetgroupMetadata, TargetGroup>
    implements TargetGroups {

  public PersistenceTargetGroups() {
    super("targetgroup");
  }

  @Override
  protected TargetGroup exampleWithOwner(final OwnerFullName ownerFullName) {
    return TargetGroup.named(ownerFullName, null);
  }

  @Override
  protected TargetGroup exampleWithName(final OwnerFullName ownerFullName, final String name) {
    return TargetGroup.named(ownerFullName, name);
  }
}
