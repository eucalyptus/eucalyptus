package com.eucalyptus.webui.client;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class AppWidget implements AcceptsOneWidget {
  
  public AppWidget( ) {
  }
  
  @Override
  public void setWidget( IsWidget w ) {
    RootLayoutPanel root = RootLayoutPanel.get( );
    root.clear( );
    if ( w != null ) {
      root.add( w );
    }
  }
  
}
