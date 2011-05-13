package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class ConfigPlace extends Place {

  private String search;
  
  public ConfigPlace( String search ) {
    this.search = search;
  }
  
  public String getSearch( ) {
    return this.search;
  }
  
  @Prefix( "config" )
  public static class Tokenizer implements PlaceTokenizer<ConfigPlace> {

    @Override
    public ConfigPlace getPlace( String token ) {
      return new ConfigPlace( token );
    }

    @Override
    public String getToken( ConfigPlace place ) {
      return place.getSearch( );
    }
    
  }
  
}
