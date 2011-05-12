package com.eucalyptus.webui.client.view;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface GlobalResources extends ClientBundle {
  
  public interface EucaButtonStyle extends CssResource {
    String button( );
    String minus( );
    String plus( );
    String middle( );
    String pill( );
    String primary( );
    String left( );
    String positive( );
    String right( );
    String icon( );
    String active( );
    String negative( );
    String big( );
  }
  
  @Source( "EucaButton.css" )
  EucaButtonStyle buttonCss( );
  
  @Source( "image/plus_12x12_gray.png" )
  ImageResource plusGray( );
  @Source( "image/plus_12x12_white.png" )
  ImageResource plusWhite( );
  
  @Source( "image/minus_12x3_gray.png" )
  ImageResource minusGray( );
  @Source( "image/minus_12x3_white.png" )
  ImageResource minusWhite( );
  
}
