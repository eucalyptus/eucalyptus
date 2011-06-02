package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class RejectPlace extends SearchPlace {

  public RejectPlace( String search ) {
    super( search );
  }
  
  @Prefix( "reject" )
  public static class Tokenizer implements PlaceTokenizer<RejectPlace> {

    @Override
    public RejectPlace getPlace( String search ) {
      return new RejectPlace( search );
    }

    @Override
    public String getToken( RejectPlace place ) {
      return place.getSearch( );
    }
    
  }
  
}
