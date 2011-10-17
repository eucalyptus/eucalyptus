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
