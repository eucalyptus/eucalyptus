/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

import javax.annotation.Nonnull;
import com.google.common.base.Preconditions;

/**
 * A token issued by the SecurityTokenManager.
 */
public class SecurityToken {
  private final String accessKeyId;
  private final String secretKey;
  private final String token;
  private final long expires;

  public SecurityToken( final String accessKeyId,
                        final String secretKey,
                        final String token,
                        final long expires ) {
    Preconditions.checkNotNull( accessKeyId, "Access key identifier is required" );
    Preconditions.checkNotNull( secretKey, "Secret key is required" );
    Preconditions.checkNotNull( token, "Token is required" );

    this.accessKeyId = accessKeyId;
    this.secretKey = secretKey;
    this.token = token;
    this.expires = expires;
  }

  /**
   * Get the access key identifier for use with the token.
   *
   * @return The access key identifier
   */
  @Nonnull
  public String getAccessKeyId() {
    return accessKeyId;
  }

  /**
   * Get the (private) secret key for use with the token.
   *
   * @return The secret key
   */
  @Nonnull
  public String getSecretKey() {
    return secretKey;
  }

  /**
   * Get the public security token value.
   *
   * @return The security token
   */
  @Nonnull
  public String getToken() {
    return token;
  }

  /**
   * Get the expiry date for the token.
   *
   * @return The expiry date
   */
  public long getExpires() {
    return expires;
  }
}
