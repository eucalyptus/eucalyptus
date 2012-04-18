package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class AccountPlace extends SearchPlace {
  
  public AccountPlace( String search ) {
    super( search );
  }
  
  @Prefix( "account" )
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
