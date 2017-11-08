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
package com.eucalyptus.auth.euare;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
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
import com.eucalyptus.util.async.AsyncExceptions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;

/**
 *
 */
public class CachingPrincipalProvider extends RegionDelegatingPrincipalProvider {

  private final static AtomicReference<Pair<String,Cache<PrincipalCacheKey,PrincipalCacheValue>>> cacheReference =
      new AtomicReference<>( );

  @Override
  public UserPrincipal lookupCachedPrincipalByUserId( final UserPrincipal cached, final String userId, final String nonce ) throws AuthException {
    return cache( new UserIdPrincipalCacheKey( userId, nonce ), new PrincipalLoader( ) {
      @Override
      public UserPrincipal load( final UserPrincipal cached ) throws AuthException {
        return CachingPrincipalProvider.super.lookupCachedPrincipalByUserId( cached, userId, nonce );
      }
    } );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByRoleId( final UserPrincipal cached, final String roleId, final String nonce ) throws AuthException {
    return cache( new RoleIdPrincipalCacheKey( roleId, nonce ), new PrincipalLoader( ) {
      @Override
      public UserPrincipal load( final UserPrincipal cached ) throws AuthException {
        return CachingPrincipalProvider.super.lookupCachedPrincipalByRoleId( cached, roleId, nonce );
      }
    } );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByAccessKeyId( final UserPrincipal cached, final String keyId, final String nonce ) throws AuthException {
    return cache( new AccessKeyIdPrincipalCacheKey( keyId, nonce ), new PrincipalLoader( ) {
      @Override
      public UserPrincipal load( final UserPrincipal cached ) throws AuthException {
        return CachingPrincipalProvider.super.lookupCachedPrincipalByAccessKeyId( cached, keyId, nonce );
      }
    } );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByCertificateId( final UserPrincipal cached, final String certificateId ) throws AuthException {
    return cache( new CertificateIdPrincipalCacheKey( certificateId ), new PrincipalLoader( ) {
      @Override
      public UserPrincipal load( final UserPrincipal cached ) throws AuthException {
        return CachingPrincipalProvider.super.lookupCachedPrincipalByCertificateId( cached, certificateId );
      }
    } );
  }

  public UserPrincipal lookupCachedPrincipalByAccountNumber( final UserPrincipal cached, final String accountNumber ) throws AuthException {
    return cache( new AccountNumberPrincipalCacheKey( accountNumber ), new PrincipalLoader( ) {
      @Override
      public UserPrincipal load( final UserPrincipal cached ) throws AuthException {
        return CachingPrincipalProvider.super.lookupCachedPrincipalByAccountNumber( cached, accountNumber );
      }
    } );
  }

  private UserPrincipal cache(
      final PrincipalCacheKey key,
      final PrincipalLoader loader ) throws AuthException {
    PrincipalCacheValue principalValue = null;
    final Cache<PrincipalCacheKey,PrincipalCacheValue> cache = cache( );
    try {
      principalValue = cache.get( key, loader.callable( null ) );
      if ( principalValue.updated + AuthenticationProperties.getAuthorizationExpiry( ) < System.currentTimeMillis( ) ) {
        cache.invalidate( key ); // invalidate expired and refresh
        principalValue =  cache.get( key, loader.callable( principalValue.principal ) );
      }
      return principalValue.principal;
    } catch ( final ExecutionException e ) {
      // reuse cached value on failure within configured limit, but not for web service error responses
      if ( !AsyncExceptions.asWebServiceError( e ).isPresent( ) &&
          principalValue != null &&
          principalValue.created + AuthenticationProperties.getAuthorizationReuseExpiry( ) > System.currentTimeMillis( ) ) {
        cache.put( key, new PrincipalCacheValue( principalValue ) );
        return principalValue.principal;
      }
      if ( e.getCause( ) instanceof AuthException ) {
        throw (AuthException) e.getCause( );
      } else {
        throw new AuthException( e );
      }
    }
  }

  private static Cache<PrincipalCacheKey,PrincipalCacheValue> cache( ) {
    Cache<PrincipalCacheKey,PrincipalCacheValue> cache;
    final Pair<String,Cache<PrincipalCacheKey,PrincipalCacheValue>> cachePair = cacheReference.get( );
    final String cacheSpec = AuthenticationProperties.AUTHORIZATION_CACHE;
    if ( cachePair == null || !cacheSpec.equals( cachePair.getLeft( ) ) ) {
      final Pair<String,Cache<PrincipalCacheKey,PrincipalCacheValue>> newCachePair =
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

  private static Cache<PrincipalCacheKey,PrincipalCacheValue> cache( final String cacheSpec ) {
    return CacheBuilder
        .from( CacheBuilderSpec.parse( cacheSpec ) )
        .build( );
  }

  private static abstract class PrincipalLoader {
    abstract UserPrincipal load( UserPrincipal cached ) throws AuthException;

    Callable<PrincipalCacheValue> callable( final UserPrincipal cached ) {
      return new Callable<PrincipalCacheValue>( ) {
        @Override
        public PrincipalCacheValue call( ) throws AuthException {
          return new PrincipalCacheValue( load( cached ) );
        }
      };
    }
  }

  private static final class PrincipalCacheValue {
             private final long created;
             private final long updated;
    @Nonnull private final UserPrincipal principal;

    public PrincipalCacheValue( @Nonnull final UserPrincipal principal ) {
      Parameters.checkParam( "principal", principal, notNullValue( ) );
      this.created = System.currentTimeMillis( );
      this.updated = created;
      this.principal = principal;
    }

    public PrincipalCacheValue( @Nonnull final PrincipalCacheValue value ) {
      Parameters.checkParam( "value", value, notNullValue( ) );
      this.created = value.created;
      this.updated = System.currentTimeMillis( );
      this.principal = value.principal;
    }
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
