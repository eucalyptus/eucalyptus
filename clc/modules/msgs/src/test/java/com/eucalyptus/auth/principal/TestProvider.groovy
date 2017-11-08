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
package com.eucalyptus.auth.principal

import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.AuthException
import com.eucalyptus.auth.api.PrincipalProvider
import com.eucalyptus.auth.util.Identifiers
import com.eucalyptus.crypto.Crypto
import com.google.common.io.BaseEncoding

import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey

/**
 *
 */
class TestProvider implements PrincipalProvider {

  List<AccountInfo> accounts = []

  static class AccountInfo implements AccountIdentifiers {
    String accountNumber
    String accountAlias
    String canonicalId
    List<UserPrincipal> users = []

    User addTestUser( String name, String path, Map<String,String> properties  ) {
      AccountInfo info = this
      String userId = Identifiers.generateIdentifier( "AID" )
      User testUser = new UserPrincipal( ) {
        @Override String getName() { name }
        @Override String getPath() { path }
        @Override String getUserId() { userId }
        @Override String getAuthenticatedId() { getUserId( ) }
        @Override String getAccountAlias() { info.accountAlias }
        @Override String getAccountNumber() { info.accountNumber }
        @Override String getCanonicalId() { info.canonicalId }
        @Override boolean isEnabled() { true }
        @Override boolean isAccountAdmin() { "admin" == name }
        @Override boolean isSystemAdmin() { Accounts.isSystemAccount( accountAlias ) }
        @Override boolean isSystemUser() { isSystemAdmin() }
        @Override String getPassword() { null }
        @Override Long getPasswordExpires() { Long.MAX_VALUE }
        @Override List<AccessKey> getKeys() { [ ] }
        @Override List<Certificate> getCertificates() { [ ] }
        @Override List<PolicyVersion> getPrincipalPolicies() { [ ] }
        @Override String getToken() { null }
        @Override String getPTag() { null }
      }
      users << testUser
      testUser
    }
  }

  AccountInfo addTestAccount( String accountName ) {
    byte[] random = new byte[32]
    Crypto.getSecureRandomSupplier().get().nextBytes( random )
    AccountInfo info = new AccountInfo(
        accountNumber: Identifiers.generateAccountNumber( ),
        accountAlias: accountName,
        canonicalId: BaseEncoding.base16().lowerCase().encode( random )
    )
    accounts << info
    info
  }

  private <T> T check( T item ) {
    if ( !item ) throw new AuthException( "Not found" )
    item
  }

  @Override
  UserPrincipal lookupPrincipalByUserId(final String userId, final String nonce) throws AuthException {
    check( accounts*.users.flatten().find{ User user -> user.userId == userId  } )
  }

  @Override
  UserPrincipal lookupPrincipalByRoleId(final String roleId, final String nonce) throws AuthException {
    throw new AuthException( "Not implemented in test provider" )
  }

  @Override
  UserPrincipal lookupPrincipalByAccessKeyId(final String keyId, final String nonce) throws AuthException {
    throw new AuthException( "Not implemented in test provider" )
  }

  @Override
  UserPrincipal lookupPrincipalByCertificateId(final String certificateId) throws AuthException {
    throw new AuthException( "Not implemented in test provider" )
  }

  @Override
  UserPrincipal lookupPrincipalByCanonicalId(final String canonicalId) throws AuthException {
    check( accounts.find{ AccountInfo info -> info.canonicalId == canonicalId }?.users?.find{ User user -> user.accountAdmin } )
  }

  @Override
  UserPrincipal lookupPrincipalByAccountNumber(final String accountNumber) throws AuthException {
    lookupPrincipalByAccountNumberAndUsername( accountNumber, 'admin' )
  }

  @Override
  UserPrincipal lookupPrincipalByAccountNumberAndUsername(
      final String accountNumber,
      final String username
  ) throws AuthException {
    check( accounts.find{ AccountInfo info -> info.accountNumber == accountNumber }?.users?.find{ User user -> user.name == username } )
  }

  @Override
  UserPrincipal lookupCachedPrincipalByUserId( final UserPrincipal cached, final String userId, final String nonce ) throws AuthException {
    return lookupPrincipalByUserId( userId, nonce );
  }

  @Override
  UserPrincipal lookupCachedPrincipalByRoleId( final UserPrincipal cached, final String roleId, final String nonce ) throws AuthException {
    return lookupPrincipalByRoleId( roleId, nonce );
  }

  @Override
  UserPrincipal lookupCachedPrincipalByAccessKeyId( final UserPrincipal cached, final String keyId, final String nonce ) throws AuthException {
    return lookupPrincipalByAccessKeyId( keyId, nonce );
  }

  @Override
  UserPrincipal lookupCachedPrincipalByCertificateId( final UserPrincipal cached, final String certificateId ) throws AuthException {
    return lookupPrincipalByCertificateId( certificateId );
  }

  @Override
  UserPrincipal lookupCachedPrincipalByAccountNumber( final UserPrincipal cached, final String accountNumber ) throws AuthException {
    return lookupPrincipalByAccountNumber( accountNumber );
  }

  @Override
  AccountIdentifiers lookupAccountIdentifiersByAlias(final String alias) throws AuthException {
    check( accounts.find{ AccountInfo info -> info.accountAlias == alias } )
  }

  @Override
  AccountIdentifiers lookupAccountIdentifiersByCanonicalId(final String canonicalId) throws AuthException {
    check( accounts.find{ AccountInfo info -> info.canonicalId == canonicalId } )
  }

  @Override
  AccountIdentifiers lookupAccountIdentifiersByEmail(final String email) throws AuthException {
    throw new AuthException( "Not implemented in test provider" )
  }

  @Override
  List<AccountIdentifiers> listAccountIdentifiersByAliasMatch(final String aliasExpression) throws AuthException {
    throw new AuthException( "Not implemented in test provider" )
  }

  @Override
  InstanceProfile lookupInstanceProfileByName(final String accountNumber, final String name) throws AuthException {
    throw new AuthException( "Not implemented in test provider" )
  }

  @Override
  Role lookupRoleByName(final String accountNumber, final String name) throws AuthException {
    throw new AuthException( "Not implemented in test provider" )
  }

  @Override
  OpenIdConnectProvider lookupOidcProviderByUrl(final String accountNumber, final String url) throws AuthException {
    throw new AuthException( "Not implemented in test provider" )
  }

  @Override
  SecurityTokenContent decodeSecurityToken(
      final String accessKeyIdentifier, final String securityToken) throws AuthException {
    throw new AuthException( "Not implemented in test provider" )
  }

  @Override
  void reserveGlobalName(final String namespace, final String name, final Integer duration, final String clientToken ) {
  }

  @Override
  X509Certificate getCertificateByAccountNumber(final String accountNumber) throws AuthException {
    throw new AuthException( "Not implemented in test provider" )
  }

  @Override
  X509Certificate signCertificate(
      final String accountNumber,
      final RSAPublicKey publicKey, final String principal, final int expiryInDays) throws AuthException {
    throw new AuthException( "Not implemented in test provider" )
  }
}
