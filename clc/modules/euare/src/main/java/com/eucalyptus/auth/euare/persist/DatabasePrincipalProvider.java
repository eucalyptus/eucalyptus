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

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.persistence.PersistenceException;
import org.hibernate.FlushMode;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.auth.AccessKeys;
import com.eucalyptus.auth.euare.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.InvalidAccessKeyAuthException;
import com.eucalyptus.auth.api.PrincipalProvider;
import com.eucalyptus.auth.euare.EuareServerCertificateUtil;
import com.eucalyptus.auth.euare.persist.entities.AccessKeyEntity;
import com.eucalyptus.auth.euare.persist.entities.AccessKeyEntity_;
import com.eucalyptus.auth.euare.persist.entities.AccountEntity;
import com.eucalyptus.auth.euare.persist.entities.CertificateEntity;
import com.eucalyptus.auth.euare.persist.entities.InstanceProfileEntity;
import com.eucalyptus.auth.euare.persist.entities.OpenIdProviderEntity;
import com.eucalyptus.auth.euare.persist.entities.ReservedNameEntity;
import com.eucalyptus.auth.euare.persist.entities.RoleEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity;
import com.eucalyptus.auth.euare.persist.entities.UserEntity_;
import com.eucalyptus.auth.euare.principal.EuareOpenIdConnectProvider;
import com.eucalyptus.auth.euare.principal.EuareRole;
import com.eucalyptus.auth.euare.principal.EuareUser;
import com.eucalyptus.auth.euare.principal.GlobalNamespace;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.euare.principal.EuareInstanceProfile;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.OpenIdConnectProvider;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.auth.euare.UserPrincipalImpl;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 *
 */
@SuppressWarnings( { "unused", "Guava", "StaticPseudoFunctionalStyleMethod" } )
@ComponentNamed( "localPrincipalProvider" )
public class DatabasePrincipalProvider implements PrincipalProvider {

