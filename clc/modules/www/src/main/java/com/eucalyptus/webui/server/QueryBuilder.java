package com.eucalyptus.webui.server;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
  
  public QueryBuilder and( String field, String pattern ) {
    if ( field != null && !"".equals( field ) ) {
      sb.append( field ).append( EQUAL );
    }
    sb.append( pattern );
    return this;
  }
  
  public QueryBuilder or( String field, String pattern ) {
    sb.append( OR );
    and( field, pattern );
    return this;
  }
  
  public QueryBuilder not( String field, String pattern ) {
    sb.append( NOT );
    and( field, pattern );
    return this;
  }
  
  public String query( ) {
    return this.type + PLACE_SEPARATOR + sb.toString( );
  }
  
  public String subquery( ) {
    return sb.toString( );
  }
  
  public String url( ) {
    try {
      return BASE + this.type + PLACE_SEPARATOR + URLEncoder.encode( sb.toString( ), "UTF-8" );
    } catch ( UnsupportedEncodingException e ) {
      return BASE + this.type + PLACE_SEPARATOR + URLEncoder.encode( sb.toString( ) );
    }
  }
  
}
