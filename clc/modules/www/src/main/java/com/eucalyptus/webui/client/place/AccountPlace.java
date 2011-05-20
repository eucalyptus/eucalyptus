package com.eucalyptus.webui.client.place;

import com.eucalyptus.webui.client.service.CategoryConstants;
import com.google.gwt.http.client.URL;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class AccountPlace extends SearchPlace {
  
  public AccountPlace( String search ) {
    super( search );
  }
  
  @Prefix( CategoryConstants.ACCOUNT )
  public static class Tokenizer implements PlaceTokenizer<AccountPlace> {

    @Override
    public AccountPlace getPlace( String token ) {
      return new AccountPlace( decode( token )  );
    }

    @Override
    public String getToken( AccountPlace place ) {
      return encode( place.getSearch( ) );
    }
    
  }
  
}
