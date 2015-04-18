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

import java.util.List;
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
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.Either;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.NonNullFunction;
import com.google.common.base.Optional;
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
    return localProvider.lookupPrincipalByCertificateId( certificateId ); //TODO:STEVE: remote for certificate lookup
  }

  @Override
  public UserPrincipal lookupPrincipalByCanonicalId( final String canonicalId ) throws AuthException {
    if ( regionConfigurationManager.getRegionInfo().isPresent() && Contexts.exists( ) && Contexts.lookup( ).getUser( ).getCanonicalId( ).equals( canonicalId ) ) {
      return Contexts.lookup( ).getUser( ); //TODO:STEVE: remove this temporary lookup hack
    }
    return localProvider.lookupPrincipalByCanonicalId( canonicalId ); //TODO:STEVE: remote for canonical id lookup
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
    return regionDispatchAndReduce( new NonNullFunction<IdentityProvider, Either<AuthException,AccountIdentifiers>>( ) {
      @Nonnull
      @Override
      public Either<AuthException,AccountIdentifiers> apply( final IdentityProvider identityProvider ) {
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
        for ( final RegionInfo.RegionService service : regionInfo.get( ).getServices( ) ) {
          if ( "identity".equals( service.getType( ) ) ) {
            final IdentityProvider remoteProvider = new RemoteIdentityProvider( service.getEndpoints( ) );
            return invoker.apply( remoteProvider );
          }
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
      if ( Iterables.size( successResults ) == 1 ) {
        return Iterables.getOnlyElement( successResults );
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
