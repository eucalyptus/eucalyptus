/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

  SecurityToken( final String accessKeyId,
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
