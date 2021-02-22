/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.views;

import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;

/**
 *
 */
@Immutable
public interface DBInstanceRuntimeView {

  @Nullable
  String getSystemSubnetId();

  @Nullable
  String getSystemVpcId();

  @Nullable
  String getSystemVolumeId();

  @Nullable
  String getSystemInstanceId();

  @Nullable
  String getStackId();

  @Nullable
  String getUserSubnetId();

  @Nullable
  String getUserNetworkInterfaceId();

  @Nullable
  String getPublicIp();

  @Nullable
  String getPrivateIp();
}
