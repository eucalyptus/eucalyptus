/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.rds.common.RdsMetadata;
import com.eucalyptus.rds.service.persist.Tags;

@ComponentNamed("rdsTags")
public class PersistenceTags extends RdsPersistenceSupport<RdsMetadata.TagMetadata, Tag<?>> implements Tags {

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
