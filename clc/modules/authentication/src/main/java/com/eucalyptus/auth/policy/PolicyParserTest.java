package com.eucalyptus.auth.policy;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class PolicyParserTest {
  
  public static void main( String args[ ] ) {
    String jsonText =
      "{'key':'value', 'array':['e1', 'e2'], 'object':{'k1':'v1', 'k2':'v2'}}";
    
    JSONObject json = JSONObject.fromObject( jsonText );
    
    System.out.println( "Field: key = " + json.getString( "key" ) );
    
    System.out.println( "Field: array = " + getByType( String.class, json, "array" ) );
    
    Object value = json.get( "foo" );
    if ( value instanceof JSONObject ) {
      System.out.println( "'array' value is object" );
    } else if ( value instanceof JSONArray ) {
      System.out.println( "'array' value is array" );
    }
  }
  
  @SuppressWarnings( "unchecked" )
  private static <T> T getByType( Class<T> type, JSONObject map, String key ) throws JSONException {
    Object value = map.get( key );
    if ( value == null ) {
      return null;
    }
    if ( value.getClass( ) != type ) {
      throw new JSONException( "Expecting " + type.getName( ) + " value but got " + value.getClass( ).getName( ) );
    }
    return ( T ) value;
  }
}
