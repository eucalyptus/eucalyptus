package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface DetailView extends IsWidget {
  
  void setTitle( String title );
  
  void setPresenter( Presenter presenter );
  
  public interface Presenter {
    void hideDetail( );
  }
  
}
