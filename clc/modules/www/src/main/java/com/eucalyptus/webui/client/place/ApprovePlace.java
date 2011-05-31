package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class ApprovePlace extends SearchPlace {

  public ApprovePlace( String search ) {
    super( search );
  }
  
  @Prefix( "approve" )
  public static class Tokenizer implements PlaceTokenizer<ApprovePlace> {

    @Override
    public ApprovePlace getPlace( String search ) {
      return new ApprovePlace( search );
    }

    @Override
    public String getToken( ApprovePlace place ) {
      return place.getSearch( );
    }
    
  }
  
}
