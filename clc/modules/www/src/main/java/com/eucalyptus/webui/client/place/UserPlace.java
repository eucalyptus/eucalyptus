package com.eucalyptus.webui.client.place;

import com.eucalyptus.webui.client.service.CategoryConstants;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class UserPlace extends SearchPlace {

  public UserPlace( String search ) {
    super( search );
  }

  @Prefix( CategoryConstants.USER )
  public static class Tokenizer implements PlaceTokenizer<UserPlace> {

    @Override
    public UserPlace getPlace( String search ) {
      return new UserPlace( decode( search ) );
    }

    @Override
    public String getToken( UserPlace place ) {
      return encode( place.getSearch( ) );
    }
    
  }
  
}
