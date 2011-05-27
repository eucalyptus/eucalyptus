package com.eucalyptus.webui.shared.query;

import java.io.Serializable;

public class QueryParsingException extends Exception implements Serializable {

  private static final long serialVersionUID = 1L;
  
  public QueryParsingException( ) {
    super( );
  }
  
  public QueryParsingException( String message ) {
    super( message );
  }
  
  public QueryParsingException( Throwable cause ) {
    super( cause );
  }
  
  public QueryParsingException( String message, Throwable cause ) {
    super( message, cause );
  }
  
}
