/*************************************************************************
 * Copyright 2008 Regents of the University of California
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

package com.eucalyptus.auth.json;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
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
    return checkBinaryOption( map, element1, element2, true );
  }

  public static String checkBinaryOption( JSONObject map, String element1, String element2, boolean required ) throws JSONException {
    boolean has1 = map.containsKey( element1 );
    boolean has2 = map.containsKey( element2 );
    if ( required && !has1 && !has2 ) {
      throw new JSONException( "Element " + element1 + " or " + element2 + " is required" );
    }
    if ( has1 && has2 ) {
      throw new JSONException( "Element " + element1 + " and " + element2 + " can not be both present" );
    }
    return ( has1 ? element1 : element2 );
  }

  public static List<String> parseStringOrStringList( JSONObject statement, String key ) throws JSONException {
    return parseStringOrStringList( Collections.singleton( String.class ), statement, key );
  }

  public static List<String> parseStringOrStringList( Set<Class<?>> types, JSONObject statement, String key ) throws JSONException {
    List<String> results = Lists.newArrayList( );
    try {
      Object value = getByType( types, statement, key );
      if (value != null) results.add( String.valueOf( value ) );
      return results;
    } catch ( JSONException e ) {
    }
    try {
      return getArrayByType( types, statement, key, String::valueOf );
    } catch ( JSONException e ) {
      throw new JSONException( key + " element can only be String or a list of String" );
    }
  }
  
  @SuppressWarnings( "unchecked" )
  public static <T> List<T> getArrayByType( Class<T> type, JSONObject map, String key ) throws JSONException {
    return ( List<T> ) getArrayByType( Collections.singleton( type ), map, key, Function.identity( ) );
  }

  public static <T> List<T> getArrayByType( Set<Class<?>> types, JSONObject map, String key, Function<Object,T> converter ) throws JSONException {
    List<T> results = Lists.newArrayList( );
    JSONArray array = getByType( JSONArray.class, map, key );
    if ( array != null ) {
      for ( Object o : array ) {
        if ( types.contains( o.getClass( ) ) ) {
          results.add( converter.apply( o ) );
        } else {
          throw new JSONException( "Expecting array element type " + types + " but got " + o.getClass( ).getName( ) );
        }
      }
    }
    return results;
  }

  @SuppressWarnings( "unchecked" )
  public static <T> T getByType( Class<T> type, JSONObject map, String key ) throws JSONException {
    return ( T ) getByType( Collections.singleton( type ), map, key );
  }

  public static Object getByType( Set<Class<?>> types, JSONObject map, String key ) throws JSONException {
    Object value = map.get( key );
    if ( value == null ) {
      return null;
    }
    if ( !types.contains( value.getClass( ) ) ) {
      throw new JSONException( "Expecting " + types + " but got " + value.getClass( ).getName( ) );
    }
    return value;
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
