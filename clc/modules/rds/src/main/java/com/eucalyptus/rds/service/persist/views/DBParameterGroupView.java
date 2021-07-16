/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.views;

import com.eucalyptus.rds.common.policy.RdsResourceName;
import org.immutables.value.Value;

/**
 *
 */
@Value.Immutable
public interface DBParameterGroupView {

  String getNaturalId();

  String getDisplayName();

  String getOwnerAccountNumber();

  String getFamily();

  default String getArn() {
    return new RdsResourceName(
        "",
        getOwnerAccountNumber(),
        "pg",
        getDisplayName()
    ).toString();
  }
}
