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
