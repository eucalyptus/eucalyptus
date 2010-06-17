package edu.ucsb.eucalyptus.admin.client.util;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import edu.ucsb.eucalyptus.admin.client.EucaButton;

public class Buttons {
  public final static String     STYLE_HIDDEN = "euca-hidden";
  public final static String     STYLE_ERROR  = "euca-error";
  public final static String     STYLE_NOOP  = "euca-noop";
  public final static EucaButton HIDDEN       = new EucaButton( "", "", STYLE_HIDDEN, Events.DO_NOTHING );
  public final static EucaButton FILLER       = new EucaButton( "FILLER", "SOME FILLER TEXT", new ClickHandler( ) {
                                                @Override
                                                public void onClick( ClickEvent arg0 ) {
                                                  FILLER.setText( FILLER.getText( ) + "." );
                                                }
                                              } );
  
  public final static EucaButton errorButton( Throwable t ) {
    return new EucaButton( "ERROR: " + t.getMessage( ).replaceAll( "stack:.*", "" ), t.toString( ), STYLE_ERROR, Events.DO_NOTHING );
  }
}
