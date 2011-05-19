package com.eucalyptus.webui.client.place;

import com.eucalyptus.webui.client.service.Categories;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class UserPlace extends SearchPlace {

  public UserPlace( String search ) {
    super( search );
  }

  @Prefix( Categories.USER )
  public static class Tokenizer implements PlaceTokenizer<UserPlace> {

    @Override
    public UserPlace getPlace( String search ) {
      return new UserPlace( search );
    }

    @Override
    public String getToken( UserPlace place ) {
      return place.getSearch( );
    }
    
  }
  
}
