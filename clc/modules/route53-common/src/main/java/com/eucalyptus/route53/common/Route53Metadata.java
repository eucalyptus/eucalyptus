/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common;

import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.route53.common.policy.Route53PolicySpec;

@PolicyVendor(Route53PolicySpec.VENDOR_ROUTE53)
public interface Route53Metadata extends RestrictedType {

  @PolicyResourceType( "change" )
  interface ChangeMetadata extends Route53Metadata {}

  @PolicyResourceType( "delegationset" )
  interface DelegationSetMetadata extends Route53Metadata {}

  @PolicyResourceType( "healthcheck" )
  interface HealthCheckMetadata extends Route53Metadata {}

  @PolicyResourceType( "hostedzone" )
  interface HostedZoneMetadata extends Route53Metadata {}

  @PolicyResourceType( "trafficpolicy" )
  interface TrafficPolicyMetadata extends Route53Metadata {}

  @PolicyResourceType( "trafficpolicyinstance" )
  interface TrafficPolicyInstanceMetadata extends Route53Metadata {}

  @PolicyResourceType( "queryloggingconfig" )
  interface QueryLoggingConfigMetadata extends Route53Metadata {}

}
