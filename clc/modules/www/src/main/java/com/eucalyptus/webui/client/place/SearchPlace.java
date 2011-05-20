package com.eucalyptus.webui.client.place;

import com.google.gwt.http.client.URL;
import com.google.gwt.place.shared.Place;

public class SearchPlace extends Place {
  
  protected String search;
  
  public SearchPlace( String search ) {
    this.search = search;
  }
  
  public String getSearch( ) {
    return search;
  }
  
  /**
   * URI decode the search subquery (part without the "type:") following RFC3986.
   * 
   * @param encoded
   * @return
   */
  public static String decode( String encoded ) {
    if ( encoded != null ) {
      return URL.decodePathSegment( encoded );
    }
    return "";
  }
  
  /**
   * URI encode the search subquery (part without the "type:") following RFC3986
   * @param decoded
   * @return
   */
  public static String encode( String decoded ) {
    if ( decoded != null ) {
      return URL.encodePathSegment( decoded );
    }
    return "";
  }
  
  /**
   * URI encode a complete search string.
   * 
   * @param typedSearch
   * @return
   */
  public static String encodeTyped( String typedSearch ) {
    if ( typedSearch != null ) {
      int colonLoc = typedSearch.indexOf( ':' );
      if ( colonLoc >= 0 ) {
        return typedSearch.substring( 0, colonLoc + 1 ) + encode( typedSearch.substring( colonLoc + 1 ) );
      } else {
        return encode( typedSearch );
      }
    }
    return "";
  }
  
}
