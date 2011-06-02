package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface FooterView extends IsWidget {

  public static final int DEFAULT_STATUS_CLEAR_DELAY = 60000;//1 min
  
  public enum StatusType {
    ERROR,
    LOADING,
    NONE
  }
  
  void setVersion( String version );
  
  void showStatus( StatusType type, String status, int clearDelay );
  
  void setPresenter( Presenter listener );
  
  public interface Presenter {
    void onShowLogConsole( );
    void onHideLogConsole( );
  }
  
}
