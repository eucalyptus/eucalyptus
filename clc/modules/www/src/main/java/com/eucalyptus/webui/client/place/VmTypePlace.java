package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class VmTypePlace extends SearchPlace {
    
  public VmTypePlace( String search ) {
    super( search );
  }
  
  @Prefix( "vmtype" )
  public static class Tokenizer implements PlaceTokenizer<VmTypePlace> {

    @Override
    public VmTypePlace getPlace( String search ) {
      return new VmTypePlace( search );
    }

    @Override
    public String getToken( VmTypePlace place ) {
      return place.getSearch( );
    }
    
  }
  
}
