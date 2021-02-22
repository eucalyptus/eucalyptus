/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.policy;

import com.eucalyptus.auth.policy.ern.ResourceNameSupport;

/**
 *
 */
public class RdsResourceName extends ResourceNameSupport {

  public RdsResourceName(
      final String region,
      final String account,
      final String resourceType,
      final String resourceName
  ) {
    super( RdsPolicySpec.VENDOR_RDS, region, account, resourceType, resourceName );
  }

}
