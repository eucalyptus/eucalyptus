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
