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
package com.eucalyptus.auth;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import com.eucalyptus.auth.api.IdentityProvider;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.EuareUser;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.auth.principal.UserPrincipalImpl;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.component.annotation.ComponentNamed;

/**
 *
 */
@ComponentNamed( "localIdentityProvider" )
public class DatabaseIdentityProvider implements IdentityProvider {

  @Override
  public UserPrincipal lookupPrincipalByUserId( final String userId, final String nonce ) throws AuthException {
    final EuareUser user = Accounts.lookupUserById( userId );
    return decorateCredentials( Accounts.userAsPrincipal( user ), nonce, user.getToken( ) );
  }

  @Override
  public UserPrincipal lookupPrincipalByRoleId( final String roleId, final String nonce ) throws AuthException {
    final Role role = Accounts.lookupRoleById( roleId );
    return decorateCredentials( Accounts.roleAsPrincipal( role ), nonce, role.getSecret() );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccessKeyId( final String keyId, final String nonce ) throws AuthException {
    final AccessKey accessKey = Accounts.lookupAccessKeyById( keyId );
    if ( !accessKey.isActive( ) ) {
      throw new InvalidAccessKeyAuthException( "Invalid access key or token" );
    }
    return decorateCredentials( accessKey.getPrincipal(), nonce, accessKey.getSecretKey() );
  }

  @Override
  public UserPrincipal lookupPrincipalByCertificateId( final String certificateId ) throws AuthException {
    final Certificate certificate = Accounts.lookupCertificateById( certificateId );
    if ( !certificate.isActive( ) ) {
      throw new AuthException( "Certificate is inactive or revoked: " + certificate.getX509Certificate().getSubjectX500Principal( ) );
    }
    return certificate.getPrincipal();
  }

  @Override
  public UserPrincipal lookupPrincipalByCanonicalId( final String canonicalId ) throws AuthException {
    return Accounts.userAsPrincipal( Accounts.lookupAccountByCanonicalId( canonicalId ).lookupAdmin( ) );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumber( final String accountNumber ) throws AuthException {
    return Accounts.userAsPrincipal( Accounts.lookupAccountById( accountNumber ).lookupAdmin( ) );
  }

  private UserPrincipal decorateCredentials( final UserPrincipal userPrincipal,
                                             final String nonce,
                                             final String secret ) throws AuthException {
    final UserPrincipal decorated;
    if ( nonce == null ) {
      decorated = userPrincipal;
    } else {
      final String secretKey = SecurityTokenManager.generateSecret( nonce, secret );
      final Collection<AccessKey> keys = Collections.<AccessKey>singleton( new AccessKey( ) {
        @Override public Boolean isActive( ) { return true; }
        @Override public void setActive( final Boolean active ) { }
        @Override public String getAccessKey( ) { return null; }
        @Override public String getSecretKey( ) { return secretKey; }
        @Override public Date getCreateDate( ) { return null; }
        @Override public UserPrincipal getPrincipal( ) { return null; }
      } );
      decorated = new UserPrincipalImpl( userPrincipal, keys );
    }
    return decorated;
  }
}
