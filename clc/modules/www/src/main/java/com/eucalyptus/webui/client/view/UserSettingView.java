package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface UserSettingView extends IsWidget {

  void setUser( String user );
  
  void setPresenter( Presenter listener );
  
  public interface Presenter {
    void logout( );

    void onShowProfile( );

    void onChangePassword( );

    void onDownloadCredential( );

    void onShowKey( );
  }
  
}
