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
package com.eucalyptus.crypto.util;

import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

/**
 * 
 */
public enum SecurityHeader {

  Date,
  X_Amz_Date(true);
  
  private final String header;
  
  private SecurityHeader() {
    this( false );  
  }

  private SecurityHeader( final boolean dashValue ) {
    this.header = dashValue ? name().replace('_','-') : name();
  }

  /**
   * Get the header name.
   *
   * @return The name
   */
  @Nonnull
  public String header() {
    return header;
  }

  public enum Value {
    AWS4_HMAC_SHA256( true ) {
      @Override
      public boolean matches( @Nullable final String headerValue ) {
        return value().equals( Iterables.getFirst(
            Splitter.on( whitespace ).limit( 2 ).omitEmptyStrings()
                .split( Strings.nullToEmpty( headerValue ) ), "" ) );
      }
    };

    private static final Pattern whitespace = Pattern.compile( "\\s+" );

    private final String value;

    private Value() {
      this( false );
    }
    
    private Value( final boolean dashValue ) {
      this.value = dashValue ? name().replace('_','-') : name();
    }

    /**
     * Does this header value match the given header.
     *
     * @param header The value to match against
     * @return True if matches
     */
    public abstract boolean matches( @Nullable String header );

    /**
     * Get the header value.
     *
     * @return The value
     */
    @Nonnull
    public String value() {
      return value;
    }
  }
}
