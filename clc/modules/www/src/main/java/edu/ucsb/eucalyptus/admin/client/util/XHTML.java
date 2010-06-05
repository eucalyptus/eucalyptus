package edu.ucsb.eucalyptus.admin.client.util;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;

public class XHTML {
  public static String headerWithImage( final String text, final ImageResource image, final String style ) {
    return new HorizontalPanel( ) {
      {
        setSpacing( 0 );
        setVerticalAlignment( HasVerticalAlignment.ALIGN_MIDDLE );
        add( new Image( image ) );
        HTML headerText = new HTML( text ) {
          {
            setStyleName( style );
          }
        };
        add( headerText );
      }
    }.getElement( ).getString( );
  }
}
