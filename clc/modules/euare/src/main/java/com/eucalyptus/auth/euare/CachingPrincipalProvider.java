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

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.AuthenticationProperties;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.Parameters;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;

/**
 *
 */
public class CachingPrincipalProvider extends RegionDelegatingPrincipalProvider {

  private final static AtomicReference<Pair<String,Cache<PrincipalCacheKey,UserPrincipal>>> cacheReference =
      new AtomicReference<>( );

  @Override
  public UserPrincipal lookupCachedPrincipalByUserId( final String userId, final String nonce ) throws AuthException {
    return cache( new UserIdPrincipalCacheKey( userId, nonce ), new Callable<UserPrincipal>( ) {
      @Nonnull
      @Override
      public UserPrincipal call( ) throws AuthException {
        return CachingPrincipalProvider.super.lookupCachedPrincipalByUserId( userId, nonce );
      }
    } );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByRoleId( final String roleId, final String nonce ) throws AuthException {
    return cache( new RoleIdPrincipalCacheKey( roleId, nonce ), new Callable<UserPrincipal>( ) {
      @Nonnull
      @Override
      public UserPrincipal call( ) throws AuthException {
        return CachingPrincipalProvider.super.lookupCachedPrincipalByRoleId( roleId, nonce );
      }
    } );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByAccessKeyId( final String keyId, final String nonce ) throws AuthException {
    return cache( new AccessKeyIdPrincipalCacheKey( keyId, nonce ), new Callable<UserPrincipal>( ) {
      @Nonnull
      @Override
      public UserPrincipal call( ) throws AuthException {
        return CachingPrincipalProvider.super.lookupCachedPrincipalByAccessKeyId( keyId, nonce );
      }
    } );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByCertificateId( final String certificateId ) throws AuthException {
    return cache( new CertificateIdPrincipalCacheKey( certificateId ), new Callable<UserPrincipal>( ) {
      @Nonnull
      @Override
      public UserPrincipal call( ) throws AuthException {
        return CachingPrincipalProvider.super.lookupCachedPrincipalByCertificateId( certificateId );
      }
    } );
  }

  public UserPrincipal lookupCachedPrincipalByAccountNumber( final String accountNumber ) throws AuthException {
    return cache( new AccountNumberPrincipalCacheKey( accountNumber ), new Callable<UserPrincipal>( ) {
      @Nonnull
      @Override
      public UserPrincipal call( ) throws AuthException {
        return CachingPrincipalProvider.super.lookupCachedPrincipalByAccountNumber( accountNumber );
      }
    } );
  }

  private UserPrincipal cache(
      final PrincipalCacheKey key,
      final Callable<UserPrincipal> invoker ) throws AuthException {
    try {
      return cache( ).get( key, invoker );
    } catch ( final ExecutionException e ) {
      if ( e.getCause( ) instanceof AuthException ) {
        throw (AuthException) e.getCause( );
      } else {
        throw new AuthException( e );
      }
    }
  }

  private static Cache<PrincipalCacheKey,UserPrincipal> cache( ) {
    Cache<PrincipalCacheKey,UserPrincipal> cache;
    final Pair<String,Cache<PrincipalCacheKey,UserPrincipal>> cachePair = cacheReference.get( );
    final String cacheSpec = AuthenticationProperties.AUTHORIZATION_CACHE;
    if ( cachePair == null || !cacheSpec.equals( cachePair.getLeft( ) ) ) {
      final Pair<String,Cache<PrincipalCacheKey,UserPrincipal>> newCachePair =
          Pair.pair( cacheSpec, cache( cacheSpec ) );
      if ( cacheReference.compareAndSet( cachePair, newCachePair ) || cachePair == null ) {
        cache = newCachePair.getRight( );
      } else {
        cache = cachePair.getRight( );
      }
    } else {
      cache = cachePair.getRight( );
    }
    return cache;
  }

  private static Cache<PrincipalCacheKey,UserPrincipal> cache( final String cacheSpec ) {
    return CacheBuilder
        .from( CacheBuilderSpec.parse( cacheSpec ) )
        .build( );
  }

  private static abstract class PrincipalCacheKey {
    @Nonnull  private final String identifier;
    @Nullable private final String nonce;

    protected PrincipalCacheKey(
        @Nonnull  final String identifier,
        @Nullable final String nonce
    ) {
      Parameters.checkParam( "identifier", identifier, not( isEmptyOrNullString( ) ) );
      this.identifier = identifier;
      this.nonce = nonce;
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final PrincipalCacheKey that = (PrincipalCacheKey) o;

      if ( !identifier.equals( that.identifier ) ) return false;
      if ( nonce != null ? !nonce.equals( that.nonce ) : that.nonce != null ) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = identifier.hashCode();
      result = 31 * result + ( nonce != null ? nonce.hashCode() : 0 );
      return result;
    }
  }

  private static final class UserIdPrincipalCacheKey extends PrincipalCacheKey {
    protected UserIdPrincipalCacheKey(
        @Nonnull final String identifier,
        @Nullable final String nonce
    ) {
      super( identifier, nonce );
    }
  }

  private static final class RoleIdPrincipalCacheKey extends PrincipalCacheKey {
    protected RoleIdPrincipalCacheKey(
        @Nonnull final String identifier,
        @Nullable final String nonce
    ) {
      super( identifier, nonce );
    }
  }

  private static final class AccessKeyIdPrincipalCacheKey extends PrincipalCacheKey {
    protected AccessKeyIdPrincipalCacheKey(
        @Nonnull final String identifier,
        @Nullable final String nonce
    ) {
      super( identifier, nonce );
    }
  }

  private static final class CertificateIdPrincipalCacheKey extends PrincipalCacheKey {
    protected CertificateIdPrincipalCacheKey(
        @Nonnull final String identifier
    ) {
      super( identifier, null );
    }
  }

  private static final class AccountNumberPrincipalCacheKey extends PrincipalCacheKey {
    protected AccountNumberPrincipalCacheKey(
        @Nonnull final String identifier
    ) {
      super( identifier, null );
    }
  }
}
