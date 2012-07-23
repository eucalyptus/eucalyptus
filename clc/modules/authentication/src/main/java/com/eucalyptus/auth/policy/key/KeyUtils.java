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
import net.sf.json.JSONException;

public class KeyUtils {

  public static void validateIntegerValue( String value, String key ) throws JSONException {
    try {
      Long lv = Long.valueOf( value );
      if ( lv <= 0 ) {
        throw new JSONException( "Invalid value for " + key + ": " + value + ". Must be positive." );
      }
    } catch ( NumberFormatException e ) {
      throw new JSONException( "Invalid value format for " + key + ": " + value + ". Integer is required." );
    }
  }
  
  public static void validateDateValue( String value, String key ) throws JSONException {
    try {
      Iso8601DateParser.parse( value );
    } catch ( ParseException e ) {
      throw new JSONException( "Invalid value format for " + key + ": " + value + ". Date (ISO8601) is required.", e );
    }
  }
  
  public static void validateCidrValue( String value, String key ) throws JSONException {
    try {
      Cidr.valueOf( value );
    } catch ( CidrParseException e ) {
      throw new JSONException( "Invalid value format for " + key + ": " + value + ". IPv4 address or CIDR (RFC4632) is required.", e );
    }
  }
  
}
