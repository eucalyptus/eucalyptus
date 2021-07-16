/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.rds.common.RdsMetadata.DBParameterGroupMetadata;
import com.eucalyptus.rds.service.persist.DBParameterGroups;


@ComponentNamed
public class PersistenceDBParameterGroups extends RdsPersistenceSupport<DBParameterGroupMetadata, DBParameterGroup> implements DBParameterGroups {

  public PersistenceDBParameterGroups() {
    super("pg");
  }

  @Override
  protected DBParameterGroup exampleWithOwner(final OwnerFullName ownerFullName) {
    return DBParameterGroup.exampleWithOwner(ownerFullName);
  }

  @Override
  protected DBParameterGroup exampleWithName(final OwnerFullName ownerFullName, final String name) {
    return DBParameterGroup.exampleWithName(ownerFullName, name);
  }
}
