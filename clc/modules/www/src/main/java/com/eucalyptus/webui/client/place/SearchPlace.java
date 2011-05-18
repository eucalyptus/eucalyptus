package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.Place;

public class SearchPlace extends Place {
  
  protected String search;
  
  public SearchPlace( String search ) {
    this.search = search;
  }
  
  public String getSearch( ) {
    return search;
  }
  
}
