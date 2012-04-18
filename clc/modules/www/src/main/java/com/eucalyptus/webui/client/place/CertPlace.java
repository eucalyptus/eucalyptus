package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class CertPlace extends SearchPlace {

  public CertPlace( String search ) {
    super( search );
  }

  @Prefix( "cert" )
  public static class Tokenizer implements PlaceTokenizer<CertPlace> {

    @Override
    public CertPlace getPlace( String search ) {
      return new CertPlace( search );
    }

    @Override
    public String getToken( CertPlace place ) {
      return place.getSearch( );
    }
    
  }
  
}
