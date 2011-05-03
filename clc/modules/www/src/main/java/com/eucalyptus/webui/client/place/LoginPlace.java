package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class LoginPlace extends Place {
  
  public LoginPlace( ) {
  }
  
  public static class Tokenizer implements PlaceTokenizer<LoginPlace> {

    @Override
    public LoginPlace getPlace( String token ) {
      return new LoginPlace( );
    }

    @Override
    public String getToken( LoginPlace place ) {
      return null;
    }
    
  }
  
}
