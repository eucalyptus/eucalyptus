/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.principal;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.type.RestrictedType;

/**
 *
 */
@PolicyVendor( PolicySpec.VENDOR_IAM )
@PolicyResourceType( value = PolicySpec.IAM_RESOURCE_ROLE, resourcePolicyActions = { "sts:assumerole", "sts:assumerolewithwebidentity" } )
public interface BaseRole extends RestrictedType, RestrictedType.PolicyRestrictedType {

  String getAccountNumber( ) throws AuthException;

  String getRoleId( );

  String getRoleArn( ) throws AuthException;

  String getPath( );

  String getName( );

  String getSecret( );
}
