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

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.OpenIdConnectProvider;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.UserPrincipal;

/**
 *
 */
public interface PrincipalProvider {

  /**
   * @param nonce value used for secret access key generation (optional)
   */
  UserPrincipal lookupPrincipalByUserId( String userId, String nonce ) throws AuthException;
  UserPrincipal lookupPrincipalByRoleId( String roleId, String nonce ) throws AuthException;
  UserPrincipal lookupPrincipalByAccessKeyId( String keyId, String nonce ) throws AuthException;
  UserPrincipal lookupPrincipalByCertificateId( String certificateId ) throws AuthException;
  UserPrincipal lookupPrincipalByCanonicalId( String canonicalId ) throws AuthException;
  UserPrincipal lookupPrincipalByAccountNumber( String accountNumber ) throws AuthException;
  UserPrincipal lookupPrincipalByAccountNumberAndUsername( String accountNumber, String username ) throws AuthException;

  /**
   * Principal lookup that may be cached
   */
  UserPrincipal lookupCachedPrincipalByUserId( UserPrincipal cached, String userId, String nonce ) throws AuthException;
  UserPrincipal lookupCachedPrincipalByRoleId( UserPrincipal cached, String roleId, String nonce ) throws AuthException;
  UserPrincipal lookupCachedPrincipalByAccessKeyId( UserPrincipal cached, String keyId, String nonce ) throws AuthException;
  UserPrincipal lookupCachedPrincipalByCertificateId( UserPrincipal cached, String certificateId ) throws AuthException;
  UserPrincipal lookupCachedPrincipalByAccountNumber( UserPrincipal cached, String accountNumber ) throws AuthException;

  /**
   *
   */
  AccountIdentifiers lookupAccountIdentifiersByAlias( String alias ) throws AuthException;
  AccountIdentifiers lookupAccountIdentifiersByCanonicalId( String canonicalId ) throws AuthException;
  AccountIdentifiers lookupAccountIdentifiersByEmail( String email ) throws AuthException;
  List<AccountIdentifiers> listAccountIdentifiersByAliasMatch( String aliasExpression ) throws AuthException;

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
  OpenIdConnectProvider lookupOidcProviderByUrl( String accountNumber, String url ) throws AuthException;

  /**
   *
   */
  SecurityTokenContent decodeSecurityToken( String accessKeyIdentifier, String securityToken ) throws AuthException;

  /**
   * Reserve a global name for a period of time.
   *
   * @param namespace The namespace for the name (qualified policy resource type)
   * @param name The name to reserve
   * @param duration The reservation duration in seconds
   * @param clientToken Optional client identifier for the request
   */
  void reserveGlobalName( String namespace, String name, Integer duration, String clientToken ) throws AuthException;

  X509Certificate getCertificateByAccountNumber( String accountNumber ) throws AuthException;

  X509Certificate signCertificate( String accountNumber, RSAPublicKey publicKey, String principal, int expiryInDays ) throws AuthException;
}
