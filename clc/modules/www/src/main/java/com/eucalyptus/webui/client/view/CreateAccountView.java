package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface CreateAccountView extends IsWidget {

  void init( );

  void setPresenter( Presenter presenter );
  
  public interface Presenter {

    void doCreateAccount( String value );

    void cancel( );
    
  }
  
}
