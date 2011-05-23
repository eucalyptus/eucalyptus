package com.eucalyptus.webui.server;

import java.io.UnsupportedEncodingException;

/**
 * Building query string given query components.
 * 
 * The format of the query url (containing the URL base)
 * 
 * <code> [BASE_URL]#[type]:[subquery] </code>
 * 
 * @author wenye
 *
 */
public class QueryBuilder {

  public static final String BASE = "EucalyptusWebInterface.html#";
  public static final String PLACE_SEPARATOR = ":";
  public static final String EQUAL = "=";
  public static final String OR = "+";
  public static final String NOT = "-";
  
  private StringBuilder sb = new StringBuilder( );
  private String type;
  
  private QueryBuilder( ) { }
  
  public static QueryBuilder get( ) {
    return new QueryBuilder( );
  }
  
  public QueryBuilder start( String type ) {
    this.type = type;
    return this;
  }
  
  private QueryBuilder component( String field, String pattern ) {
    if ( field != null && !"".equals( field ) ) {
      sb.append( field ).append( EQUAL );
    }
    sb.append( pattern );
    return this;    
  }
  
  public QueryBuilder and( String field, String pattern ) {
    if ( sb.length( ) > 0 ) {
      sb.append( " " );
    }
    return component( field, pattern );
  }
  
  public QueryBuilder or( String field, String pattern ) {
    if ( sb.length( ) > 0 ) {
      sb.append( " " );
    }
    sb.append( OR );
    return component( field, pattern );
  }
  
  public QueryBuilder not( String field, String pattern ) {
    if ( sb.length( ) > 0 ) {
      sb.append( " " );
    }
    sb.append( NOT );
    return component( field, pattern );
  }
  
  public String query( ) {
    return this.type + PLACE_SEPARATOR + sb.toString( );
  }
  
  public String subquery( ) {
    return sb.toString( );
  }
  
  public String url( ) {
    try {
      return BASE + this.type + PLACE_SEPARATOR + UriUtils.encodePathSegment( sb.toString( ), "UTF-8" );
    } catch ( UnsupportedEncodingException e ) {
      throw new RuntimeException( "Failed to use UTF-8 in URL encoding" );
    }
  }
  
}
