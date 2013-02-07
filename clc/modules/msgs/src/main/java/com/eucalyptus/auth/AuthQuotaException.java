/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import javax.annotation.Nonnull;
import com.eucalyptus.util.Parameters;

/**
 * Authorization error due to quota limit exceeded.
 */
public class AuthQuotaException extends AuthException {
  private static final long serialVersionUID = 1L;

  @Nonnull
  private final String type;
  
  public AuthQuotaException( @Nonnull final String type,
                             final String message, 
                             final Throwable cause ) {
    super( message, cause );
    Parameters.checkParam( "Resource type", type, not( isEmptyOrNullString() ) );
    this.type = type;
  }

  public AuthQuotaException( @Nonnull final String type,
                             final String message ) {
    super( message );
    Parameters.checkParam( "Resource type", type, not( isEmptyOrNullString() ) );
    this.type = type;
  }

  /**
   * Get the resource type.
   * 
   * @return The requested type.
   */
  @Nonnull
  public String getType() {
    return type;
  }
}
