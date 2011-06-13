package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface LoginView extends IsWidget {
  
  void setFocus( );
  
  void setPrompt( String prompt );
  
  void clearPassword( );
  
  void setPresenter( Presenter listener );
  
  public interface Presenter {
    
    void login( String accountName, String userName, String password, boolean staySignedIn );

    void onAccountSignup( );

    void onUserSignup( );

    void onRecoverPassword( );
    
  }
  
}
