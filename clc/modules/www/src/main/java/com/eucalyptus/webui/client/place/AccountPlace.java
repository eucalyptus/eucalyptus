package com.eucalyptus.webui.client.place;

import com.eucalyptus.webui.client.service.Categories;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class AccountPlace extends SearchPlace {
  
  public AccountPlace( String search ) {
    super( search );
  }
  
  @Prefix( Categories.ACCOUNT )
  public static class Tokenizer implements PlaceTokenizer<AccountPlace> {

    @Override
    public AccountPlace getPlace( String token ) {
      return new AccountPlace( token );
    }

    @Override
    public String getToken( AccountPlace place ) {
      return place.getSearch( );
    }
    
  }
  
}
