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
    List<String> results = Lists.newArrayList( );
    try {
      String value = getByType( String.class, statement, key );
      if (value != null) results.add( value );
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
