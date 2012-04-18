package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class ReportPlace extends SearchPlace {

  public ReportPlace( String search ) {
    super( search );
  }

  @Prefix( "report" )
  public static class Tokenizer implements PlaceTokenizer<ReportPlace> {

    @Override
    public ReportPlace getPlace( String search ) {
      return new ReportPlace( search );
    }

    @Override
    public String getToken( ReportPlace place ) {
      return place.getSearch( );
    }
    
  }
  
}
