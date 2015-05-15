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
package com.eucalyptus.auth.euare;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.persist.DatabaseIdentityProvider;
import com.eucalyptus.auth.api.IdentityProvider;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurationManager;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.auth.euare.identity.region.RegionInfo;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Either;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.NonNullFunction;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 *
 */
public class RegionDelegatingIdentityProvider implements IdentityProvider {

  private final IdentityProvider localProvider = new DatabaseIdentityProvider( );
  private final RegionConfigurationManager regionConfigurationManager = new RegionConfigurationManager( );

  @Override
  public UserPrincipal lookupPrincipalByUserId( final String userId, final String nonce ) throws AuthException {
    return regionDispatchByIdentifier( userId, new NonNullFunction<IdentityProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.lookupPrincipalByUserId( userId, nonce );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByRoleId( final String roleId, final String nonce ) throws AuthException {
    return regionDispatchByIdentifier( roleId, new NonNullFunction<IdentityProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.lookupPrincipalByRoleId( roleId, nonce );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccessKeyId( final String keyId, final String nonce ) throws AuthException {
    return regionDispatchByIdentifier( keyId, new NonNullFunction<IdentityProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.lookupPrincipalByAccessKeyId( keyId, nonce );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByCertificateId( final String certificateId ) throws AuthException {
    return regionDispatchAndReduce( new NonNullFunction<IdentityProvider, Either<AuthException, UserPrincipal>>() {
      @Nonnull
      @Override
      public Either<AuthException, UserPrincipal> apply( final IdentityProvider identityProvider ) {
        try {
          return Either.right( identityProvider.lookupPrincipalByCertificateId( certificateId ) );
        } catch ( AuthException e ) {
          return Either.left( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByCanonicalId( final String canonicalId ) throws AuthException {
    return regionDispatchAndReduce( new NonNullFunction<IdentityProvider, Either<AuthException, UserPrincipal>>() {
      @Nonnull
      @Override
      public Either<AuthException, UserPrincipal> apply( final IdentityProvider identityProvider ) {
        try {
          return Either.right( identityProvider.lookupPrincipalByCanonicalId( canonicalId ) );
        } catch ( AuthException e ) {
          return Either.left( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumber( final String accountNumber ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<IdentityProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.lookupPrincipalByAccountNumber( accountNumber );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumberAndUsername(
      final String accountNumber,
      final String name
  ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<IdentityProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.lookupPrincipalByAccountNumberAndUsername( accountNumber, name );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByAlias( final String alias ) throws AuthException {
    return regionDispatchAndReduce( new NonNullFunction<IdentityProvider, Either<AuthException, AccountIdentifiers>>() {
      @Nonnull
      @Override
      public Either<AuthException, AccountIdentifiers> apply( final IdentityProvider identityProvider ) {
        try {
          return Either.right( identityProvider.lookupAccountIdentifiersByAlias( alias ) );
        } catch ( AuthException e ) {
          return Either.left( e );
        }
      }
    } );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByCanonicalId( final String canonicalId ) throws AuthException {
    return regionDispatchAndReduce( new NonNullFunction<IdentityProvider, Either<AuthException,AccountIdentifiers>>( ) {
      @Nonnull
      @Override
      public Either<AuthException,AccountIdentifiers> apply( final IdentityProvider identityProvider ) {
        try {
          return Either.right( identityProvider.lookupAccountIdentifiersByCanonicalId( canonicalId ) );
        } catch ( AuthException e ) {
          return Either.left( e );
        }
      }
    } );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByEmail( final String email ) throws AuthException {
    return regionDispatchAndReduce( new NonNullFunction<IdentityProvider, Either<AuthException,AccountIdentifiers>>( ) {
      @Nonnull
      @Override
      public Either<AuthException,AccountIdentifiers> apply( final IdentityProvider identityProvider ) {
        try {
          return Either.right( identityProvider.lookupAccountIdentifiersByEmail( email ) );
        } catch ( AuthException e ) {
          return Either.left( e );
        }
      }
    } );
  }

  @Override
  public List<AccountIdentifiers> listAccountIdentifiersByAliasMatch( final String aliasExpression ) throws AuthException {
    return regionDispatchAndReduce( new NonNullFunction<IdentityProvider, Either<AuthException,List<AccountIdentifiers>>>( ) {
      @Nonnull
      @Override
      public Either<AuthException,List<AccountIdentifiers>> apply( final IdentityProvider identityProvider ) {
        try {
          return Either.right( identityProvider.listAccountIdentifiersByAliasMatch( aliasExpression ) );
        } catch ( AuthException e ) {
          return Either.left( e );
        }
      }
    }, Lists.<AccountIdentifiers>newArrayList( ), CollectionUtils.<AccountIdentifiers,List<AccountIdentifiers>>addAll( ) );
  }

  @Override
  public InstanceProfile lookupInstanceProfileByName( final String accountNumber, final String name ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<IdentityProvider, InstanceProfile>() {
      @Nonnull
      @Override
      public InstanceProfile apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.lookupInstanceProfileByName( accountNumber, name );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public Role lookupRoleByName( final String accountNumber, final String name ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<IdentityProvider, Role>() {
      @Nonnull
      @Override
      public Role apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.lookupRoleByName( accountNumber, name );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public SecurityTokenContent decodeSecurityToken( final String accessKeyIdentifier,
                                                   final String securityToken ) throws AuthException {
    return regionDispatchByIdentifier( accessKeyIdentifier, new NonNullFunction<IdentityProvider, SecurityTokenContent>() {
      @Nonnull
      @Override
      public SecurityTokenContent apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.decodeSecurityToken( accessKeyIdentifier, securityToken );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public void reserveGlobalName( final String namespace,
                                 final String name,
                                 final Integer duration ) throws AuthException {
    final int numberOfRegions = Iterables.size( regionConfigurationManager.getRegionInfos( ) );
    final Integer successes = numberOfRegions <= 1 ?
        1 : // skip if only a local region
        regionDispatchAndReduce( new NonNullFunction<IdentityProvider, Either<AuthException,String>>( ) {
          @Nonnull
          @Override
          public Either<AuthException,String> apply( final IdentityProvider identityProvider ) {
            try {
              identityProvider.reserveGlobalName( namespace, name, duration );
              return Either.right( "" );
            } catch ( AuthException e ) {
              return Either.left( e );
            }
          }
        }, 0, CollectionUtils.<String>count( Predicates.notNull( ) ) );
    if ( successes < ( 1 + ( numberOfRegions / 2 ) ) ) {
      throw new AuthException( AuthException.CONFLICT );
    }
  }

  @Override
  public List<X509Certificate> lookupAccountCertificatesByAccountNumber( final String accountNumber ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<IdentityProvider, List<X509Certificate>>() {
      @Nonnull
      @Override
      public List<X509Certificate> apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.lookupAccountCertificatesByAccountNumber( accountNumber );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public X509Certificate getCertificateByAccountNumber( final String accountNumber ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<IdentityProvider, X509Certificate>() {
      @Nonnull
      @Override
      public X509Certificate apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.getCertificateByAccountNumber( accountNumber );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public X509Certificate signCertificate(
      final String accountNumber,
      final RSAPublicKey publicKey,
      final String principal,
      final int expiryInDays
  ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<IdentityProvider, X509Certificate>() {
      @Nonnull
      @Override
      public X509Certificate apply( final IdentityProvider identityProvider ) {
        try {
          return identityProvider.signCertificate( accountNumber, publicKey, principal, expiryInDays );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  private <R> R regionDispatchByIdentifier(
      final String identifier,
      final NonNullFunction<IdentityProvider, R> invoker ) throws AuthException {
    return regionDispatch( regionConfigurationManager.getRegionByIdentifier( identifier ), invoker );
  }

  private <R> R regionDispatchByAccountNumber(
      final String accountNumber,
      final NonNullFunction<IdentityProvider, R> invoker ) throws AuthException {
    return regionDispatch( regionConfigurationManager.getRegionByAccountNumber( accountNumber ), invoker );
  }

  private <R> R regionDispatch(
      final Optional<RegionInfo> regionInfo,
      final NonNullFunction<IdentityProvider, R> invoker
  ) throws AuthException {
    try {
      if ( regionInfo.isPresent( ) &&
          !RegionConfigurations.getRegionName( ).asSet( ).contains( regionInfo.get( ).getName( ) ) ) {
        final Optional<Set<String>> endpoints = regionInfo.transform( RegionInfo.serviceEndpoints( "identity" ) );
        if ( endpoints.isPresent( ) && !endpoints.get( ).isEmpty( ) ) {
          final IdentityProvider remoteProvider = new RemoteIdentityProvider( endpoints.get( ) );
          return invoker.apply( remoteProvider );
        }
        return invoker.apply( localProvider );
      } else {
        return invoker.apply( localProvider );
      }
    } catch ( final RuntimeException e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw e;
    }
  }

  private <R> R regionDispatchAndReduce(
      final NonNullFunction<IdentityProvider, Either<AuthException,R>> invoker
  ) throws AuthException {
    return regionDispatchAndReduce( invoker, null, CollectionUtils.<R>unique( ) );
  }

  private <R,I> I regionDispatchAndReduce(
      final NonNullFunction<IdentityProvider, Either<AuthException,R>> invoker,
      final I initial,
      final Function<I,Function<R,I>> reducer
  ) throws AuthException {
    try {
      final Iterable<RegionInfo> regionInfos = regionConfigurationManager.getRegionInfos( );
      final List<Either<AuthException,R>> regionResults = Lists.newArrayList( );
      regionResults.add( invoker.apply( localProvider ) );
      if ( !Iterables.isEmpty( regionInfos ) ) {
        withRegions:
        for ( final RegionInfo regionInfo : regionInfos ) {
          if ( !RegionConfigurations.getRegionName( ).asSet( ).contains( regionInfo.getName( ) ) ) {
            for ( final RegionInfo.RegionService service : regionInfo.getServices( ) ) {
              if ( "identity".equals( service.getType( ) ) ) {
                final IdentityProvider remoteProvider = new RemoteIdentityProvider( service.getEndpoints( ) );
                regionResults.add( invoker.apply( remoteProvider ) );
                continue withRegions;
              }
            }
          }
        }
      }
      //TODO:STEVE: check error codes to ensure failure due to not found only? (or catch more specific exception for either)
      final Iterable<R> successResults = Optional.presentInstances( Iterables.transform(
          regionResults,
          Either.<AuthException,R>rightOption( ) ) );
      if ( Iterables.size( successResults ) > 0 ) {
        return CollectionUtils.reduce( successResults, initial, reducer );
      }
      throw Iterables.get(
          Optional.presentInstances( Iterables.transform( regionResults, Either.<AuthException,R>leftOption( ) ) ),
          0 );
    } catch ( final RuntimeException e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw e;
    }

  }
}
