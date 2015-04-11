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
package com.eucalyptus.auth.api;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.UserPrincipal;

/**
 *
 */
public interface IdentityProvider {

  /**
   *
   * @param nonce value used for secret access key generation (optional)
   * @return
   * @throws AuthException
   */
  UserPrincipal lookupPrincipalByUserId( String userId, String nonce ) throws AuthException;
  UserPrincipal lookupPrincipalByRoleId( String roleId, String nonce ) throws AuthException;
  UserPrincipal lookupPrincipalByAccessKeyId( String keyId, String nonce ) throws AuthException;
  UserPrincipal lookupPrincipalByCertificateId( String certificateId ) throws AuthException;
  UserPrincipal lookupPrincipalByCanonicalId( String canonicalId ) throws AuthException;
  UserPrincipal lookupPrincipalByAccountNumber( String accountNumber ) throws AuthException;
  UserPrincipal lookupPrincipalByAccountNumberAndUsername( String accountNumber, String username ) throws AuthException;

  /**
   *
   */
  AccountIdentifiers lookupAccountIdentifiersByAlias( String alias ) throws AuthException;
  AccountIdentifiers lookupAccountIdentifiersByCanonicalId( String canonicalId ) throws AuthException;

  /**
   *
   */
  InstanceProfile lookupInstanceProfileByName( String accountNumber, String name ) throws AuthException;

  /**
   *
   */
  Role lookupRoleByName( String accountNumber, String name ) throws AuthException;

  /**
   *
   */
  SecurityTokenContent decodeSecurityToken( String accessKeyIdentifier, String securityToken ) throws AuthException;
}
