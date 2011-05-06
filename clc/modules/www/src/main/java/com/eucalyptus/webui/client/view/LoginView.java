package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface LoginView extends IsWidget {
  
  void setPrompt( String prompt );
  
  void setPresenter( Presenter listener );
  
  public interface Presenter {
    void login( String username, String password, boolean staySignedIn );
  }
  
}
