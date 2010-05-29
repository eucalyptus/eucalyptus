package edu.ucsb.eucalyptus.admin.client.util;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public class Events {
  public static ClickHandler     DO_NOTHING         = new ClickHandler( ) {
    @Override
    public void onClick( ClickEvent arg0 ) {}
  };

}
