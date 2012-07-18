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

package com.eucalyptus.auth.policy.key;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ISO 8601 date and time format.
 * 
 * http://www.w3.org/TR/1998/NOTE-datetime-19980827
 * 
 */
public class Iso8601DateParser {
  
  private static final Pattern DATE_PATTERN =
      Pattern.compile( "(\\d\\d\\d\\d)(?:\\-(\\d\\d)(?:\\-(\\d\\d)(?:T(\\d\\d):(\\d\\d)(?::(\\d\\d)(?:\\.(\\d+))?)?(?:(Z)|(?:(\\+|\\-)(\\d\\d):(\\d\\d))))?)?)?" );
  
  /**
   * Parse an ISO 8601 format date.
   * NOTE: There is a slight difference between a real ISO 8601 date string and a date string accepted by this parser.
   * Because we use SimpleDateFormat to parse, if any of the month/day/hour/minute/second field exceeds its allowed range,
   * the parser simply propagate to the higher digits. For example, if minute field is "61", it will become hour "1" and
   * minute "1".
   * 
   * @param input The input date string
   * @return A parsed Java Date
   * @throws ParseException for any syntax error
   */
  public static Date parse( String input ) throws ParseException {
    //System.out.println( "Current time: " + toString( new Date( ) ) );
    //System.out.println( "Parsing: " + input );
    if ( input == null || "".equals( input ) ) {
      throw new ParseException( "Empty date string", 0 );
    }
    boolean hasTzd = false;
    Matcher matcher = DATE_PATTERN.matcher( input );
    if ( !matcher.matches( ) ) {
      throw new ParseException( "Invalid date string", 0 );
    }
    SimpleDateFormat sdf;
    if ( matcher.group( 2 ) == null ) {
      sdf = new SimpleDateFormat( "yyyy" );
    } else if ( matcher.group( 3 ) == null ) {
      sdf = new SimpleDateFormat( "yyyy-MM" );
    } else if ( matcher.group( 4 ) == null ) {
      sdf = new SimpleDateFormat( "yyyy-MM-dd" );
    } else if ( matcher.group( 6 ) == null ) {
      sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mmz" );
      hasTzd = true;
    } else if ( matcher.group( 7 ) == null ) {
      sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssz" );
      hasTzd = true;
    } else {
      sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'.'SSSz" );
      hasTzd = true;
      int fractionLength = matcher.group( 7 ).length( );
      if ( fractionLength > 3 ) {
        int dot = input.indexOf( '.' );
        input = input.substring( 0, dot + 4 ) + input.substring( dot + fractionLength + 1 );
      }
    }
    if ( hasTzd ) {
      if ( matcher.group( 8 ) != null ) {
        input = input.replaceAll( "Z", "+0000" );
      } else {
        int length = input.length( );
        input = input.substring( 0, length - 3 ) + input.substring( length - 2 );
      }
    }
    //System.out.println( "Before parsing: " + input );
    sdf.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
    Date ret = sdf.parse( input );
    //System.out.println( "Parsed date: " + toString( ret ) );
    return ret;
  }
  
  /**
   * Convert a Date into ISO 8601 format string, using UTC timezone.
   * 
   * @param date The date to convert.
   * @return The ISO 8601 format date string.
   */
  public static String toString( Date date ) {
    SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'.'S'Z'" );
    sdf.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
    String output = sdf.format( date );
    return output;
  }
  
}
