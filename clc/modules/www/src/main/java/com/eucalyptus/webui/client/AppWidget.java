package com.eucalyptus.webui.client;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.IsWidget;

public class AppWidget implements AcceptsOneWidget {
  
  private HasWidgets.ForIsWidget container;
  
  public AppWidget( HasWidgets.ForIsWidget container ) {
    this.container = container;
  }
  
  @Override
  public void setWidget( IsWidget w ) {
    container.clear( );
    if ( w != null ) {
      container.add( w );
    }
  }
  
}
