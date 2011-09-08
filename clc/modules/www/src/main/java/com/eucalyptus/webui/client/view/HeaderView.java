package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface HeaderView extends IsWidget {
    
  void setUser( String user );
  
  UserSettingView getUserSetting( );
  
  void setPresenter( Presenter presenter );
  
  public interface Presenter {
    void runManualSearch( String search );
  }
}
