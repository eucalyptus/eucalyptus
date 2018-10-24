/*************************************************************************
 * Copyright 2008 Regents of the University of California
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

package com.eucalyptus.auth.policy.ern;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.eucalyptus.auth.policy.PolicySpec;
import com.google.common.base.Strings;
import net.sf.json.JSONException;

public class AddressUtil {

  private static final Pattern IPV4_ADDRESS_PATTERN = Pattern.compile( "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})" );
  
  /**
   * Match one IPv4 address or match a range of IPv4 addresses.
   * 
   * @param pattern
   * @param address
   * @return
   */
  public static boolean addressRangeMatch( String pattern, String address ) {
    if ( PolicySpec.ALL_RESOURCE.equals( address ) ) {
      return true;
    }
    Matcher patternMatcher = PolicySpec.IPV4_ADDRESS_RANGE_PATTERN.matcher( pattern );
    if ( !patternMatcher.matches( ) ) {
      // Should not happen since pattern is verified before.
      return false;
    }
    if ( patternMatcher.group( 5 ) == null ) {
      // Not a range, but a single ipv4 address
      return pattern.equals( address );
    } else {
      Matcher addressMatcher = IPV4_ADDRESS_PATTERN.matcher( address );
      if ( !addressMatcher.matches( ) ) {
        return false;
      }
      try {
        long addressValue = addressNumerical( addressMatcher.group( 1 ), addressMatcher.group( 2 ), addressMatcher.group( 3 ), addressMatcher.group( 4 ) );
        long addressStart = addressNumerical( patternMatcher.group( 1 ), patternMatcher.group( 2 ), patternMatcher.group( 3 ), patternMatcher.group( 4 ) );
        long addressEnd = addressNumerical( patternMatcher.group( 5 ), patternMatcher.group( 6 ), patternMatcher.group( 7 ), patternMatcher.group( 8 ) );
        return ( ( addressValue >= addressStart ) && ( addressValue <= addressEnd ) );
      } catch ( Exception e ) {
        return false;
      }
    }
  }
  
  /**
   * Validate IPv4 address range.
   * 
   * @param pattern
   * @throws JSONException
   */
  public static void validateAddressRange( String pattern ) throws JSONException {
    if ( Strings.isNullOrEmpty( pattern ) ) {
      throw new JSONException( "Empty address pattern" );
    }
    Matcher matcher = PolicySpec.IPV4_ADDRESS_RANGE_PATTERN.matcher( pattern );
    if ( !matcher.matches( ) ) {
      throw new JSONException( "Invalid IPv4 address range: " + pattern );
    }
    long startAddress = addressNumerical( matcher.group( 1 ), matcher.group( 2 ), matcher.group( 3 ), matcher.group( 4 ) );
    if ( matcher.group( 5 ) != null ) {
      long endAddress = addressNumerical( matcher.group( 5 ), matcher.group( 6 ), matcher.group( 7 ), matcher.group( 8 ) );
      if ( endAddress < startAddress ) {
        throw new JSONException( "End address is smaller than start address in address range: " + pattern );
      }
    }
  }
  
  private static long addressNumerical( String o1, String o2, String o3, String o4 ) throws JSONException {
    long value = 0;
    int octet = Integer.parseInt( o1 );
    if ( octet > 255 ) {
      throw new JSONException( "Address component value larger than 255: " + o1 + "." + o2 + "." + o3 + "." + o4 );
    }
    value = ( value << 8 ) + octet;
    octet = Integer.parseInt( o2 );
    if ( octet > 255 ) {
      throw new JSONException( "Address component value larger than 255: " + o1 + "." + o2 + "." + o3 + "." + o4 );
    }
    value = ( value << 8 ) + octet;
    octet = Integer.parseInt( o3 );
    if ( octet > 255 ) {
      throw new JSONException( "Address component value larger than 255: " + o1 + "." + o2 + "." + o3 + "." + o4 );
    }
    value = ( value << 8 ) + octet;
    octet = Integer.parseInt( o4 );
    if ( octet > 255 ) {
      throw new JSONException( "Address component value larger than 255: " + o1 + "." + o2 + "." + o3 + "." + o4 );
    }
    value = ( value << 8 ) + octet;
    return value;
  }
  
  public static void main( String[] args ) {
    validateAddressRange( "192.168.7.1" );
    validateAddressRange( "192.168.7.1-192.168.7.10" );
    try {
      validateAddressRange( "192.169.255." );
    } catch ( Exception e ) {
      e.printStackTrace( );
    }
    try {
      validateAddressRange( "192.169.256.1" );
    } catch ( Exception e ) {
      e.printStackTrace( );
    }
    try {
      validateAddressRange( "192.169.255.1-" );
    } catch ( Exception e ) {
      e.printStackTrace( );
    }
    try {
      validateAddressRange( "192.169.255.1-192." );
    } catch ( Exception e ) {
      e.printStackTrace( );
    }
    
    System.out.println( addressRangeMatch( "192.168.7.10-192.168.7.122", "192.168.7.16" ) );
    System.out.println( addressRangeMatch( "192.168.7.10-192.168.7.122", "192.168.7.133" ) );
    System.out.println( addressRangeMatch( "192.168.7.10-192.168.7.122", "192.168.7.1" ) );
    System.out.println( addressRangeMatch( "192.168.7.10-192.168.7.122", "192.168.7" ) );
  }
  
}
