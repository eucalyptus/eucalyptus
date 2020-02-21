/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.policy;

import com.eucalyptus.auth.policy.key.PolicyKey;
import com.eucalyptus.route53.common.Route53Metadata.HostedZoneMetadata;
import com.eucalyptus.route53.common.policy.Route53PolicySpec;

/**
 *
 */
@PolicyKey( Route53HostedZoneNumberQuotaKey.KEY )
public class Route53HostedZoneNumberQuotaKey extends Route53NumberQuotaKeySupport<HostedZoneMetadata> {

  public static final String KEY = "route53:quota-hostedzonenumber";

  public Route53HostedZoneNumberQuotaKey() {
    super( KEY,
        Route53PolicySpec.ROUTE53_CREATEHOSTEDZONE,
        HostedZoneMetadata.class );
  }
}
