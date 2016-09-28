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
package com.eucalyptus.auth.tokens;

import java.util.ServiceLoader;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.TemporaryAccessKey;
import com.eucalyptus.auth.principal.User;

/**
 *
 */
public class SecurityTokenManager {

  private static final SecurityTokenProvider instance =
      ServiceLoader.load( SecurityTokenProvider.class ).iterator( ).next( );

  /**
   * Issue a security token.
   *
   * <p>The token is tied to the provided access key and will be invalid if the
   * underlying access key is disabled or is removed.</p>
   *
   * <p>The credential associated with the token is of type
   * TemporaryAccessKey#Session.</p>
   *
   * @param user The user for the token
   * @param accessKey The originating access key for the token
   * @param durationTruncationSeconds The duration at which to truncate without error
   * @param durationSeconds The desired duration for the token
   * @return The newly issued security token
   * @throws com.eucalyptus.auth.AuthException If an error occurs
   * @see com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType#Session
   */
  @Nonnull
  public static SecurityToken issueSecurityToken( @Nonnull  final User user,
                                                  @Nullable final AccessKey accessKey,
                                                  final int durationTruncationSeconds,
                                                  final int durationSeconds ) throws AuthException {
    return instance.doIssueSecurityToken( user, accessKey, durationTruncationSeconds, durationSeconds );
  }

  /**
   * Issue a security token.
   *
   * <p>The credential associated with the token is of type
   * TemporaryAccessKey#Access.</p>
   *
   * @param user The user for the token
   * @param durationSeconds The desired duration for the token
   * @return The newly issued security token
   * @throws AuthException If an error occurs
   * @see com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType#Access
   */
  @Nonnull
  public static SecurityToken issueSecurityToken( @Nonnull  final User user,
                                                  final int durationSeconds ) throws AuthException {
    return instance.doIssueSecurityToken( user, 0, durationSeconds );
  }

  /**
   * Issue a security token.
   *
   * <p>The credential associated with the token is of type
   * TemporaryAccessKey#Access.</p>
   *
   * @param user The user for the token
   * @param durationTruncationSeconds The duration at which to truncate without error
   * @param durationSeconds The desired duration for the token
   * @return The newly issued security token
   * @throws AuthException If an error occurs
   * @see com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType#Access
   */
  @Nonnull
  public static SecurityToken issueSecurityToken( @Nonnull  final User user,
                                                  final int durationTruncationSeconds,
                                                  final int durationSeconds ) throws AuthException {
    return instance.doIssueSecurityToken( user, durationTruncationSeconds, durationSeconds );
  }

  /**
   * Issue a security token.
   *
   * <p>The credential associated with the token is of type
   * TemporaryAccessKey#Role.</p>
   *
   * @param role The role to to assume
   * @param attributes The role token attributes
   * @param durationSeconds The desired duration for the token
   * @return The newly issued security token
   * @throws AuthException If an error occurs
   * @see com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType#Role
   */
  @Nonnull
  public static SecurityToken issueSecurityToken( @Nonnull final BaseRole role,
                                                  @Nonnull final RoleSecurityTokenAttributes attributes,
                                                  final int durationSeconds ) throws AuthException {
    return instance.doIssueSecurityToken( role, attributes, durationSeconds );
  }

  /**
   * Lookup the access key for a token.
   *
   * @param accessKeyId The identifier for the ephemeral access key
   * @param token The security token for the ephemeral access key
   * @return The access key
   * @throws AuthException If an error occurs
   */
  @Nonnull
  public static TemporaryAccessKey lookupAccessKey( @Nonnull final String accessKeyId,
                                                    @Nonnull final String token ) throws AuthException {
    return instance.doLookupAccessKey(accessKeyId, token);
  }


  /**
   * Generate a secret key access key for the given nonce / secret.
   *
   * @param nonce The security token nonce
   * @param secret The secret source value related to the security token
   * @return The secret key
   * @throws AuthException If an error occurs
   */
  @Nonnull
  public static String generateSecret( @Nonnull final String nonce,
                                       @Nonnull final String secret ) throws AuthException {
    return instance.doGenerateSecret( nonce, secret );
  }

  /**
   * Decode the given token.
   *
   * @param accessKeyId The identifier
   * @param token The token
   * @return The decoded token
   * @throws AuthException if the token cannot be decoded
   */
  @Nonnull
  public static SecurityTokenContent decodeSecurityToken( @Nonnull final String accessKeyId,
                                                          @Nonnull final String token ) throws AuthException {
    return instance.doDecode( accessKeyId, token );
  }

  public interface SecurityTokenProvider {
    SecurityToken doIssueSecurityToken( @Nonnull  final User user,
                                        @Nullable final AccessKey accessKey,
                                        final int durationTruncationSeconds,
                                        final int durationSeconds ) throws AuthException;

    SecurityToken doIssueSecurityToken( @Nonnull  final User user,
                                        final int durationTruncationSeconds,
                                        final int durationSeconds ) throws AuthException;

    SecurityToken doIssueSecurityToken( @Nonnull final BaseRole role,
                                        @Nonnull final RoleSecurityTokenAttributes attributes,
                                        final int durationSeconds ) throws AuthException;

    TemporaryAccessKey doLookupAccessKey( @Nonnull final String accessKeyId,
                                          @Nonnull final String token ) throws AuthException;

    String doGenerateSecret( @Nonnull final String nonce,
                             @Nonnull final String secret );

    SecurityTokenContent doDecode(
        final String accessKeyId,
        final String token
    ) throws AuthException;
  }


}
