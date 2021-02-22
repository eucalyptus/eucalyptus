/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.rds.common.RdsMetadata.DBInstanceMetadata;
import com.eucalyptus.rds.service.persist.DBInstances;

/**
 *
 */
@ComponentNamed
public class PersistenceDBInstances extends RdsPersistenceSupport<DBInstanceMetadata, DBInstance> implements DBInstances {

  public PersistenceDBInstances() {
    super("db");
  }

  @Override
  protected DBInstance exampleWithName(final OwnerFullName ownerFullName, final String name) {
    return DBInstance.exampleWithName(ownerFullName, name);
  }

  @Override
  protected DBInstance exampleWithOwner(final OwnerFullName ownerFullName) {
    return DBInstance.exampleWithOwner(ownerFullName);
  }
}