package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.IsWidget;

public interface ContentView extends IsWidget {

  HasWidgets.ForIsWidget getContentContainer( );
  
  void setContentTitle( String title );
  
}