  @Override
  public UserPrincipal lookupPrincipalByUserId( final String userId, final String nonce ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( UserEntity.class ) ) {
      try {
        final UserEntity user = DatabaseAuthUtils.getUnique( UserEntity.class, UserEntity_.userId, userId );
        return decorateCredentials( new UserPrincipalImpl( user ), nonce, user.getToken( ) );
      } catch ( Exception e ) {
        throw new AuthException( AuthException.NO_SUCH_USER, e );
      }
    }
  }

  @Override
  public UserPrincipal lookupPrincipalByRoleId( final String roleId, final String nonce ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( RoleEntity.class ) ) {
      final EuareRole role = Accounts.lookupRoleById( Accounts.getIdentifier( roleId ) );
      return decorateCredentials( Accounts.roleAsPrincipal( role, Accounts.getIdentifierSuffix( roleId ) ), nonce, role.getSecret() );
    }
  }

  @Override
  public UserPrincipal lookupPrincipalByAccessKeyId( final String keyId, final String nonce ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( AccessKeyEntity.class ) ) {
      final Optional<UserEntity> user;
      try {
        user = Entities.criteriaQuery( UserEntity.class )
            .join( UserEntity_.keys ).whereEqual( AccessKeyEntity_.accessKey, keyId )
            .entityCriteriaQuery( )
            .fetchSize( FlushMode.MANUAL )
            .readonly( )
            .uniqueResultOption( );
      } catch ( Exception e ) {
        throw new InvalidAccessKeyAuthException( "Failed to find access key", e );
      }
      if ( !user.isPresent( ) ) {
        throw new InvalidAccessKeyAuthException( "Failed to find access key" );
      }
      final UserPrincipal principal = new UserPrincipalImpl( user.get( ) );
      final Optional<AccessKey> accessKey = Iterables.tryFind(
          principal.getKeys( ),
          CollectionUtils.propertyPredicate( keyId, AccessKeys.accessKeyIdentifier( ) ) );
      if ( !Iterables.any( accessKey.asSet( ), AccessKeys.isActive( ) ) ) {
        throw new InvalidAccessKeyAuthException( "Invalid access key or token" );
      }
      return decorateCredentials( principal, nonce, accessKey.get( ).getSecretKey() );
    }
  }

  @Override
  public UserPrincipal lookupPrincipalByCertificateId( final String certificateId ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( CertificateEntity.class ) ) {
      final Certificate certificate = Accounts.lookupCertificateByHashId( certificateId );
      if ( !certificate.isActive( ) ) {
        throw new AuthException( "Certificate is inactive or revoked: " + certificate.getX509Certificate( ).getSubjectX500Principal( ) );
      }
      return certificate.getPrincipal();
    }
  }

  @Override
  public UserPrincipal lookupPrincipalByCanonicalId( final String canonicalId ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( UserEntity.class ) ) {
      return Accounts.userAsPrincipal( Accounts.lookupAccountByCanonicalId( canonicalId ).lookupAdmin() );
    }
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumber( final String accountNumber ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( UserEntity.class ) ) {
      return Accounts.userAsPrincipal( Accounts.lookupAccountById( accountNumber ).lookupAdmin() );
    }
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumberAndUsername(
      final String accountNumber,
      final String name
  ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( UserEntity.class ) ) {
      return Accounts.userAsPrincipal( Accounts.lookupAccountById( accountNumber ).lookupUserByName( name ) );
    }
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByUserId( final UserPrincipal cached, final String userId, final String nonce ) throws AuthException {
    return lookupPrincipalByUserId( userId, nonce );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByRoleId( final UserPrincipal cached, final String roleId, final String nonce ) throws AuthException {
    return lookupPrincipalByRoleId( roleId, nonce );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByAccessKeyId( final UserPrincipal cached, final String keyId, final String nonce ) throws AuthException {
    return lookupPrincipalByAccessKeyId( keyId, nonce );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByCertificateId( final UserPrincipal cached, final String certificateId ) throws AuthException {
    return lookupPrincipalByCertificateId( certificateId );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByAccountNumber( final UserPrincipal cached, final String accountNumber ) throws AuthException {
    return lookupPrincipalByAccountNumber( accountNumber );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByAlias( final String alias ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( AccountEntity.class ) ) {
      return Accounts.lookupAccountByName( alias );
    }
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByCanonicalId( final String canonicalId ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( AccountEntity.class ) ) {
      return Accounts.lookupAccountByCanonicalId( canonicalId );
    }
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByEmail( final String email ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( UserEntity.class ) ) {
      final EuareUser euareUser = Accounts.lookupUserByEmailAddress( email );
      if ( euareUser.isAccountAdmin( ) ) {
        return euareUser.getAccount();
      }
      throw new AuthException( AuthException.NO_SUCH_USER );
    }
  }

  @Override
  public List<AccountIdentifiers> listAccountIdentifiersByAliasMatch( final String aliasExpression ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( AccountEntity.class ) ) {
      return Accounts.resolveAccountNumbersForName( aliasExpression );
    }
  }

  @Override
  public InstanceProfile lookupInstanceProfileByName( final String accountNumber, final String name ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( InstanceProfileEntity.class ) ) {
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
  }

  @Override
  public Role lookupRoleByName( final String accountNumber, final String name ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( RoleEntity.class ) ) {
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
  }

  @Override
  public OpenIdConnectProvider lookupOidcProviderByUrl( final String accountNumber, final String url ) throws AuthException {
    try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( OpenIdProviderEntity.class ) ) {
      final EuareOpenIdConnectProvider euareProvider = Accounts.lookupAccountById( accountNumber ).lookupOpenIdConnectProvider( url );
      final String providerArn = Accounts.getOpenIdConnectProviderArn( euareProvider );
      final String providerAccountNumber = euareProvider.getAccountNumber( );
      final List<String> providerClientIds = ImmutableList.copyOf( euareProvider.getClientIds( ) );
      final List<String> providerThumbprints = ImmutableList.copyOf( euareProvider.getThumbprints( ) );
      return new OpenIdConnectProvider( ) {
        @Override public String getAccountNumber( ) { return providerAccountNumber; }
        @Override public String getArn( ) { return providerArn; }
        @Override public String getUrl( ) { return euareProvider.getUrl( ); }
        @Override public String getHost( ) { return euareProvider.getHost( ); }
        @Override public Integer getPort( ) { return euareProvider.getPort( ); }
        @Override public String getPath( ) { return euareProvider.getPath( ); }
        @Override public List<String> getClientIds( ) { return providerClientIds; }
        @Override public List<String> getThumbprints( ) { return providerThumbprints; }
      };
    }
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
                                 final Integer duration,
                                 final String clientToken ) throws AuthException {
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
      Entities.persist( ReservedNameEntity.create( namespace, name, duration, Strings.emptyToNull( clientToken ) ) );
      tx.commit( );
    } catch ( ConstraintViolationException e ) {
      boolean conflict = true;
      if ( !Strings.isNullOrEmpty( clientToken ) ) try ( final TransactionResource tx = Entities.readOnlyDistinctTransactionFor( ReservedNameEntity.class ) ) {
        // use the existing reservation for the token if it matches and
        // has half the duration remaining
        final ReservedNameEntity entity = Entities.criteriaQuery( ReservedNameEntity.exampleWithToken( clientToken ) ).uniqueResult( );
        conflict = !entity.getNamespace( ).equals( namespace ) ||
            !entity.getName( ).equals( name ) ||
            entity.getExpiry( ).before( new Date( System.currentTimeMillis( ) + TimeUnit.SECONDS.toMillis( duration / 2  ) ) );
      } catch ( PersistenceException|NoSuchElementException e1 ) {
        // fail with conflict
      }
      if ( conflict ) {
        throw new AuthException( AuthException.CONFLICT );
      }
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
          Accounts.lookupCertificateById( name );
          throw new AuthException( AuthException.CONFLICT );
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
