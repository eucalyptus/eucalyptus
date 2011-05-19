package com.eucalyptus.webui.client.place;

import com.eucalyptus.webui.client.service.CategoryConstants;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class PolicyPlace extends SearchPlace {

  public PolicyPlace( String search ) {
    super( search );
  }

  @Prefix( CategoryConstants.POLICY )
  public static class Tokenizer implements PlaceTokenizer<PolicyPlace> {

    @Override
    public PolicyPlace getPlace( String search ) {
      return new PolicyPlace( search );
    }

    @Override
    public String getToken( PolicyPlace place ) {
      return place.getSearch( );
    }
    
  }
  
}
