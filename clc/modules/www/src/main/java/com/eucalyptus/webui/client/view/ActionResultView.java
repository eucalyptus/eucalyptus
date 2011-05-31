package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface ActionResultView extends IsWidget {

  public enum ResultType {
    ERROR,
    INFO,
    NONE
  }
    
  void display( ResultType type, String message, boolean needsConfirmation );
  
  void loading( );
  
  void setPresenter( Presenter presenter );
  
  public interface Presenter {
    void onConfirmed( );
  }
}
