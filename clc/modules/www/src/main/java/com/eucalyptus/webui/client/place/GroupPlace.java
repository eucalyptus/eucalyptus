package com.eucalyptus.webui.client.place;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class GroupPlace extends SearchPlace {

  public GroupPlace( String search ) {
    super( search );
  }

  @Prefix( "group" )
  public static class Tokenizer implements PlaceTokenizer<GroupPlace> {

    @Override
    public GroupPlace getPlace( String search ) {
      return new GroupPlace( search );
    }

    @Override
    public String getToken( GroupPlace place ) {
      return place.getSearch( );
    }
    
  }
  
}
