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
package com.eucalyptus.auth.euare.persist;

import static com.eucalyptus.auth.principal.Certificate.Util.revoked;
import static com.eucalyptus.auth.principal.Certificate.Util.x509Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.euare.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.InvalidAccessKeyAuthException;
import com.eucalyptus.auth.api.IdentityProvider;
import com.eucalyptus.auth.euare.EuareServerCertificateUtil;
import com.eucalyptus.auth.euare.persist.entities.ReservedNameEntity;
import com.eucalyptus.auth.euare.principal.GlobalNamespace;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.euare.principal.EuareInstanceProfile;
import com.eucalyptus.auth.principal.EuareRole;
import com.eucalyptus.auth.principal.EuareUser;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.auth.principal.UserPrincipalImpl;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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
    final EuareRole role = Accounts.lookupRoleById( roleId );
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
    final Certificate certificate = Accounts.lookupCertificateByHashId( certificateId );
    if ( !certificate.isActive( ) ) {
      throw new AuthException( "Certificate is inactive or revoked: " + certificate.getX509Certificate().getSubjectX500Principal( ) );
    }
    return certificate.getPrincipal();
  }

  @Override
  public UserPrincipal lookupPrincipalByCanonicalId( final String canonicalId ) throws AuthException {
    return Accounts.userAsPrincipal( Accounts.lookupAccountByCanonicalId( canonicalId ).lookupAdmin() );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumber( final String accountNumber ) throws AuthException {
    return Accounts.userAsPrincipal( Accounts.lookupAccountById( accountNumber ).lookupAdmin() );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumberAndUsername(
      final String accountNumber,
      final String name
  ) throws AuthException {
    return Accounts.userAsPrincipal( Accounts.lookupAccountById( accountNumber ).lookupUserByName( name ) );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByAlias( final String alias ) throws AuthException {
    return Accounts.lookupAccountByName( alias );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByCanonicalId( final String canonicalId ) throws AuthException {
    return Accounts.lookupAccountByCanonicalId( canonicalId );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByEmail( final String email ) throws AuthException {
    final EuareUser euareUser = Accounts.lookupUserByEmailAddress( email );
    if ( euareUser.isAccountAdmin( ) ) {
      return euareUser.getAccount();
    }
    throw new AuthException( AuthException.NO_SUCH_USER );
  }

  @Override
  public List<AccountIdentifiers> listAccountIdentifiersByAliasMatch( final String aliasExpression ) throws AuthException {
    return Accounts.resolveAccountNumbersForName( aliasExpression );
  }

  @Override
  public InstanceProfile lookupInstanceProfileByName( final String accountNumber, final String name ) throws AuthException {
    final EuareInstanceProfile profile = Accounts.lookupAccountById( accountNumber ).lookupInstanceProfileByName( name );
    final String profileArn = Accounts.getInstanceProfileArn( profile );
    final EuareRole euareRole = profile.getRole( );
    final String roleArn = euareRole == null ? null : Accounts.getRoleArn( euareRole );
    final String roleAccountNumber = euareRole == null ? null : euareRole.getAccountNumber( );
    final PolicyVersion rolePolicy = euareRole == null ? null : euareRole.getPolicy( );
    final Role role = euareRole == null ? null : new Role( ) {
      @Override public String getAccountNumber( ) { return roleAccountNumber; }
      @Override public String getRoleId( ) { return euareRole.getRoleId( ); }
      @Override public String getRoleArn( ) { return roleArn; }
      @Override public String getPath( ) { return euareRole.getPath( ); }
      @Override public String getName( ) { return euareRole.getName( ); }
      @Override public String getSecret( ) { return euareRole.getSecret( ); }
      @Override public PolicyVersion getPolicy( ) { return rolePolicy; }
      @Override public String getDisplayName( ) { return Accounts.getRoleFullName( this ); }
      @Override public OwnerFullName getOwner( ) { return euareRole.getOwner( ); }
    };
    return new InstanceProfile( ) {
      @Override public String getAccountNumber( ) { return accountNumber; }
      @Override public String getInstanceProfileId( ) { return profile.getInstanceProfileId( ); }
      @Override public String getInstanceProfileArn( ) { return profileArn; }
      @Nullable
      @Override public Role getRole( ) { return role; }
      @Override public String getName( ) { return profile.getName( ); }
      @Override public String getPath( ) { return profile.getPath(); }
    };
  }

  @Override
  public Role lookupRoleByName( final String accountNumber, final String name ) throws AuthException {
    final EuareRole euareRole = Accounts.lookupAccountById( accountNumber ).lookupRoleByName( name );
    final String roleArn = Accounts.getRoleArn( euareRole );
    final String roleAccountNumber = euareRole.getAccountNumber( );
    final PolicyVersion assumeRolePolicy = euareRole.getPolicy( );
    return new Role( ) {
      @Override public String getAccountNumber( ) { return roleAccountNumber; }
      @Override public String getRoleId( ) { return euareRole.getRoleId( ); }
      @Override public String getRoleArn( ) { return roleArn; }
      @Override public String getPath( ) { return euareRole.getPath( ); }
      @Override public String getName( ) { return euareRole.getName(); }
      @Override public String getSecret( ) { return euareRole.getSecret( ); }
      @Override public PolicyVersion getPolicy( ) { return assumeRolePolicy; }
      @Override public String getDisplayName( ) { return Accounts.getRoleFullName( this ); }
      @Override public OwnerFullName getOwner( ) { return euareRole.getOwner(); }
    };
  }

  @Override
  public SecurityTokenContent decodeSecurityToken( final String accessKeyIdentifier,
                                                   final String securityToken ) throws AuthException {
    return SecurityTokenManager.decodeSecurityToken( accessKeyIdentifier, securityToken );
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

  @Override
  public void reserveGlobalName( final String namespace,
                                 final String name,
                                 final Integer duration ) throws AuthException {
    final GlobalNamespace globalNamespace;
    try {
      globalNamespace = GlobalNamespace.forNamespace( namespace );
    } catch ( IllegalArgumentException e ) {
      throw new AuthException( e );
    }

    if ( duration == null || duration < 1 || duration > TimeUnit.DAYS.toSeconds( 1 ) ) {
      throw new AuthException( "Requested duration not supported: " + duration );
    }

    try ( final TransactionResource tx = Entities.transactionFor( ReservedNameEntity.class ) ) {
      Entities.persist( ReservedNameEntity.create( namespace, name, duration ) );
      tx.commit();
    } catch ( ConstraintViolationException e ) {
      throw new AuthException( AuthException.CONFLICT );
    } catch ( final Exception e ) {
      throw new AuthException( e );
    }

    switch ( globalNamespace ) {
      case Account_Alias:
        try {
          Accounts.lookupAccountByName( name );
          throw new AuthException( AuthException.CONFLICT );
        } catch ( AuthException e ) {
          if ( !AuthException.NO_SUCH_ACCOUNT.equals(  e.getMessage() ) ) {
            throw new AuthException( AuthException.CONFLICT );
          }
        }
        break;
      case Signing_Certificate_Id:
        try {
          final Certificate certificate = Accounts.lookupCertificateById( name );
          if ( !certificate.isRevoked( ) ) {
            throw new AuthException( AuthException.CONFLICT );
          }
        } catch ( AuthException e ) {
          if ( !AuthException.NO_SUCH_CERTIFICATE.equals(  e.getMessage() ) ) {
            throw new AuthException( AuthException.CONFLICT );
          }
        }
        break;
      default:
        throw new AuthException( AuthException.CONFLICT );
    }
  }

  @Override
  public List<X509Certificate> lookupAccountCertificatesByAccountNumber( final String accountNumber ) throws AuthException {
    final List<X509Certificate> certificates = Lists.newArrayList( );
    for ( final User user : Accounts.lookupAccountById( accountNumber ).getUsers( ) ) {
      Iterables.addAll( certificates, Iterables.transform(
          Iterables.filter(
              user.getCertificates(),
              CollectionUtils.propertyPredicate( false, revoked( ) ) ),
          x509Certificate( ) ) );
    }
    return certificates;
  }

  @Override
  public X509Certificate getCertificateByAccountNumber( final String accountNumber ) {
    return SystemCredentials.lookup( Euare.class ).getCertificate( );
  }

  @Override
  public X509Certificate signCertificate(
      final String accountNumber,
      final RSAPublicKey publicKey,
      final String principal,
      final int expiryInDays
  ) throws AuthException {
    return EuareServerCertificateUtil.generateVMCertificate( publicKey, principal, expiryInDays );
  }
}
