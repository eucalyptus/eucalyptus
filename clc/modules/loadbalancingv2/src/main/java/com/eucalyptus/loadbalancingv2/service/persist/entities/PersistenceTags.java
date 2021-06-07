/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.service.persist.Tags;

@ComponentNamed
public class PersistenceTags extends Loadbalancingv2PersistenceSupport<Loadbalancingv2Metadata.TagMetadata, Tag<?>>
    implements Tags {

  public PersistenceTags() {
    super("tag");
  }

  @Override
  protected Tag<?> exampleWithOwner(final OwnerFullName ownerFullName) {
    return Tag.named(ownerFullName, null);
  }

  @Override
  protected Tag<?> exampleWithName(final OwnerFullName ownerFullName, final String name) {
    return Tag.named(ownerFullName, name);
  }
}
