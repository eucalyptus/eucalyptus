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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.crypto.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.login.AuthenticationException;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class Timestamps {
  private static final Logger LOG = Logger.getLogger( Timestamps.class );

  public enum Type {
    RFC_2616(rfc2616),
    ISO_8601(iso8601);
    
    private final List<String> patterns;
    
    private Type( final List<String> patterns ) {
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

  public static Date parseTimestamp( final String timestamp, final Iterable<String> patterns ) throws AuthenticationException {
    if ( timestamp != null ) for ( String pattern : patterns ) {
      try {
        return sdf( pattern ).parse( timestamp );
      } catch ( ParseException e ) {
        LOG.trace( e, e );
      }
    }
    throw new AuthenticationException( "Invalid timestamp format: " + timestamp  );
  }
  
  public static String formatShortIso8601Timestamp( final Date date ) {
    return sdf( iso8601ShortTimestamp ).format( date );    
  }

  public static String formatShortIso8601Date( final Date date ) {
    return sdf( iso8601ShortDate ).format( date );
  }

  private static SimpleDateFormat sdf( final String pattern ) {
    final SimpleDateFormat format = new SimpleDateFormat( pattern );
    format.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
    return format;
  }

  /**
   * ISO 8601 short timestamp format
   */
  private static final String iso8601ShortTimestamp = "yyyyMMdd'T'HHmmss'Z'";

  /**
   * ISO 8601 short date format
   */
  private static final String iso8601ShortDate = "yyyyMMdd";

  /**
   * RFC 2616 / HTTP 1.1 date formats (http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1)
   */
  private static final List<String> rfc2616 = ImmutableList.of(
      "EEE, dd MMM yyyy HH:mm:ss zzz", // RFC 822 / 1123
      "EEEE, dd-MMM-yy HH:mm:ss zzz", // RFC 850 / 1036
      "EEE MMM d HH:mm:ss yyyy" // ANSI C asctime() format 
  );

  /**
   * ISO 8601 date formats
   */
  private static final List<String> iso8601;  
  
  static {
    final String[] patterns = {
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'Z",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'Z" };
    
    final List<String> generatedPatterns = Lists.newArrayList();
    
    for ( final String pattern : patterns ) {
      for ( final Iso8601Variants variant : Iso8601Variants.values() ) {
        // Type hint required to compile on OpenJDK 1.6
        generatedPatterns.add( ((Function<String,String>)variant).apply( pattern ) );
      }
    }
    
    LOG.debug( "Using ISO 8601 date patterns: " + generatedPatterns );
    
    iso8601 = ImmutableList.copyOf( generatedPatterns );  
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
