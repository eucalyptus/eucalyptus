package com.eucalyptus.webui.client.place;

import com.eucalyptus.webui.client.service.CategoryConstants;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class VmTypePlace extends SearchPlace {
    
  public VmTypePlace( String search ) {
    super( search );
  }
  
  @Prefix( CategoryConstants.VMTYPE )
  public static class Tokenizer implements PlaceTokenizer<VmTypePlace> {

    @Override
    public VmTypePlace getPlace( String search ) {
      return new VmTypePlace( decode( search ) );
    }

    @Override
    public String getToken( VmTypePlace place ) {
      return encode( place.getSearch( ) );
    }
    
  }
  
}
