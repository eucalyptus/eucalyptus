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
  
  public static String decode( String encoded ) {
    if ( encoded != null ) {
      return URL.decodePathSegment( encoded );
    }
    return null;
  }
  
  public static String encode( String decoded ) {
    if ( decoded != null ) {
      return URL.encodePathSegment( decoded );
    }
    return null;
  }
  
}
