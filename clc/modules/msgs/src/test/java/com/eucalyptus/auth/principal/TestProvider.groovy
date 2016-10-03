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
