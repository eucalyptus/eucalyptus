/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common;

import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.rds.common.policy.RdsPolicySpec;

@PolicyVendor(RdsPolicySpec.VENDOR_RDS)
public interface RdsMetadata extends RestrictedType {

  @PolicyResourceType( "db" )
  interface DBInstanceMetadata extends RdsMetadata {}

  @PolicyResourceType( "pg" )
  interface DBParameterGroupMetadata extends RdsMetadata {}

  @PolicyResourceType( "secgrp" )
  interface DBSecurityGroupMetadata extends RdsMetadata {}

  @PolicyResourceType( "subgrp" )
  interface DBSubnetGroupMetadata extends RdsMetadata {}

  @PolicyResourceType( "es" )
  interface EventSubscriptionMetadata extends RdsMetadata {}

  @PolicyResourceType( "og" )
  interface OptionGroupMetadata extends RdsMetadata {}

  @PolicyResourceType( "ri" )
  interface ReservedDBInstanceMetadata extends RdsMetadata {}

  @PolicyResourceType( "tag" )
  interface TagMetadata extends RdsMetadata {}
}
