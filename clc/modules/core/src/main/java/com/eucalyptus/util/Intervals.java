/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.util;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.records.Logs;
import com.google.common.collect.ImmutableMap;

/**
 * Utility for parsing time intervals (a.k.a. durations)
 */
public class Intervals {

  private static final Pattern intervalPattern = Pattern.compile( "(\\d+)(d|h|m|s|ms)?" );
  private static final Map<String,TimeUnit> units = ImmutableMap.<String,TimeUnit>builder()
      .put( "d", TimeUnit.DAYS )
      .put( "h", TimeUnit.HOURS )
      .put( "m", TimeUnit.MINUTES )
      .put( "s", TimeUnit.SECONDS )
      .put( "ms", TimeUnit.MILLISECONDS )
      .build();

  public static long parse( @Nonnull final String value ) throws ParseException {
    return parse( value, TimeUnit.MILLISECONDS );
  }

  public static long parse( @Nullable final String value,
                                      final long defaultValue ) {
    if ( value != null ) try {
      return parse( value, TimeUnit.MILLISECONDS );
    } catch ( ParseException e ) {
      Logs.exhaust().debug( e, e );
    }
    return defaultValue;
  }

  public static long parse( @Nonnull final String value,
                            @Nonnull final TimeUnit defaultUnit ) throws ParseException {
    final String valueToParse = value.toLowerCase().trim();
    final Matcher matcher = intervalPattern.matcher( valueToParse );
    if ( matcher.matches() ) {
      final TimeUnit unit = matcher.group( 2 ) != null ?
          units.get( matcher.group( 2 ) ) :
          defaultUnit;
      try {
        return unit.toMillis( Long.parseLong( matcher.group( 1 ) ) );
      } catch ( NumberFormatException nfe ) {
        throw new ParseException( "Invalid interval: " + value, 0 );
      }
    } else {
      throw new ParseException( "Invalid interval: " + value, 0 );
    }
  }

  public static long parse( @Nullable final String value,
                            @Nonnull  final TimeUnit defaultUnit,
                                      final long defaultValue ) {
    if ( value != null ) try {
      return parse( value, defaultUnit );
    } catch ( ParseException e ) {
      Logs.exhaust().debug( e, e );
    }
    return defaultValue;
  }


}
