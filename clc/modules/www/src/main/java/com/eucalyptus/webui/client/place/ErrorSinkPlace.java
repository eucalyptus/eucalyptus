package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class ErrorSinkPlace extends Place {

  @Prefix( "error" )
  public static class Tokenizer implements PlaceTokenizer<ErrorSinkPlace> {

    @Override
    public ErrorSinkPlace getPlace( String token ) {
      return new ErrorSinkPlace( );
    }

    @Override
    public String getToken( ErrorSinkPlace place ) {
      return "";
    }
    
  }
  
}
