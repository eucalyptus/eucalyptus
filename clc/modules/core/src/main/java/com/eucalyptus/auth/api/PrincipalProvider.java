/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
