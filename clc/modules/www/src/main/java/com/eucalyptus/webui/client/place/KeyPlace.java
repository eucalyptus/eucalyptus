package com.eucalyptus.webui.client.place;

import com.eucalyptus.webui.client.service.CategoryConstants;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class KeyPlace extends SearchPlace {

  public KeyPlace( String search ) {
    super( search );
  }

  @Prefix( CategoryConstants.KEY )
  public static class Tokenizer implements PlaceTokenizer<KeyPlace> {

    @Override
    public KeyPlace getPlace( String search ) {
      return new KeyPlace( decode( search ) );
    }

    @Override
    public String getToken( KeyPlace place ) {
      return encode( place.getSearch( ) );
    }
    
  }
  
}
