package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class StartPlace extends Place {

  public StartPlace( ) {
    
  }
  
  public static class Tokenizer implements PlaceTokenizer<StartPlace> {

    @Override
    public StartPlace getPlace( String token ) {
      return new StartPlace( );
    }

    @Override
    public String getToken( StartPlace place ) {
      return "default";
    }
    
  }
  
}
