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

import com.google.common.base.Strings;

/**
 * Parsing a search query.
 * 
 * Syntax:
 *   type: field1=value,value... field2=value,value,... ...
 *   
 *   type -- see {@link QueryType}
 */
public class QueryParser {

  private QueryParser( ) { }
  
  public static QueryParser get( ) {
    return new QueryParser( );
  }
  
  public SearchQuery parse( String type, String query ) throws QueryParsingException {
    if ( type == null ) {
      throw new QueryParsingException( "Empty type" );
    }
    try {
      SearchQuery parsed = new SearchQuery( QueryType.valueOf( type ) );
      if ( !Strings.isNullOrEmpty( query ) ) {
        for ( String condition : query.split( "\\s+" ) ) {
          parseComponents( parsed, condition );          
        }
      }
      return parsed;
    } catch ( IllegalArgumentException e ) {
      throw new QueryParsingException( "Can not recognize query type: " + type );
    }
  }
  
  private void parseComponents( SearchQuery parsed, String condition ) throws QueryParsingException {
    if ( !Strings.isNullOrEmpty( condition ) ) {
      int equalLoc = condition.indexOf( QueryConstants.EQUAL );
      if ( equalLoc < 1 || equalLoc > condition.length( ) - 2 ) {
        throw new QueryParsingException( "Invalid condition: " + condition );
      }
      final String field = condition.substring( 0, equalLoc );
      for ( String value : condition.substring( equalLoc + 1 ).split( QueryConstants.COMMA ) ) {
        if ( !Strings.isNullOrEmpty( value ) ) {
          parsed.getTermMap( ).put( field, new StringValue( value ) );
        }
      }
    }
  }

  public SearchQuery parse( String query ) throws QueryParsingException {
    if ( query == null ) {
      throw new QueryParsingException( "Empty query" );
    }
    int colonLoc = query.indexOf( QueryConstants.TYPE_SEPARATOR );
    if ( colonLoc < 1 ) {
      throw new QueryParsingException( "Invalid full query: no type: " + query );
    }
    return parse( query.substring( 0, colonLoc ), query.substring( colonLoc + 1 ) );
  }
  
}
