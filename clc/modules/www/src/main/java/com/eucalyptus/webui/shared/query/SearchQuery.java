package com.eucalyptus.webui.shared.query;

import java.util.Set;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;

/**
 * Internal representation of a search query
 * 
 * @author Ye Wen (wenye@eucalyptus.com)
 *
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
