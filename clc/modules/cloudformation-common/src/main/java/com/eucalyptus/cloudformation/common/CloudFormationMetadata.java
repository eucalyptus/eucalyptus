/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common;

import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.cloudformation.common.policy.CloudFormationPolicySpec;

@PolicyVendor(CloudFormationPolicySpec.VENDOR_CLOUDFORMATION)
public interface CloudFormationMetadata extends RestrictedType {
  @PolicyResourceType("stack")
  public interface StackMetadata extends CloudFormationMetadata {}
}
