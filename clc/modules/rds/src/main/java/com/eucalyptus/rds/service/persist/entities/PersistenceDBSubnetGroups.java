/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.rds.common.RdsMetadata.DBSubnetGroupMetadata;
import com.eucalyptus.rds.service.persist.DBSubnetGroups;

/**
 *
 */
@ComponentNamed
public class PersistenceDBSubnetGroups extends RdsPersistenceSupport<DBSubnetGroupMetadata, DBSubnetGroup> implements DBSubnetGroups {

  public PersistenceDBSubnetGroups() {
    super("subgrp");
  }

  @Override
  protected DBSubnetGroup exampleWithOwner(final OwnerFullName ownerFullName) {
    return DBSubnetGroup.exampleWithOwner(ownerFullName);
  }

  @Override
  protected DBSubnetGroup exampleWithName(final OwnerFullName ownerFullName, final String name) {
    return DBSubnetGroup.exampleWithName(ownerFullName, name);
  }
}
