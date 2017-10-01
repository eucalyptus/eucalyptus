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
package com.eucalyptus.auth;

import static com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.eucalyptus.util.CollectionUtils;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.vavr.control.Option;

/**
 *
 */
public class AccessKeys {

  /**
   * Lookup an ephemeral or persistent access key with caching.
   *
   * @param accessKeyId Required access key identifier
   * @param securityToken Optional security token
   * @return The access key
   * @throws AuthException If an error occurs
   */
  public static AccessKey lookupAccessKey( @Nonnull  final String accessKeyId,
                                           @Nullable final String securityToken ) throws AuthException {
    return securityToken == null ?
        Iterables.tryFind(
            Accounts.lookupCachedPrincipalByAccessKeyId( accessKeyId, null ).getKeys( ),
            CollectionUtils.propertyPredicate( accessKeyId, accessKeyIdentifier( ) ) ).get( ) :
        SecurityTokenManager.lookupAccessKey( accessKeyId, securityToken );
  }

  /**
   * Predicate for active access keys.
   *
   * @return The predicate
   * @see com.eucalyptus.auth.principal.AccessKey#isActive()
   */
  public static Predicate<AccessKey> isActive() {
    return IS_ACTIVE.INSTANCE;
  }

  /**
   * Function to get the identifier for an access key.
   *
   * @return the function
   */
  public static Function<AccessKey,String> accessKeyIdentifier( ) {
    return AccessKeyStringProperties.IDENTIFIER;
  }

  /**
   * Get the type for an access key.
   *
   * @param key The key to get the type of
   * @return The optional key type
   */
  @Nonnull
  public static Option<TemporaryKeyType> getKeyType( @Nonnull final AccessKey key ) {
    Option<TemporaryKeyType> type = Option.none( );
    if ( key instanceof TemporaryAccessKey) {
      type = Option.some( ((TemporaryAccessKey) key ).getType( ) );
    }
    return type;
  }

  private static enum AccessKeyStringProperties implements Function<AccessKey,String> {
    IDENTIFIER {
      @Nullable
      @Override
      public String apply( @Nullable final AccessKey accessKey ) {
        return accessKey == null ? null : accessKey.getAccessKey( );
      }
    },
  }

  private static enum IS_ACTIVE implements Predicate<AccessKey> {
    INSTANCE;

    @Override
    public boolean apply( final AccessKey accessKey ) {
      return Boolean.TRUE.equals( accessKey.isActive() );
    }
  }

}
