/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.policy;

import com.eucalyptus.auth.policy.ern.ResourceNameSupport;

/**
 *
 */
public class Route53ResourceName extends ResourceNameSupport {

  public Route53ResourceName( String region, String account, String type, String id ) {
    super( Route53PolicySpec.VENDOR_ROUTE53, region, account, type, id );
  }
}