/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common;

import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.rds.common.policy.RdsResourceName;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.RestrictedTypes;


public class RdsMetadatas extends RestrictedTypes {

  public static String toArn(final RdsMetadata metadata) {
    return new RdsResourceName(
      "",
      metadata.getOwner().getAccountNumber(),
        Ats.inClassHierarchy(metadata).get(PolicyResourceType.class).value(),
      metadata.getDisplayName()
    ).toString();
  }
}
