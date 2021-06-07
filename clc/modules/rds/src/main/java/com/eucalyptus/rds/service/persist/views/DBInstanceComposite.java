/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.views;

import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;

/**
 *
 */
@Immutable
public interface DBInstanceComposite {

  DBInstanceView getInstance();

  DBInstanceRuntimeView getRuntime();

  @Nullable
  DBSubnetGroupView getSubnetGroup();

  List<DBSubnetView> getSubnets();
}
