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
package com.eucalyptus.auth.euare.principal;

import java.util.Date;
import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.principal.AccountScopedPrincipal;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.BaseRole;

/**
 *
 */
public interface EuareRole extends BaseRole, EuareAccountScopedPrincipal {

  EuareAccount getAccount( ) throws AuthException;

  Policy getAssumeRolePolicy( ) throws AuthException;
  Policy setAssumeRolePolicy( String policy ) throws AuthException, PolicyParseException;

  List<EuareInstanceProfile> getInstanceProfiles() throws AuthException;

  Date getCreationTimestamp( );

  List<Policy> getPolicies( ) throws AuthException;

  /**
   * Add a policy, fail if exists.
   */
  Policy addPolicy( String name, String policy ) throws AuthException, PolicyParseException;

  /**
   * Add or update the named policy.
   */
  Policy putPolicy( String name, String policy ) throws AuthException, PolicyParseException;
  void removePolicy( String name ) throws AuthException;
}
