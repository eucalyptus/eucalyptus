/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.views;

import org.immutables.value.Value.Immutable;

/**
 *
 */
@Immutable
public interface DBInstanceRuntimeComposite {

  DBInstanceView getInstance();

  DBInstanceRuntimeView getRuntime();
}
