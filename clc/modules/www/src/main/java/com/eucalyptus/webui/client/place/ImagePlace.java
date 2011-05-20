package com.eucalyptus.webui.client.place;

import com.eucalyptus.webui.client.service.CategoryConstants;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class ImagePlace extends SearchPlace {

  public ImagePlace( String search ) {
    super( search );
  }

  @Prefix( CategoryConstants.IMAGE )
  public static class Tokenizer implements PlaceTokenizer<ImagePlace> {

    @Override
    public ImagePlace getPlace( String search ) {
      return new ImagePlace( decode( search ) );
    }

    @Override
    public String getToken( ImagePlace place ) {
      return encode( place.getSearch( ) );
    }
    
  }
  
}
