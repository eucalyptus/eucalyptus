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
