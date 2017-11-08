/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.crypto.util;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.login.AuthenticationException;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class Timestamps {
  private static final Logger LOG = Logger.getLogger( Timestamps.class );

  private static boolean allowTrailers = Boolean.parseBoolean( 
      System.getProperty( "com.eucalyptus.crypto.util.allowTimestampTrailers", "false" ) );
  
  public enum Type {
    RFC_2616(rfc2616),
    ISO_8601(iso8601);
    
    private final List<PatternHolder> patterns;
    
    private Type( final List<PatternHolder> patterns ) {
      this.patterns = patterns;  
    }
  }
  
  /**
   * Parse a timestamp from an ISO 8601 format.
   *  
   * @param timestamp The timestamp to parse.
   * @return The date representing the timestamp
   * @throws AuthenticationException If the timestamp cannot be parsed
   */
  public static Date parseIso8601Timestamp( final String timestamp ) throws AuthenticationException {
    return parseTimestamp( timestamp, Timestamps.iso8601 );
  }

  /**
   * Parse a timestamp from an RFC 2616 / HTTP 1.1 date format.
   *
   * @param timestamp The timestamp to parse.
   * @return The date representing the timestamp
   * @throws AuthenticationException If the timestamp cannot be parsed
   */
  public static Date parseRfc2616Timestamp( final String timestamp ) throws AuthenticationException {
    return parseTimestamp( timestamp, Timestamps.rfc2616 );
  }

  public static Date parseTimestamp( final String timestamp, final Type type ) throws AuthenticationException {
    return parseTimestamp( timestamp, type.patterns );
  }

  private static Date parseTimestamp( final String timestamp, final Iterable<PatternHolder> patterns ) throws AuthenticationException {
    if ( timestamp != null ) for ( final PatternHolder pattern : patterns ) {
      final ParsePosition position = new ParsePosition(0);
      final Date parsed = pattern.parse( timestamp, position );
      if ( parsed == null || (position.getIndex() < timestamp.length() && !allowTrailers)) {
        if ( LOG.isTraceEnabled() ) LOG.trace( "Parse of timestamp '"+timestamp+"' failed for pattern '"+pattern+"', at: " + position.getErrorIndex() );
      } else {
        return parsed;
      }
    }
    throw new AuthenticationException( "Invalid timestamp format: " + timestamp  );
  }

  public static String formatRfc822Timestamp( final Date date ) {
    return sdf( rfc822Timestamp ).format( date );
  }

  public static String formatIso8601Timestamp( final Date date ) {
    return sdf( iso8601Timestamp ).format( date );
  }

  public static String formatShortIso8601Timestamp( final Date date ) {
    return sdf( iso8601ShortTimestamp ).format( date );    
  }

  public static String formatShortIso8601Date( final Date date ) {
    return sdf( iso8601ShortDate ).format( date );
  }

  public static String formatIso8601UTCLongDateMillisTimezone( final Date date ) {
    final SimpleDateFormat format = sdf( iso8601TimestampWithMillisAndTimezone );
    return format.format(date);
  }

  /**
   * RFC 822 timestamp format suitable for HTTP headers
   */
  private static final String rfc822Timestamp = "EEE, dd MMM yyyy HH:mm:ss z";

  /**
   * ISO 8601 short timestamp format
   */
  private static final String iso8601TimestampWithMillisAndTimezone = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

  /**
   * ISO 8601 short timestamp format
   */
  private static final String iso8601Timestamp = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  /**
   * ISO 8601 short timestamp format
   */
  private static final String iso8601ShortTimestamp = "yyyyMMdd'T'HHmmss'Z'";

  /**
   * ISO 8601 short date format
   */
  private static final String iso8601ShortDate = "yyyyMMdd";

  /**
   * Time zone to be used for simple date format.
   */
  private static final Map<String,String> zonesByPattern = ImmutableMap.of(
      rfc822Timestamp, "GMT"
  );

  /**
   * RFC 2616 / HTTP 1.1 date formats (http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1)
   */
  private static final List<PatternHolder> rfc2616 = ImmutableList.of(
    new PatternHolder( "EEE, dd MMM yyyy HH:mm:ss zzz" ), // RFC 822 / 1123
    new PatternHolder(  "EEEE, dd-MMM-yy HH:mm:ss zzz" ), // RFC 850 / 1036
    new PatternHolder(  "EEE MMM d HH:mm:ss yyyy" ) // ANSI C asctime() format 
  );

  /**
   * ISO 8601 date formats
   */
  static final List<PatternHolder> iso8601;
  
  static {
    final List<String> patterns = Lists.newArrayList(
      "yyyy-MM-dd'T'HH:mm:ss",
      "yyyy-MM-dd'T'HH:mm:ssZ",
      "yyyy-MM-dd'T'HH:mm:ssX",
      "yyyy-MM-dd'T'HH:mm:ssXXX",
      "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
      "yyyy-MM-dd'T'HH:mm:ss.SSSX",
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'Z",
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
      "yyyy-MM-dd'T'HH:mm:ss'Z'Z"
    );

    // Generate seed patterns with various sub-second precisions
    for ( int i=1; i<10; i++ ) {
      String pattern = "yyyy-MM-dd'T'HH:mm:ss." + Strings.repeat( "S", i );
      patterns.add( pattern );
      patterns.add( pattern + "'Z'" );
    }

    final List<PatternHolder> generatedPatterns = Lists.newArrayList();    
    for ( final String pattern : patterns ) {
      for ( final Iso8601Variants variant : Iso8601Variants.values() ) {
        // Type hint required to compile on OpenJDK 1.6
        generatedPatterns.add( PatternHolder.generate( ((Function<String,String>)variant).apply( pattern ) ) );
      }
    }

    LOG.debug( "Using ISO 8601 date patterns: " + generatedPatterns );
    
    iso8601 = ImmutableList.copyOf( generatedPatterns );  
  }

  private static ThreadLocal<Cache<String,SimpleDateFormat>> patternLocal =
      new ThreadLocal<Cache<String,SimpleDateFormat>>(  ) {
        @Override
        protected Cache<String,SimpleDateFormat> initialValue( ) {
          return CacheBuilder.newBuilder( ).softValues( ).build( );
        }
      };

  private static SimpleDateFormat sdf( final String pattern ) {
    SimpleDateFormat format = patternLocal.get( ).getIfPresent( pattern );
    if ( format == null ) {
      format = new SimpleDateFormat( pattern );
      format.setTimeZone( TimeZone.getTimeZone( zone( pattern ) ) );
      patternLocal.get( ).put( pattern, format );
    }
    return format;
  }

  private static String zone( final String pattern ) {
    return zonesByPattern.getOrDefault( pattern, "UTC" );
  }

  private static final class PatternHolder {
    private final String pattern;
    private final int length; // length of the text that can match the pattern if known
    private final int fractionTrunction;
    private final int fractionPadding;

    private PatternHolder( final String pattern ) {
      this( pattern, -1 ); 
    }

    private PatternHolder( final String pattern, final int length ) {
      this.pattern = pattern;
      this.length = length;
      
      int precision = 0;
      for ( char character : pattern.toCharArray() ) {
        if ( character == 'S' ) precision++;  
      }      
      fractionTrunction = Math.max( 0, precision - 3 );
      fractionPadding = precision == 0  ? 0 : Math.max( 0, 3 - precision );
    }

    /**
     * TODO - WARNING, only supports a special case for expected ISO 8601 date formats 
     */
    private static PatternHolder generate( final String pattern ) {
      String representativeInput = pattern;
      representativeInput = representativeInput.replace( "'T'", "T" );
      representativeInput = representativeInput.replace( "'Z'", "U" );
      representativeInput = representativeInput.replace( "Z", "-0000" );
      representativeInput = representativeInput.replace( "XXX", "-00:00" );
      representativeInput = representativeInput.replace( "X", "-00" );
      return new PatternHolder( pattern, representativeInput.length() );
    }
    
    private Date parse( final String timestamp, final ParsePosition position ) {
      Date result = null;
      if ( length == -1 || length == timestamp.length() - position.getIndex() ) {
        String timestampForParsing = timestamp;   
        boolean valid = true;
        if ( fractionTrunction > 0 || fractionPadding > 0 ) {
          valid = false;
          final int fractionIndex = timestamp.indexOf('.');
          if ( fractionTrunction > 0 ) {
            // Date parser parses milliseconds from the wrong end of the value
            // e.g. 000581 is 581 milliseconds when it should be zero
            // To parse correctly we shift the value and pad with leading zeros.
            if ( fractionIndex > 0 && timestamp.length() >= (fractionIndex + 4 + fractionTrunction) ) {
                timestampForParsing = 
                  timestamp.substring( 0, fractionIndex + 1 ) +
                  Strings.repeat( "0", fractionTrunction ) +
                  timestamp.substring( fractionIndex + 1, fractionIndex + 4 ) +
                  timestamp.substring( fractionIndex + 4 + fractionTrunction );
              final String unparsed = timestamp.substring( fractionIndex + 4, fractionIndex + 4 + fractionTrunction );            
              valid = isDigits( unparsed );
            }
          } else if ( fractionIndex > 0 && timestamp.length() >= (fractionIndex + 3 - fractionPadding) ) {
            // Date parser parses fractional tens or hundredths of a second incorrectly
            // e.g. .5 is 5 milliseconds when it should be 500
            // To parse correctly we pad with trailing zeros.
            timestampForParsing =
                timestamp.substring( 0, fractionIndex + ( 4 - fractionPadding )  ) +
                Strings.repeat( "0", fractionPadding ) +
                timestamp.substring( fractionIndex + ( 4 - fractionPadding ) );
            valid = true;
          }
        }
        if ( valid ) {
          result = sdf( pattern ).parse( timestampForParsing, position );
        }
      } 
      
      if ( result == null && position.getErrorIndex() < 0 ) {
        position.setErrorIndex( position.getIndex() );
      }
      
      return result;
    }
    
    private boolean isDigits( final String text )  {
      boolean digits = true;
      for ( final char character : text.toCharArray() ) {
        if ( character < '0' || character > '9' ) {
          digits = false;
          break;
        }
      }     
      return digits;
    }
    
    public String toString() {
      return pattern;
    }
  }
  
  private enum Iso8601Variants implements Function<String,String> {
    IDENTITY {
      @Override
      public String apply( final String pattern ) {
        return pattern;
      }
    },
    /**
     * The hyphens can be omitted if compactness of the representation is more 
     * important than human readability ....  As with the date notation, the 
     * separating colons can also be omitted
     */
    SHORT {
      @Override
      public String apply( final String pattern ) {
        return pattern.replaceAll( ":|-", "" );
      }
    }
  } 

}
