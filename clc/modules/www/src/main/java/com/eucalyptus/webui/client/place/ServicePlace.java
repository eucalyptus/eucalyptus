package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class ServicePlace extends Place {

  private String search;
  
  public ServicePlace( String search ) {
    this.search = search;
  }
  
  public String getSearch( ) {
    return this.search;
  }
  
  @Prefix( "service" )
  public static class Tokenizer implements PlaceTokenizer<ServicePlace> {

    @Override
    public ServicePlace getPlace( String token ) {
      return new ServicePlace( token );
    }

    @Override
    public String getToken( ServicePlace place ) {
      return place.getSearch( );
    }
    
  }
  
}
