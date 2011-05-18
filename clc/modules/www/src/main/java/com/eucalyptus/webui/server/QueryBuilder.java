package com.eucalyptus.webui.server;

import com.google.gwt.http.client.URL;

public class QueryBuilder {

  public static final String BASE = "EucalyptusWebInterface.html#";
  public static final String PLACE_SEPARATOR = ":";
  public static final String EQUAL = "=";
  public static final String OR = "+";
  public static final String NOT = "-";
  
  private StringBuilder sb = new StringBuilder( );
  
  private QueryBuilder( ) { }
  
  public static QueryBuilder get( ) {
    return new QueryBuilder( );
  }
  
  public QueryBuilder start( String type ) {
    sb.append( type ).append( PLACE_SEPARATOR );
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
    return sb.toString( );
  }
  
  public String url( ) {
    return URL.encode( BASE + sb.toString( ) );
  }
  
}
