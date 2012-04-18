package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class KeyPlace extends SearchPlace {

  public KeyPlace( String search ) {
    super( search );
  }

  @Prefix( "key" )
  public static class Tokenizer implements PlaceTokenizer<KeyPlace> {

    @Override
    public KeyPlace getPlace( String search ) {
      return new KeyPlace( search );
    }

    @Override
    public String getToken( KeyPlace place ) {
      return place.getSearch( );
    }
    
  }
  
}
