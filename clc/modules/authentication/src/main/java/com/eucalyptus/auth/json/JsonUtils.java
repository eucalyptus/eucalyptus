package com.eucalyptus.auth.json;

import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import com.google.common.collect.Lists;

public class JsonUtils {
  
  public static String checkRequired( JSONObject map, String element ) throws JSONException {
    if ( !map.containsKey( element ) ) {
      throw new JSONException( "Missing required element " + element );
    }
    return element;
  }
  
  public static String checkBinaryOption( JSONObject map, String element1, String element2 ) throws JSONException {
    boolean has1 = map.containsKey( element1 );
    boolean has2 = map.containsKey( element2 );
    if ( !has1 && !has2 ) {
      throw new JSONException( "Element " + element1 + " or " + element2 + " is required" );
    }
    if ( has1 && has2 ) {
      throw new JSONException( "Element " + element1 + " and " + element2 + " can not be both present" );
    }
    return ( has1 ? element1 : element2 );
  }
  
  public static List<String> parseStringOrStringList( JSONObject statement, String key ) throws JSONException {
    List<String> results = Lists.newArrayList( );
    try {
      String value = getByType( String.class, statement, key );
      results.add( value );
      return results;
    } catch ( JSONException e ) {
    }
    try {
      return getArrayByType( String.class, statement, key );
    } catch ( JSONException e ) {
      throw new JSONException( key + " element can only be String or a list of String" );
    }
  }
  
  @SuppressWarnings( "unchecked" )
  public static <T> List<T> getArrayByType( Class<T> type, JSONObject map, String key ) throws JSONException {
    List<T> results = Lists.newArrayList( );
    JSONArray array = getByType( JSONArray.class, map, key );
    if ( array != null ) {
      for ( Object o : array ) {
        if ( o.getClass( ) == type ) {
          results.add( ( T ) o );
        } else {
          throw new JSONException( "Expecting array element type " + type.getName( ) + " but got " + o.getClass( ).getName( ) );
        }
      }
    }
    return results;
  }
  
  @SuppressWarnings( "unchecked" )
  public static <T> T getByType( Class<T> type, JSONObject map, String key ) throws JSONException {
    Object value = map.get( key );
    if ( value == null ) {
      return null;
    }
    if ( value.getClass( ) != type ) {
      throw new JSONException( "Expecting " + type.getName( ) + " but got " + value.getClass( ).getName( ) );
    }
    return ( T ) value;
  }

  public static <T> T getRequiredByType( Class<T> type, JSONObject map, String key ) throws JSONException {
    T value = getByType( type, map, key );
    if ( value == null ) {
      throw new JSONException( "Expecting required element " + key );
    }
    return value;
  }
  
  public static <T> List<T> getRequiredArrayByType( Class<T> type, JSONObject map, String key ) throws JSONException {
    List<T> results = getArrayByType( type, map, key );
    if ( results.size( ) < 1 ) {
      throw new JSONException( "Expecting required element " + key );
    }
    return results;
  }
  
}
