/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.views;

import org.immutables.value.Value.Immutable;
import com.eucalyptus.rds.service.persist.entities.DBSubnet.Status;

/**
 *
 */
@Immutable
public interface DBSubnetView {

  Status getStatus();

  String getSubnetId();

  String getAvailabilityZone();

}
