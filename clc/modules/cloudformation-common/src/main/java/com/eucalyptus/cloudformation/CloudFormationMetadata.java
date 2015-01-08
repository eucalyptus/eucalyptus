/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudformation;

import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.cloudformation.common.policy.CloudFormationPolicySpec;
import com.eucalyptus.component.annotation.PolicyVendor;
import com.eucalyptus.util.RestrictedType;

/**
 * Created by ethomas on 10/22/14.
 */
@PolicyVendor( CloudFormationPolicySpec.VENDOR_CLOUDFORMATION )
public interface CloudFormationMetadata extends RestrictedType {
  @PolicyResourceType("stack")
  public interface StackMetadata extends CloudFormationMetadata {}

}
