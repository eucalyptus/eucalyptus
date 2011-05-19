package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class StartPlace extends Place {
  
  public StartPlace( ) {
  }
  
  @Prefix( "start" )
  public static class Tokenizer implements PlaceTokenizer<StartPlace> {

    @Override
    public StartPlace getPlace( String token ) {
      return new StartPlace( );
    }

    @Override
    public String getToken( StartPlace place ) {
      return "";
    }
    
  }
  
}
