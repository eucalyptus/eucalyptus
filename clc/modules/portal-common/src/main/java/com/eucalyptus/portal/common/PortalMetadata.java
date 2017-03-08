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
package com.eucalyptus.portal.common;

import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.portal.common.policy.PortalPolicySpec;

/**
 *
 */
@PolicyVendor( PortalPolicySpec.VENDOR_PORTAL )
public interface PortalMetadata extends RestrictedType {
  @PolicyResourceType( "account-settings" )
  interface BillingAccountMetadata extends PortalMetadata { }


  @PolicyResourceType( "billing-settings" )
  interface BillingInfoMetadata extends PortalMetadata { }

  @PolicyResourceType( "billing-reports" )
  interface  BillingReportMetadata extends PortalMetadata{ }
}
