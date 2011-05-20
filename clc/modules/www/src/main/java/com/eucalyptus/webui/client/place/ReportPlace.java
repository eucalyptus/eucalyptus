package com.eucalyptus.webui.client.place;

import com.eucalyptus.webui.client.service.CategoryConstants;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class ReportPlace extends SearchPlace {

  public ReportPlace( String search ) {
    super( search );
  }

  @Prefix( CategoryConstants.REPORT )
  public static class Tokenizer implements PlaceTokenizer<ReportPlace> {

    @Override
    public ReportPlace getPlace( String search ) {
      return new ReportPlace( decode( search ) );
    }

    @Override
    public String getToken( ReportPlace place ) {
      return encode( place.getSearch( ) );
    }
    
  }
  
}
