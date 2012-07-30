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

package com.eucalyptus.webui.shared.query;

import java.util.Set;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;

/**
 * Internal representation of a search query
 */
public class SearchQuery {

  public interface Matcher {
    boolean match( QueryValue value );
  }
  
  private QueryType type;
  
  private HashMultimap<String, QueryValue> termMap = HashMultimap.create( );
  
  public SearchQuery( QueryType type ) {
    this.setType( type );
  }

  public void setType( QueryType type ) {
    this.type = type;
  }

  public QueryType getType( ) {
    return type;
  }

  public void setTermMap( HashMultimap<String, QueryValue> termMap ) {
    this.termMap = termMap;
  }

  public HashMultimap<String, QueryValue> getTermMap( ) {
    return termMap;
  }
  
  /**
   * Match a field's values using a provided matcher for each value.
   * 
   * @param field
   * @param matcher
   * @return
   */
  public boolean match( String field, Matcher matcher ) {
    Set<QueryValue> values = this.termMap.get( field );
    if ( values == null || values.size( ) < 1 ) {
      return true;
    }
    for ( QueryValue v : values ) {
      if ( matcher.match( v ) ) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Contains the field condition.
   * 
   * @param field
   * @return
   */
  public boolean has( String field ) {
    Set<QueryValue> values = this.termMap.get( field );
    if ( values == null || values.size( ) < 1 ) {
      return false;
    }
    return true;
  }
  
  /**
   * Contains the field condition with only a single value
   * 
   * @param field
   * @return
   */
  public boolean hasSingle( String field ) {
    Set<QueryValue> values = this.termMap.get( field );
    if ( values != null && values.size( ) == 1 ) {
      return true;
    }
    return false;  
  }
  
  /**
   * Contains the field with single value and does not contain excluded fields.
   * 
   * @param field
   * @param excluded
   * @return
   */
  public boolean hasSingleExclusive( String field, String... excluded ) {
    for ( String ex : excluded ) {
      if ( this.termMap.containsKey( ex ) ) {
        return false;
      }
    }
    return hasSingle( field );
  }
  
  /**
   * Contains the only field.
   * 
   * @param field
   * @return
   */
  public boolean hasOnly( String field ) {
    if ( this.termMap.keySet( ).size( ) == 1 ) {
      return has( field );
    }
    return false;
  }
  
  /**
   * Contains the only field and with a single value.
   * 
   * @param field
   * @return
   */
  public boolean hasOnlySingle( String field ) {
    if ( this.termMap.keySet( ).size( ) == 1 ) {
      return hasSingle( field );
    }
    return false;
  }
  
  public String toString( boolean withType ) {
    StringBuilder sb = new StringBuilder( );
    Joiner joiner = Joiner.on( QueryConstants.COMMA ).skipNulls( );
    if ( withType ) {
      sb.append( this.type.name( ) ).append( QueryConstants.TYPE_SEPARATOR );
    }
    boolean first = true;
    for ( String key : this.termMap.keySet( ) ) {
      if ( first ) {
        first = false;
      } else {
        sb.append( QueryConstants.SPACE );
      }
      sb.append( key ).append( QueryConstants.EQUAL ).append( joiner.join( this.termMap.get( key ) ) );
    }
    return sb.toString( );
  }
  
  @Override
  public String toString( ) {
    return toString( true );
  }

  public void add( String name, QueryValue value ) {
    this.termMap.put( name, value );
  }
  
  public QueryValue getSingle( String field ) {
    Set<QueryValue> values = this.termMap.get( field );
    if ( values != null ) {
      return values.toArray( new QueryValue[0] )[0];
    }
    return null;
  }
  
  @Override
  public boolean equals( Object other ) {
    if ( other == null ) {
      return false;
    }
    if ( !( other instanceof SearchQuery ) ) {
      return false;
    }
    SearchQuery otherQuery = ( SearchQuery )other;
    if ( this.type == otherQuery.type &&
         ( ( this.termMap == null && otherQuery.termMap == null ) ||
           ( this.termMap != null && this.termMap.equals( otherQuery.termMap ) ) ) ) {
      return true;
    }
    return false;
  }
  
}
