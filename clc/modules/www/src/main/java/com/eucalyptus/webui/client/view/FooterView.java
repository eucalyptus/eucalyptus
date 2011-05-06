package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface FooterView extends IsWidget {

  void setPresenter( Presenter listener );
  
  public interface Presenter {
    void onShowLogConsole( );
    void onHideLogConsole( );
  }
  
}
