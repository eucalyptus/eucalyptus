/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.tokens.common.policy;

import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.ResourceNameSupport;

/**
 *
 */
public class StsResourceName extends ResourceNameSupport {

  public StsResourceName(
      final String region,
      final String account,
      final String resourceType,
      final String resourceName
  ) {
    super( PolicySpec.VENDOR_STS, region, account, resourceType, resourceName );
  }
}
