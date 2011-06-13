package com.eucalyptus.webui.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface RightScaleView extends IsWidget {

  void display( String cloudUrl, String cloudId );
  
  void setPresenter( Presenter presenter );
  
  public interface Presenter {

    void register( );
    
  }
}
