package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class AccountPlace extends Place {

  @Prefix( "account" )
  public static class Tokenizer implements PlaceTokenizer<AccountPlace> {

    @Override
    public AccountPlace getPlace( String token ) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String getToken( AccountPlace place ) {
      // TODO Auto-generated method stub
      return null;
    }
    
  }
  
}
