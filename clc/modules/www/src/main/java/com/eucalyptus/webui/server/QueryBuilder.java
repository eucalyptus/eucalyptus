package com.eucalyptus.webui.server;

import java.io.UnsupportedEncodingException;
import com.eucalyptus.webui.shared.query.QueryConstants;
import com.eucalyptus.webui.shared.query.QueryType;
import com.eucalyptus.webui.shared.query.SearchQuery;
import com.eucalyptus.webui.shared.query.StringValue;

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

  //public static final String BASE = "EucalyptusWebInterface.html#";
  public static final String SHARP = "#";
  
  private SearchQuery query;
  
  private QueryBuilder( ) { }
  
  public static QueryBuilder get( ) {
    return new QueryBuilder( );
  }
  
  public QueryBuilder start( QueryType type ) {
    this.query = new SearchQuery( type );
    return this;
  }  
  
  public QueryBuilder add( String field, String value ) {
    this.query.getTermMap( ).put( field, new StringValue( value ) );
    return this;
  }
  
  public String query( ) {
    return this.query.toString( );
  }
  
  public String url( ) {
    try {
      StringBuilder sb = new StringBuilder( );
      sb.append( SHARP ).append( UriUtils.encodeFragment( this.query.toString( ), "UTF-8" ) );
      return sb.toString( );
    } catch ( UnsupportedEncodingException e ) {
      throw new RuntimeException( "Failed to use UTF-8 in URL encoding" );
    }
  }
  
  public String url( String base ) {
    if ( base != null ) {
      return base + url( );
    }
    return url( );
  }
  
}
