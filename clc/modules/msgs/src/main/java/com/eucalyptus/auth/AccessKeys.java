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
import javaslang.control.Option;

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
