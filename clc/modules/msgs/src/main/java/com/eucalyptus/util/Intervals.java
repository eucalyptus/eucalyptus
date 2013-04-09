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
