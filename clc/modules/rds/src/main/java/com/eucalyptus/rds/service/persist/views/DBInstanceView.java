/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist.views;

import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;
import com.eucalyptus.rds.service.persist.entities.DBInstance;

/**
 *
 */
@Immutable
public interface DBInstanceView {

  String getNaturalId();

  String getDisplayName();

  String getOwnerAccountNumber();

  DBInstance.Status getState();

  @Nullable
  Integer getAllocatedStorage();

  @Nullable
  String getAvailabilityZone();

  @Nullable
  Boolean getCopyTagsToSnapshot();

  @Nullable
  String getDbName();

  @Nullable
  Integer getDbPort();

  @Nullable
  String getInstanceClass();

  @Nullable
  String getEngine();

  @Nullable
  String getEngineVersion();

  @Nullable
  String getMasterUsername();

  @Nullable
  String getMasterUserPassword();

  @Nullable
  Boolean getPubliclyAccessible();

  List<String> getVpcSecurityGroups();
}
